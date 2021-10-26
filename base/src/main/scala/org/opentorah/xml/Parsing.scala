package org.opentorah.xml

import org.opentorah.util.Effects
import java.net.URL
import zio.{Has, ZIO}

final class Parsing private:

  // Note: using case classes Empty, Characters, Elements and Mixed (in the spirit of "parse, do not validate")
  // leads to much verbosity, since copy method is only available on case classes, which shouldn't be inherited from...
  private final class Current(val xml: Xml)(
    val from: Option[From],
    val name: String,
    val attributes: Attribute.StringValues,
    val contentType: Element.ContentType,
    val nodes: xml.Nodes,
    val characters: Option[String],
    val nextElementNumber: Int
  ):
    def copy(
      attributes: Attribute.StringValues = attributes,
      nodes: xml.Nodes = nodes,
      characters: Option[String] = characters,
      nextElementNumber: Int = nextElementNumber
    ): Current = Current(xml)(
      from,
      name,
      attributes,
      contentType,
      nodes,
      characters,
      nextElementNumber
    )

  private var stack: List[Current] = List.empty

  // TODO report error better: effect.tapCause(cause => console.putStrLn(cause.prettyPrint))?
  private def addErrorTrace[A](error: Effects.Error): Effects.Error =
    new Effects.Error(
      message = error.getMessage + "\n" + stack.flatMap(_.from).map(_.url),
      cause = error.getCause
    )

  private def modifyCurrent[A](newCurrent: Current): Unit =
    stack = newCurrent :: stack.tail

  private def checkEmpty: Unit =
    if stack.nonEmpty then throw IllegalStateException(s"Non-empty $this!")

  private def checkNoLeftovers: Effects.IO[Unit] =
    stack.headOption.fold(Effects.ok)((current: Current) =>
      Effects.check(current.attributes.isEmpty, s"Unparsed attributes: ${current.attributes}") *>
      Effects.check(current.characters.isEmpty, s"Unparsed characters: ${current.characters.get}") *>
      Effects.check(current.nodes.forall(current.xml.isWhitespace), s"Unparsed nodes: ${current.nodes}")
    )

  private def currentFromUrl: Element.FromUrl = Element.FromUrl(
    url = currentBaseUrl.get,
    inline = stack.head.from.isEmpty
  )

  private def currentBaseUrl: Option[URL] =
    stack.flatMap(_.from).head.url
    
  private def currentXml: Xml =
    stack.head.xml

  private def push(
    from: Option[From],
    nextElement: Parsing.NextElement[?]
  ): Effects.IO[Unit] =
    val contentType: Element.ContentType = nextElement.elementAndParser.element.contentType
    val xmlElement: nextElement.xml.Element = nextElement.xmlElement
    val nodes: nextElement.xml.Nodes = nextElement.xml.getChildren(xmlElement)
    val (elements: Seq[nextElement.xml.Element], characters: Option[String]) = partition(nextElement.xml)(nodes)

    Effects.check(contentType.elementsAllowed  || elements.isEmpty  , s"Spurious elements: $elements") *>
    Effects.check(contentType.charactersAlowed || characters.isEmpty, s"Spurious characters: '${characters.get}'") *>
    ZIO.succeed {
      val newCurrent: Current = Current(nextElement.xml)(
        from = from,
        name = nextElement.xml.getName(xmlElement),
        attributes = nextElement.xml.getAttributes(xmlElement),
        contentType = contentType,
        nodes = if contentType.elementsAllowed then nodes else Seq.empty,
        characters = if contentType == Element.ContentType.Characters then characters else None,
        nextElementNumber = 0
      )
      stack = newCurrent :: stack
    }

  private def pop(): Unit =
    stack = stack.tail

  // TODO maybe take namespace into account?
  // TODO handle repeated attributes?
  private def takeAttribute(attribute: Attribute[?]): Option[String] =
    // TODO Attribute is (and must be!) invariant in T, but here we need to compare
    given CanEqual[Attribute[?], Attribute[String]] = CanEqual.derived

    val current: Current = stack.head
    val (take: Attribute.StringValues, leave: Attribute.StringValues) =
      current.attributes.partition((candidate: Attribute.Value[String]) => attribute == candidate.attribute)
    modifyCurrent(current.copy(attributes = leave))
    take.headOption.flatMap(_.value)

  private def allAttributes: Attribute.StringValues =
    val current: Current = stack.head
    modifyCurrent(current.copy(attributes = Seq.empty))
    current.attributes

  private def nextElement[A](elements: Elements[A]): Parser[Option[Parsing.NextElement[A]]] =
    val current: Current = stack.head
    Effects.check(current.contentType.elementsAllowed, s"No element in $current") *>
    ZIO.succeed {
      val noLeadingWhitespace: Seq[current.xml.Node] = current.nodes.dropWhile(current.xml.isWhitespace)
      val headElement: Option[current.xml.Element] =
        noLeadingWhitespace.headOption.filter(current.xml.isElement).map(current.xml.asElement)
      headElement.map(current.xml.getName).flatMap(elements.elementAndParser).map { (result: Element.AndParser[A]) =>
        modifyCurrent(current.copy(nodes = noLeadingWhitespace.tail, nextElementNumber = current.nextElementNumber + 1))
        Parsing.NextElement(current.xml)(headElement.get, result)
      }
    }

  private def takeCharacters: Parser[Option[String]] =
    val current: Current = stack.head

    current.contentType match
      case Element.ContentType.Empty | Element.ContentType.Elements =>
        Effects.fail(s"No characters in $current")

      case Element.ContentType.Characters =>
        ZIO.succeed {
          modifyCurrent(current.copy(characters = None))
          current.characters
        }

      case Element.ContentType.Mixed =>
        val (elements: Seq[current.xml.Element], characters: Option[String]) = partition(current.xml)(current.nodes)
        Effects.check(elements.isEmpty, s"Elements in $current") *>
        ZIO.succeed {
          modifyCurrent(current.copy(nodes = Seq.empty))
          characters
        }

  private def allNodes: Parser[Element.Nodes] =
    val current: Current = stack.head
    Effects.check(current.contentType.elementsAllowed, s"No nodes in $current") *>
    ZIO.succeed {
      modifyCurrent(current.copy(nodes = Seq.empty))
      Element.Nodes(current.xml)(current.nodes)
    }

  private def partition(xml: Xml)(nodes: xml.Nodes): (Seq[xml.Element], Option[String]) =
    val (elems, nonElems) = nodes.partition(xml.isElement)
    val characters: String = nonElems.map(xml.toString).mkString.trim
    (elems.map(xml.asElement), if characters.isEmpty then None else Some(characters))


private[xml] object Parsing:

  private final class NextElement[A](val xml: Xml)(val xmlElement: xml.Element, val elementAndParser: Element.AndParser[A])

  def empty: Parsing = new Parsing

  // TODO generalize access() and use it here...
  def addErrorTrace(error: Effects.Error): zio.URIO[Has[Parsing], Effects.Error] =
    ZIO.access[Has[Parsing]](_.get.addErrorTrace(error))

  def optional[A](elements: Elements[A]): Parser[Option[A]] = for
    // TODO take namespace into account!
    nextElementOpt: Option[NextElement[A]] <- accessM(_.nextElement(elements))
    result: Option[A] <- nextElementOpt.fold(ZIO.none)((nextElement: NextElement[A]) =>
      nested(
        from = None,
        nextElement = nextElement
      ).map(Some(_))
    )
  yield result

  final def required[A](elements: Elements[A], from: From): Parser[A] = for
    xmlElement: from.xml.Element <- from.load
    xmlElementName: String = from.xml.getName(xmlElement)
    result: A <- elements.elementAndParser(xmlElementName)
      .fold(Effects.fail(s"$elements required, but '$xmlElementName' found"))(elementAndParser =>
        accessM(_.checkNoLeftovers) *>
        nested(
          from = Some(from),
          nextElement = NextElement(from.xml)(xmlElement, elementAndParser)
        )
      )
  yield result

  private def nested[A](
    from: Option[From],
    nextElement: NextElement[A]
  ): Parser[A] = for
    _ <- accessM(_.push(from, nextElement))
    result <- nextElement.elementAndParser.parser
    _ <- accessM(_.checkNoLeftovers)
    _ <- access(_.pop())
  yield result

  def fromUrl: Parser[Element.FromUrl] = access(_.currentFromUrl)

  def currentBaseUrl: Parser[Option[URL]] = access(_.currentBaseUrl)

  def currentXml: Parser[Xml] = access(_.currentXml)

  def allAttributes: Parser[Attribute.StringValues] = access(_.allAttributes)

  def takeAttribute(attribute: Attribute[?]): Parser[Option[String]] = access(_.takeAttribute(attribute))

  def allNodes: Parser[Element.Nodes] = accessM(_.allNodes)

  def takeCharacters: Parser[Option[String]] = accessM(_.takeCharacters)

  def checkEmpty: Parser[Unit] = access(_.checkEmpty)

  private def access[T](f: Parsing => T): Parser[T] =
    ZIO.access[Has[Parsing]]((parsingHas: Has[Parsing]) => f(parsingHas.get))

  private def accessM[T](f: Parsing => Parser[T]): Parser[T] =
    ZIO.accessM[Has[Parsing]]((parsingHas: Has[Parsing]) => f(parsingHas.get))

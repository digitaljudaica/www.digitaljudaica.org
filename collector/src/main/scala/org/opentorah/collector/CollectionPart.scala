package org.opentorah.collector

import org.opentorah.metadata.Names
import org.opentorah.tei.Title
import org.opentorah.xml.{Attribute, Element, Parsable, Parser, Unparser}

final class CollectionPart(
  val names: Names,
  val from: String,
  val title: Title.Value
):
  def take(documents: Seq[Document]): CollectionPart.Part =
    if documents.isEmpty then throw IllegalArgumentException("No documents for Part!")
    if documents.head.baseName != from then throw IllegalArgumentException("Incorrect 'from' document")
    CollectionPart.Part(Some(title), documents)

object CollectionPart extends Element[CollectionPart]("part"):

  final class Part(
    val title: Option[Title.Value],
    val documents: Seq[Document]
  )

  def getParts(
    parts: Seq[CollectionPart],
    documents: Seq[Document]
  ): Seq[Part] =
    if parts.isEmpty then Seq(Part(None, documents))
    else splitParts(Seq.empty, parts, documents)

  @scala.annotation.tailrec
  private def splitParts(
    result: Seq[Part],
    parts: Seq[CollectionPart],
    documents: Seq[Document]
  ): Seq[Part] = parts match
    case p1 :: p2 :: ds =>
      val (partDocuments: Seq[Document], tail: Seq[Document]) = documents.span(_.baseName != p2.from)
      splitParts(result :+ p1.take(partDocuments), p2 :: ds, tail)

    case p1 :: Nil =>
      result :+ p1.take(documents)

    case Nil =>
      if documents.nonEmpty then
        throw IllegalArgumentException("Documents left over: " + documents.mkString(", ") + ".")
      result

  private val fromAttribute: Attribute.Required[String] = Attribute("from").required

  override def contentParsable: Parsable[CollectionPart] = new Parsable[CollectionPart]:
    override def parser: Parser[CollectionPart] = for
      names: Names <- Names.withDefaultNameParsable()
      from: String <- fromAttribute()
      title: Title.Value <- Title.element.required()
    yield CollectionPart(
      names,
      from,
      title
    )

    override def unparser: Unparser[CollectionPart] = Unparser.concat(
      Names.withDefaultNameParsable(_.names),
      fromAttribute(_.from),
      Title.element.required(_.title)
    )

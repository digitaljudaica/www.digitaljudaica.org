package org.opentorah.texts.tanach

import Tanach.TanachBook
import org.opentorah.metadata.{Metadata, Names}
import org.opentorah.util.Collections
import org.opentorah.xml.{Element, From, Parsable, Parser}
import zio.IO

object TanachMetadata {

  sealed trait State
  final case object Empty extends State
  final case class Parsed(
    names: Map[TanachBook, Names],
    chapters: Map[TanachBook, Chapters],
    metadata: Map[TanachBook, TanachBookMetadata.Parsed]
  ) extends State
  final case class Resolved(
    names: Map[TanachBook, Names],
    chapters: Map[TanachBook, Chapters],
    metadata: Map[TanachBook, TanachBookMetadata]
  ) extends State

  private var state: State = Empty

  def getBook2names: Map[TanachBook, Names] = {
    ensureParsed()
    state match {
      case Parsed(names, _, _) => names
      case Resolved(names, _, _) => names
      case Empty => throw new IllegalStateException()
    }
  }

  def getChapters(book: TanachBook): Chapters = {
    ensureParsed()
    state match {
      case Parsed(_, chapters, _) => chapters(book)
      case Resolved(_, chapters, _) => chapters(book)
      case Empty => throw new IllegalStateException()
    }
  }

  private def ensureParsed(): Unit = {
    checkNotProcessing()
    state match {
      case Parsed(_, _, _) =>
      case Resolved(_, _, _) =>
      case Empty => process(Parser.parseDo(parse))
    }
  }

  private def parse: Parser[Parsed] = for {
    metadatas <- Metadata.load(
      from = From.resource(Tanach),
      elementParsable = Parsable.withInclude(new Element[TanachBookMetadata.Parsed]("book") {
        override protected def parser: Parser[TanachBookMetadata.Parsed] = bookParser
      })
    )

    metadata <- Metadata.bind(
      keys = Tanach.values,
      metadatas,
      getKey = (metadata: TanachBookMetadata.Parsed) => metadata.book
    )
  } yield Parsed(
    Collections.mapValues(metadata)(_.names),
    Collections.mapValues(metadata)(_.chapters),
    metadata
  )

  private def bookParser: Parser[TanachBookMetadata.Parsed] = for {
    names <- Names.withDefaultNameParser
    chapters <- Chapters.parser
    book <- Metadata.find[TanachBook](Tanach.values, names)
    result <- book match {
      case book: Tanach.ChumashBook => ChumashBookMetadata.parser(book, names, chapters)
      case book: Tanach.Psalms.type => PsalmsMetadata.parser(book, names, chapters)
      case book: Tanach.NachBook => IO.succeed(new NachBookMetadata.Parsed(book, names, chapters))
    }
  } yield result

  def forChumash(book: Tanach.ChumashBook): ChumashBookMetadata = forBook(book).asInstanceOf[ChumashBookMetadata]

  def forPsalms: PsalmsMetadata = forBook(Tanach.Psalms).asInstanceOf[PsalmsMetadata]

  def forBook(book: TanachBook): TanachBookMetadata = {
    ensureResolved()
    state match {
      case Resolved(_, _, metadata) => metadata(book)
      case Empty => throw new IllegalStateException()
      case Parsed(_, _, _) => throw new IllegalStateException()
    }
  }

  private def ensureResolved(): Unit = {
    checkNotProcessing()
    ensureParsed()
    state match {
      case parsed@Parsed(_, _, _) => process(resolve(parsed))
      case Resolved(_, _, _) =>
      case Empty => throw new IllegalStateException()
    }
  }

  private def resolve(parsed: Parsed): Resolved = Resolved(
    names = parsed.names,
    chapters = parsed.chapters,
    metadata = Collections.mapValues(parsed.metadata)(metadata => Parser.parseDo(metadata.resolve))
  )

  private var processing: Boolean = false

  private def checkNotProcessing(): Unit = this.synchronized {
    if (processing) throw new IllegalStateException()
  }

  private def process(f: => State): Unit = this.synchronized {
    processing = true
    state = f
    processing = false
  }
}

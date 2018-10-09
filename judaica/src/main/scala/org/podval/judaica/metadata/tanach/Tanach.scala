package org.podval.judaica.metadata.tanach

import org.podval.judaica.metadata.{Holder, Metadata, Named, NamedCompanion, Names, Util, XML}

import scala.xml.Elem

object Tanach extends NamedCompanion {

  override type Key = TanachBook

  sealed trait TanachBook extends Named {
    final def chapters: Chapters = toChapters(this)
  }

  override lazy val toNames: Map[TanachBook, Names] = metadatas.names

  private lazy val toChapters: Map[TanachBook, Chapters] = metadatas.chapters

  sealed abstract class ChumashBook(val parshiot: Seq[Parsha]) extends TanachBook with NamedCompanion {
    final override type Key = Parsha

    final override def values: Seq[Parsha] = parshiot

    private val metadatas = new ChumashBookMetadataHolder(this)

    final override def names: Names = toNames(parshiot.head)

    final override lazy val toNames: Map[Parsha, Names] = metadatas.names

    lazy val span: Map[Parsha, Span] = metadatas.span

    lazy val days: Map[Parsha, Custom.Of[Seq[Span]]] = metadatas.days

    lazy val daysCombined: Map[Parsha, Custom.Of[Seq[Span]]] = metadatas.daysCombined

    lazy val aliyot: Map[Parsha, Seq[Span]] = metadatas.aliyot

    lazy val maftir: Map[Parsha, Span] = metadatas.maftir
  }

  case object Genesis extends ChumashBook(Parsha.genesis)
  case object Exodus extends ChumashBook(Parsha.exodus)
  case object Leviticus extends ChumashBook(Parsha.leviticus)
  case object Numbers extends ChumashBook(Parsha.numbers)
  case object Deuteronomy extends ChumashBook(Parsha.deuteronomy)

  val chumash: Seq[ChumashBook] = Seq(Genesis, Exodus, Leviticus, Numbers, Deuteronomy)

  def getChumashForName(name: String): ChumashBook = getForName(name).asInstanceOf[ChumashBook]

  sealed trait NachBook extends TanachBook {
    final override def names: Names = toNames(this)
  }

  sealed trait ProphetsBook extends NachBook

  sealed trait EarlyProphetsBook extends ProphetsBook

  case object Joshua extends EarlyProphetsBook
  case object Judges extends EarlyProphetsBook
  case object SamuelI extends EarlyProphetsBook { override def name: String = "I Samuel" }
  case object SamuelII extends EarlyProphetsBook { override def name: String = "II Samuel" }
  case object KingsI extends EarlyProphetsBook { override def name: String = "I Kings" }
  case object KingsII extends EarlyProphetsBook { override def name: String = "II Kings" }

  val earlyProphets: Seq[ProphetsBook] = Seq(Joshua, Judges, SamuelI, SamuelII, KingsI, KingsII)

  sealed trait LateProphetsBook extends ProphetsBook

  case object Isaiah extends LateProphetsBook
  case object Jeremiah extends LateProphetsBook
  case object Ezekiel extends LateProphetsBook

  <!-- תרי עשר -->
  sealed trait TreiAsarBook extends LateProphetsBook

  case object Hosea extends TreiAsarBook
  case object Joel extends TreiAsarBook
  case object Amos extends TreiAsarBook
  case object Obadiah extends TreiAsarBook
  case object Jonah extends TreiAsarBook
  case object Micah extends TreiAsarBook
  case object Nahum extends TreiAsarBook
  case object Habakkuk extends TreiAsarBook
  case object Zephaniah extends TreiAsarBook
  case object Haggai extends TreiAsarBook
  case object Zechariah extends TreiAsarBook
  case object Malachi extends TreiAsarBook

  val treiAsar: Seq[TreiAsarBook] = Seq(Hosea, Joel, Amos, Obadiah, Jonah, Micah,
    Nahum, Habakkuk, Zephaniah, Haggai, Zechariah, Malachi)

  val lateProphets: Seq[ProphetsBook] = Seq(Isaiah, Jeremiah, Ezekiel) ++ treiAsar

  val prophets: Seq[ProphetsBook] = earlyProphets ++ lateProphets

  def getProhetForName(name: String): ProphetsBook = getForName(name).asInstanceOf[ProphetsBook]

  sealed trait WritingsBook extends NachBook

  case object Psalms extends WritingsBook
  case object Proverbs extends WritingsBook
  case object Job extends WritingsBook
  case object SongOfSongs extends WritingsBook { override def name: String = "Song of Songs" }
  case object Ruth extends WritingsBook
  case object Lamentations extends WritingsBook
  case object Ecclesiastes extends WritingsBook
  case object Esther extends WritingsBook
  case object Daniel extends WritingsBook
  case object Ezra extends WritingsBook
  case object Nehemiah extends WritingsBook
  case object ChroniclesI extends WritingsBook { override def name: String = "I Chronicles" }
  case object ChroniclesII extends WritingsBook { override def name: String = "II Chronicles" }

  val writings: Seq[WritingsBook] = Seq(Psalms, Proverbs, Job, SongOfSongs, Ruth, Lamentations, Ecclesiastes,
    Esther, Daniel, Ezra, Nehemiah, ChroniclesI, ChroniclesII)

  val nach: Seq[TanachBook] = prophets ++ writings

  override val values: Seq[TanachBook] = chumash ++ nach

  private final case class TanachMetadata(names: Names, chapters: Chapters, weekElements: Seq[Elem])

  private object metadatas extends Holder[TanachBook, TanachMetadata] {
    protected override def load: Map[TanachBook, TanachMetadata] = Metadata.loadMetadata(
      keys = values,
      obj = Tanach.this,
      elementName = "book"
    ).map { case (book, metadata) =>
      metadata.attributes.close()
      val (chapterElements: Seq[Elem], weekElements: Seq[Elem]) =
        XML.span(metadata.elements, "chapter", "week")
      if (!book.isInstanceOf[ChumashBook]) XML.checkNoMoreElements(weekElements)
      book -> TanachMetadata(metadata.names, Chapters(chapterElements), weekElements)
    }

    override def names: Map[TanachBook, Names] = get.mapValues(_.names)

    def chapters: Map[TanachBook, Chapters] = get.mapValues(_.chapters)
  }

  private final case class ParshaMetadata(
    names: Names,
    span: Span.Parsed,
    days: Custom.Sets[Seq[Span.Numbered]],
    daysCombined: Custom.Sets[Seq[Span.Numbered]],
    aliyot: Seq[Span.Numbered],
    maftir: Span.Parsed
  )

  private final class ChumashBookMetadataHolder(book: ChumashBook) extends Holder[Parsha, ParshaMetadata] {
    protected override def load: Map[Parsha, ParshaMetadata] =
      Metadata.bind(
        keys = book.parshiot,
        elements = Tanach.metadatas.get(book).weekElements,
        obj = this
      ).mapValues { metadata =>

        def byCustom(days: Seq[Tanach.DayParsed]): Custom.Sets[Seq[Span.Numbered]] =
          days.groupBy(_.custom).mapValues(days => days.map(_.span))

        val span = Span.parse(metadata.attributes)
        metadata.attributes.close()

        val (aliyahElements, dayElements, maftirElements) = XML.span(metadata.elements,
          "aliyah", "day", "maftir")
        require(maftirElements.length == 1)

        val (days: Seq[DayParsed], daysCombined: Seq[DayParsed]) = dayElements.map(parseDay).partition(!_.isCombined)

        ParshaMetadata(
          names = metadata.names,
          span = span,
          days = byCustom(days),
          daysCombined = byCustom(daysCombined),
          aliyot = aliyahElements.map(element => XML.parseEmpty(element, "aliyah", Span.parseNumbered)),
          maftir = XML.parseEmpty(maftirElements.head, "maftir", Span.parse)
        )
      }

    override def names: Map[Parsha, Names] = get.mapValues(_.names)

    def span: Map[Parsha, Span] = Util.inSequence(
      keys = book.parshiot,
      map = get.mapValues(_.span),
      f = (pairs: Seq[(Parsha, Span.Parsed)]) => Span.setImpliedTo(pairs.map(_._2), book.chapters.full, book.chapters)
    )

    def days: Map[Parsha, Custom.Of[Seq[Span]]] = get.map { case (parsha, metadata) =>
      parsha -> Custom.denormalize(processDays(metadata.days, parsha.span, book.chapters))
    }

    def daysCombined: Map[Parsha, Custom.Of[Seq[Span]]] = Util.inSequence(
      keys = book.parshiot,
      map = get.map { case (parsha: Parsha, metadata: ParshaMetadata) => parsha -> metadata.daysCombined },
      f = combineDays
    )

    def aliyot: Map[Parsha, Seq[Span]] = get.map { case (parsha, metadata) =>
      // TODO QUESTION if Cohen ends in a custom place, does it affect the end of the 3 aliyah on Mon/Thu?
      // TODO QUESTION if the parshiot combine, does it affect those small aliyot?
      val aliyotSpan: Span = Span(parsha.span.from, metadata.aliyot.last.span.to.getOrElse(parsha.days(Custom.Common).head.to))
      val aliyotWithImplied1: Seq[Span.Numbered] = Span.addImplied1(metadata.aliyot, aliyotSpan, book.chapters)
      val result: Seq[Span] = Span.setImpliedToCheckAndDropNumbers(aliyotWithImplied1, 3, aliyotSpan, book.chapters)
      parsha -> result
    }

    def maftir: Map[Parsha, Span] = get.map { case (parsha, metadata) =>
      val maftir = metadata.maftir
      val result: Span = Span.setImpliedTo(
        Seq(maftir),
        Span(maftir.from, maftir.to.getOrElse(parsha.span.to)),
        book.chapters
      ).head
      parsha -> result
    }
  }

  private final case class DayParsed(
    span: Span.Numbered,
    custom: Set[Custom],
    isCombined: Boolean
  )

  private def parseDay(element: Elem): DayParsed = {
    val attributes = XML.openEmpty(element, "day")
    val result = DayParsed(
      span = Span.parseNumbered(attributes),
      custom = attributes.get("custom").fold[Set[Custom]](Set(Custom.Common))(Custom.parse),
      isCombined = attributes.doGetBoolean("combined")
    )
    attributes.close()
    result
  }

  private def combineDays(weeks: Seq[(Parsha, Custom.Sets[Seq[Span.Numbered]])]): Seq[Custom.Of[Seq[Span]]] = weeks match {
    case (parsha, days) :: (parshaNext, daysNext) :: tail =>
      val result: Custom.Of[Seq[Span]] = if (!parsha.combines) Map.empty else  {
        // TODO Use defaults from days?
        val combined: Custom.Sets[Seq[Span.Numbered]] = daysNext ++ days.map { case (customs, value) =>
          (customs, value ++ daysNext.getOrElse(customs, Seq.empty))
        }

        val chapters: Chapters = parsha.book.chapters
        Custom.denormalize(processDays(combined, chapters.merge(parsha.span, parshaNext.span), chapters))
      }

      result +: combineDays((parshaNext, daysNext) +: tail)

    case (parsha, days) :: Nil =>
      require(!parsha.combines)
      // TODO require(days.isEmpty, s"Not empty: $days")
      Seq(Map.empty)

    case Nil => Nil
  }

  private def processDays(
    days: Custom.Sets[Seq[Span.Numbered]],
    span: Span,
    chapters: Chapters
  ): Custom.Sets[Seq[Span]] = {
    val withImplied1 = Span.addImplied1(Custom.common(days), span, chapters)

    days.mapValues { spans: Seq[Span.Numbered] =>
      val overlayedSpans = WithNumber.overlay(withImplied1, spans)
      Span.setImpliedToCheckAndDropNumbers(overlayedSpans, 7, span, chapters)
    }
  }
}

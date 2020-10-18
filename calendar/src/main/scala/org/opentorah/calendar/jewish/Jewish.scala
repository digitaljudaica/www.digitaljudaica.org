package org.opentorah.calendar.jewish

import org.opentorah.metadata.LanguageSpec
import org.opentorah.dates.Calendar

trait Jewish extends Calendar[Jewish] {

  final override type Year = JewishYear

  final override type YearCharacter = (Boolean, Year.Kind)

  final override lazy val Year = new JewishYearCompanion {
    override val numbers: Jewish = Jewish.this
    protected override def newYear(number: Int): Year =
      new JewishYear(number) {
        override val numbers: Jewish = Jewish.this
      }
  }

  final override type Month = JewishMonth

  final override lazy val Month = new JewishMonthCompanion {
    override val numbers: Jewish = Jewish.this
    private[opentorah] override def apply(yearOpt: Option[Year], number: Int): Month =
      new JewishMonth(yearOpt, number) {
        override val numbers: Jewish = Jewish.this
      }
  }

  final override type Day = JewishDay

  final override lazy val Day = new JewishDayCompanion {
    override val numbers: Jewish = Jewish.this
    private[opentorah] override def apply(monthOpt: Option[Month], number: Int): Day =
      new JewishDay(monthOpt, number) {
        override val numbers: Jewish = Jewish.this
      }
  }

  final override type Point = JewishMoment

  final override lazy val Point: JewishMomentCompanion = new JewishMomentCompanion {
    override val numbers: Jewish = Jewish.this
    protected override def newNumber(digits: Seq[Int]): Point =
      new JewishMoment(digits) {
        override val numbers: Jewish = Jewish.this
        final override def companion: JewishMomentCompanion = Point
      }
  }

  final override def toString(number: Int)(implicit spec: LanguageSpec): String = spec.toString(number)
}


object Jewish extends Jewish

package org.podval.calendar.gregorian

import org.podval.calendar.dates.{Calendar, CalendarMember}
import org.podval.calendar.numbers.{Digits, PointCompanion}

class Gregorian private() extends Calendar[Gregorian] {

  trait GregorianCalendarMember extends CalendarMember[Gregorian] {
    final override def calendar: Gregorian = Gregorian.this
  }

  final override type Year = GregorianYear

  final override def createYear(number: Int): Year =
    new GregorianYear(number) with GregorianCalendarMember

  final override type YearCharacter = Boolean

  final override object Year extends GregorianYearCompanion with GregorianCalendarMember

  final override type Month = GregorianMonth

  final override def createMonth(number: Int): Month =
    new GregorianMonth(number) with GregorianCalendarMember

  final override type MonthName = Month.Name

  final override object Month extends GregorianMonthCompanion with GregorianCalendarMember

  final override type Day = GregorianDay

  final override def createDay(number: Int): Day =
    new GregorianDay(number) with GregorianCalendarMember

  final override type DayName = Day.Name

  final override object Day extends GregorianDayCompanion with GregorianCalendarMember

  final override type Point = GregorianMoment

  final override object Point extends PointCompanion[Gregorian] {
    override def numbers: Gregorian = Gregorian.this
    override def apply(digits: Int*): Point = new Digits(digits) with GregorianMoment {
      final override def companion: PointCompanion[Gregorian] = Point
    }
  }
}


object Gregorian extends Gregorian

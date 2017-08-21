package org.podval.calendar.calendar

import org.podval.calendar.util.Numbered

/**
  *
  * @param number  of the Year
  */
abstract class YearBase[C <: Calendar[C]](number: Int)
  extends Numbered[C#Year](number) with CalendarMember[C]
{ this: C#Year =>
  def character: C#YearCharacter

  final def isLeap: Boolean = calendar.Year.isLeap(number)

  final def next: C#Year = this + 1

  final def prev: C#Year = this - 1

  final def +(change: Int): C#Year = calendar.Year(number + change)

  final def -(change: Int): C#Year = calendar.Year(number - change)

  final def firstDay: C#Day = firstMonth.firstDay

  final def lastDay: C#Day = lastMonth.lastDay

  def firstDayNumber: Int

  def lengthInDays: Int

  final def days: Seq[C#Day] = months.flatMap(_.days)

  final def firstMonth: C#Month = month(1)

  final def lastMonth: C#Month = month(lengthInMonths)

  final def firstMonthNumber: Int = calendar.Year.firstMonth(number)

  final def lengthInMonths: Int = calendar.Year.lengthInMonths(number)

  final def months: Seq[C#Month] = (1 to lengthInMonths).map(month)

  final def month(numberInYear: Int): C#Month = {
    require(0 < numberInYear && numberInYear <= lengthInMonths)
    calendar.Month(firstMonthNumber + numberInYear - 1)
  }

  final def month(name: C#MonthName): C#Month =
    month(monthDescriptors.indexWhere(_.name == name) + 1)

  final def monthForDay(day: Int): C#Month = {
    require(0 < day && day <= lengthInDays)
    month(monthDescriptors.count(_.daysBefore < day))
  }

  // TODO this needs to move into YearCompanion for any chance to eliminate YearCharacter?
  final def monthDescriptors: List[C#MonthDescriptor] =
    calendar.Year.monthDescriptors(character)
}

package org.podval.calendar.dates

import org.scalatest.FlatSpec

import Jewish.{Year, Day, DayName, MonthName}
import MonthName._


@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
final class DatesTest extends FlatSpec {

  "known dates" should "have correct day of the week" in {
    assertResult(DayName.Sheni)(Day(5772, Marheshvan, 24).name)
  }


  "conversions from date to days and back" should "end where they started" in {
    date2days2date(1   , Tishrei,  1)
    date2days2date(2   , Tishrei,  1)
    date2days2date(5768, AdarII , 28)
    date2days2date(5769, Nisan  , 14)
  }


  private def date2days2date(yearNumber: Int, monthName: MonthName, dayNumber: Int) {
    val year = Year(yearNumber)
    assertResult(yearNumber)(year.number)

    val month = year.month(monthName)
    assertResult(monthName)(month.name)

    val day = month.day(dayNumber)

    assertResult(year)(day.year)
    assertResult(month)(day.month)
    assertResult(dayNumber)(day.numberInMonth)
  }
}

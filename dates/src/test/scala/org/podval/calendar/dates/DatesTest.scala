/*
 * Copyright 2011-2014 Podval Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.podval.calendar.dates;

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite

import Jewish.{Year, Day, Month}
import Month._


@RunWith(classOf[JUnitRunner])
final class DatesTest extends FunSuite {

  val x = Jewish


  test("known dates should fall on known days of the week") {
    // XXX ???
//    Assert.assertEquals(6, Day(   2, Tishrei   ,  1).numberInWeek)
    assertResult(Day.Sheni)(Day(5772, Marheshvan, 24).name)
  }


  test("conversions from date to days and back should end where they started") {
    val x = Jewish
    date2days2date(1   , Tishrei,  1)
    date2days2date(2   , Tishrei,  1)
    date2days2date(5768, AdarII , 28)
    date2days2date(5769, Nisan  , 14)
  }


  private def date2days2date(yearNumber: Int, monthName: Month.Name, dayNumber: Int) {
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

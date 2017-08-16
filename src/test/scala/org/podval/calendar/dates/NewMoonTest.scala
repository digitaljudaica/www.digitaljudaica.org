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

package org.podval.calendar.dates

import org.scalatest.FlatSpec

import Jewish.Month._
import Gregorian.Month._
import Jewish.Day._


@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
final class NewMoonTest extends FlatSpec {

  "new moons from the printed tables" should "calculate correctly" in {
    // see http://www.owen0001.host-ed.me/cal/moladot.php

    // year and month for the molad; jewish date; georgian date; georgian time
    import Jewish.DayName._

    newMoon(5769, Tishrei   , Shlishi ,  5769, Tishrei,  1,  2008, September, 30,  1, 58, 13)
    newMoon(5769, Marheshvan, Rvii    ,  5769, Tishrei, 30,  2008, October  , 29, 14, 42, 14)
    newMoon(5769, Kislev    , Shishi  ,  5769, Kislev ,  1,  2008, November , 28,  3, 26, 15)

    newMoon(5771, Tishrei   , Chamishi,  5771, Tishrei,  1,  2010, September,  8, 19, 36,  1)

    // TODO Elul 28?!
//    newMoon(5772, Tishrei   , Shlishi ,  5771, Elul   , 27,  2011, September, 27, 17,  8, 14)
    newMoon(5772, Marheshvan, Chamishi,  5772, Tishrei, 29,  2011, October  , 27,  5, 52, 15)

    newMoon(5773, Tishrei   , Rishon  ,  5772, Elul   , 29,  2012, September, 16,  1, 57,  8)
    newMoon(5773, Adar      , Rishon  ,  5773, Shvat  , 30,  2013, February , 10, 17, 37, 13)
    newMoon(5773, Nisan     , Shlishi ,  5773, Nisan  ,  1,  2013, March    , 12,  6, 21, 14)
  }



  private def newMoon(moladYear: Int, moladMonth: Jewish.Month.Name, dayOfWeek: Jewish.DayName,
              year: Int, month: Jewish.Month.Name, day: Int,
              yearG: Int, monthG: Gregorian.Month.Name, dayG: Int,
              hours: Int, minutes: Int, parts: Int)
  {
    val dayJ = Jewish.Day(year, month, day)
    val dateG = Gregorian.Day(yearG, monthG, dayG).toMoment.hours(hours).minutes(minutes).partsWithoutMinutes(parts)
    val molad = Jewish.Year(moladYear).month(moladMonth).newMoon

    assertResult(dayOfWeek)(molad.day.name)
    assertResult(dayJ)(molad.day)

    val dateJ = Conversions.toJewish(dateG)
    assertResult(dayJ)(dateJ.day)

    val moladG = Conversions.fromJewish(molad)
    assertResult(dateG)(moladG)
  }
}

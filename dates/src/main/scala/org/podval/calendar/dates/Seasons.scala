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

import Jewish._


// TODO Where and when was the Sun created? Does this jibe with Rambam's epoch?
// TODO Which day of the week (+1/-1) was the Giving of the Law? (Sema)
// TODO Rambam's epoch - two days after molad?! (Petya Ofman)
// TODO Rename/dissolve?
// TODO angular speed of the moon = 360 / (1/tropical month + 1/solar year)
object Seasons {

  // KH 9:3

  // TODO add convenience methods to clean this up

  val firstTkufasNissan = Year(1).month(Month.Nisan).newMoon - days(7).hours(9).parts(642)  // KH 9:3


  val yearOfShmuel = days(365) + hours(6)


  // TODO up to moments; 1 part = 76 moments
  val yearOfRavAda = Month.meanLunarPeriod * Year.monthsInCycle / 19


  // Sun enters Teleh  KH 9:3
//  def tkufasNissan(year: Int) = firstTkufasNissan + yearOfRavAda * (year-1)
  def tkufasNissan(year: Int) = firstTkufasNissan + yearOfShmuel * (year-1)


  // Tkufas Tammuz - Sartan; Tishrei - Moznaim; Teves - Gdi.  KH 9:3





  // Since Birkas HaChama is said in the morning, we add 12 hours to the time of the equinox
  // Sanctification of the Sun falls from Adar 10 to Nissan 26.
  // Only 27 days in Adar and Nissan have have the sanctification of the Sun happen on them at least once.
  // It never happens on Passover.
  // It happens more often than on the Passover Eve on 7 days.
  def birkasHachama(cycle: Int) = firstTkufasNissan + yearOfShmuel * 28 * cycle + hours(12)


  def main(args: Array[String]) {
    println(Conversions.fromJewish(birkasHachama(206)))
  }
}

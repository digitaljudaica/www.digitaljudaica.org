package org.podval.calendar.dates

/*
 * Copyright 2014 Podval Group.
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
abstract class TimeNumberSystem extends {

  final val hoursPerDay = 24


  final val partsPerHour = 1080


  final val momentsPerPart = 76


  protected final override val signs: List[String] = List("d", "h", "p", "m")


  protected final override val ranges: List[Int] = List(hoursPerDay, partsPerHour, momentsPerPart)


  protected final override val headRange: Option[Int] = None

} with NumberSystem {

  require(hoursPerDay % 2 == 0)


  final val hoursPerHalfDay = hoursPerDay / 2


  private val minutesPerHour = 60


  require(partsPerHour % minutesPerHour == 0)


  final val partsPerMinute = partsPerHour / minutesPerHour


  protected final override type Interval = TimeInterval


  protected final override val intervalCreator: Creator[Interval] = TimeInterval.apply



  trait TimeNumber extends Number {

    final def days: Int = head


    final def days(value: Int): SelfType = digit(0, value)


    final def day(number: Int): SelfType = days(number-1)


    final def hours: Int = digit(1)


    final def hours(value: Int): SelfType = digit(1, value)


    final def firstHalfHours(value: Int): SelfType = {
      require(0 <= hours && hours < hoursPerHalfDay)
      hours(value)
    }


    final def secondHalfHours(value: Int): SelfType = {
      require(0 <= value && value < hoursPerHalfDay)
      hours(value + hoursPerHalfDay)
    }


    final def parts: Int = digit(2)


    final def parts(value: Int): SelfType = digit(2, value)


    final def minutes: Int = parts / partsPerMinute


    final def minutes(value: Int): SelfType = parts(value*partsPerMinute+partsWithoutMinutes)


    final def partsWithoutMinutes: Int = parts % partsPerMinute


    final def partsWithoutMinutes(value: Int): SelfType = parts(minutes*partsPerMinute+value)


    final def moments: Int = digit(3)


    final def moments(value: Int): SelfType = digit(3, value)
  }



  abstract class TimePoint(negative: Boolean, digits: List[Int]) extends NumberBase(negative, digits) with TimeNumber with PointBase



  final class TimeInterval(negative: Boolean, digits: List[Int]) extends NumberBase(negative, digits) with TimeNumber with IntervalBase



  object TimeInterval {

    def apply(negative: Boolean, digits: List[Int]): TimeInterval = new TimeInterval(negative, digits)
  }
}

package org.podval.calendar.dates.jewish

import org.scalatest.FlatSpec
import Jewish.Year


@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class IntervalTest extends FlatSpec {

  "division for the year of Rav Ada" should "be correct" in {
    val yearOfRavAda = Year.cycleLength / (Year.yearsInCycle, 2)
    // TODO compare with values from Sun.scala?
    println(yearOfRavAda)
  }
}

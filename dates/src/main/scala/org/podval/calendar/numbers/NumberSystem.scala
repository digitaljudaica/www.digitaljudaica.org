package org.podval.calendar.numbers

trait NumberSystem[S <: NumberSystem[S]] { this: S =>

  type Point <: PointBase[S]

  def createPoint(digits: Seq[Int]): Point

  val Point: PointCompanion[S]

  type Interval <: IntervalBase[S]

  def createInterval(digits: Seq[Int]): Interval

  val Interval: IntervalCompanion[S]

  val defaultLength: Int

  def correctHeadDigit(value: Int): Int = value

  /**
    *
    * @param position within the tail
    * @return positive, even number
    */
  def range(position: Int): Int

  def headSign: String

  val signPartial: PartialFunction[Int, String]

  final def sign(position: Int): Option[String] = signPartial.lift(position)
}

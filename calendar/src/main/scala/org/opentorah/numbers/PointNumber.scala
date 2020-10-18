package org.opentorah.numbers

/** Point from the number system `S`.
  *
  * @tparam S  type of the number system
  */
abstract class PointNumber[S <: Numbers[S]](digits: Digits) extends Number[S, S#Point](digits) { this: S#Point =>

  /** Returns Point resulting from adding specified Vector to this one. */
  final def +(that: S#Vector): S#Point = numbers.Point.fromDigits(add(that))

  /** Returns Point resulting subtracting specified Vector to this one. */
  final def -(that: S#Vector): S#Point = numbers.Point.fromDigits(subtract(that))
}

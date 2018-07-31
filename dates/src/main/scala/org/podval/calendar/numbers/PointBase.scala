package org.podval.calendar.numbers

trait PointBase[S <: Numbers[S]] extends Number[S, S#Point]
{ this: S#Point =>
  final def +(that: S#Vector): S#Point = companion.fromDigits(add(that))

  final def -(that: S#Vector): S#Point = companion.fromDigits(subtract(that))

  final def -(that: S#Point): S#Vector = numbers.Vector.fromDigits(subtract(that))

  final override def toVector: S#Vector = this - companion.zero
}

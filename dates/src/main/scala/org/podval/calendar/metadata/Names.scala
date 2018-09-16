package org.podval.calendar.metadata

final class Names(val names: Seq[Name]) {
  // No duplicates
  require(names.size == names.map(_.name).toSet.size, s"Different sizes: $names and ${names.map(_.name).toSet}")
  // TODO check that there are no duplicate combinations of parameters OTHER than name!

  def isEmpty: Boolean = names.isEmpty

  def find(name: String): Option[Name] = names.find(_.name == name)

  def has(name: String): Boolean = find(name).isDefined

  def find(spec: LanguageSpec): Option[Name] = names.find(_.satisfies(spec))

  def doFind(spec: LanguageSpec): Name =
    find(spec)
      .orElse(find(spec.dropFlavour))
      .orElse(find(spec.dropFlavour.dropIsTransliterated))
      .orElse(find(spec.dropFlavour.dropIsTransliterated.dropLanguage))
      .get

  override def toString: String = names.mkString("Names(", ", ", ")")

  def isDisjoint(other: Names): Boolean = names.forall(name => !other.has(name.name))
}

object Names {
  trait HasNames {
    def names: Names

    // TODO toString = names.doFind(LanguageSpec.empty).name
  }

  trait NamedBase extends HasNames {
    def name: String = Named.className(this)

    override def toString: String = name

    final def toString(spec: LanguageSpec): String = names.doFind(spec).name
  }

  def checkDisjoint(nameses: Seq[Names]): Unit = {
    for {
      one <- nameses
      other <- nameses if !other.eq(one)
    } yield {
      require(one.isDisjoint(other), s"Names overlap: $one and $other")
    }
  }

  def merge(one: Names, other: Names): Names =
    if (other.isEmpty) one else throw new IllegalArgumentException(s"Merging Names not implemented: $one with $other")
}

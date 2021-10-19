package org.opentorah.collector

import org.opentorah.site.HtmlContent
import org.opentorah.store.{By, Path, Pure}
import org.opentorah.xml.{Element, Parsable, Parser, ScalaXml, Unparser}

final class ByHierarchy(
  override val fromUrl: Element.FromUrl,
  selectorName: String,
  val hierarchyStores: Seq[Hierarchical]
) extends
  By.WithSelector[Hierarchical](selectorName),
  Pure[Hierarchical],
  Element.FromUrl.With:

  override def storesPure: Seq[Hierarchical] = hierarchyStores

  // TODO generate hierarchy root index and reference it from the summary.
  // TODO allow viewing tree indexes rooted in intermediate ByHierarchys.

  def oneLevelIndex(path: Path, collector: Collector): ScalaXml.Element =
    <p>
      <l>{Hierarchical.displayName(this)}:</l>
      <ul>{
        for hierarchical <- hierarchyStores yield
          val hierarchicalPath: Path = path :+ hierarchical
          <li>
            {reference(hierarchicalPath, hierarchical, collector)}
          </li>
      }</ul>
    </p>

  def treeIndex(path: Path, collector: Collector): ScalaXml.Element =
    <div class="tree-index">
      <ul>
        <li><em>{Hierarchical.displayName(this)}</em></li>
        <li>
          <ul>{
            for hierarchical <- hierarchyStores yield
              val hierarchicalPath: Path = path :+ hierarchical
              <li>
                {reference(hierarchicalPath, hierarchical, collector)}
                {hierarchical.getBy.toSeq.map(by => by.treeIndex(hierarchicalPath :+ by, collector))}
              </li>
          }</ul>
        </li>
      </ul>
    </div>

  private def reference(path: Path, hierarchical: Hierarchical, collector: Collector): ScalaXml.Element =
    collector.a(path)(text = hierarchical.displayTitle)

object ByHierarchy extends Element[ByHierarchy]("by"):
  override def contentParsable: Parsable[ByHierarchy] = new Parsable[ByHierarchy]:
    override def parser: Parser[ByHierarchy] = for
      fromUrl: Element.FromUrl <- Element.fromUrl
      selectorName: String <- By.selectorParser
      hierarchyStores: Seq[Hierarchical] <- Hierarchical.followRedirects.seq()
    yield ByHierarchy(
      fromUrl,
      selectorName,
      hierarchyStores
    )

    override def unparser: Unparser[ByHierarchy] = Unparser.concat(
      By.selectorUnparser,
      Hierarchical.seq(_.hierarchyStores)
    )

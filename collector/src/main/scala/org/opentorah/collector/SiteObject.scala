package org.opentorah.collector

import org.opentorah.store.Path
import org.opentorah.tei.Tei
import org.opentorah.util.Files
import org.opentorah.xml.Xml
import scala.xml.Elem

abstract class SiteObject(val site: Site) {

  final val teiFile: SiteFile = new SiteFile {
    override def url: Seq[String] = teiUrl

    override def content: String = {
      val elem: Elem = Xml.transform(
        xml = Tei.toXml(teiTransformer(tei)),
        transformer = xmlTransformer
      )
      Xml.header + "\n" + Transformers.teiPrettyPrinter.render(elem) +  "\n"
    }
  }

  final val teiWrapperFile: SiteFile = new TeiWrapperFile {
    override def viewer: Viewer = teiWrapperViewer

    override def url: Seq[String] = teiWrapperUrl

    final def content: String = SiteObject.withYaml(
      yaml = Seq("target" -> viewer.name) ++ yaml,
      content = Seq(SiteObject.loadTei(Files.mkUrl(teiFile.url)))
    )
  }

  protected def teiUrl: Seq[String]

  protected def teiTransformer: Tei.Transformer =
    Transformers.addPublicationStatement compose Transformers.addSourceDesc compose Transformers.addLanguage

  protected def xmlTransformer: Xml.Transformer =
    Transformers.refTransformer(site) compose Transformers.nameTransformer(site)

  protected def tei: Tei

  protected def teiWrapperViewer: Viewer

  protected def teiWrapperUrl: Seq[String]

  protected def yaml: Seq[(String, String)] = Seq.empty
}

object SiteObject {

  def loadTei(tei: String): String =
    s"<script type='module'>import loadTei from '/js/tei.js'; loadTei('$tei');</script>"

  def resolve(site: Site, parts: Seq[String]): Option[SiteFile] = {
    if (parts.isEmpty) Some(new IndexObject(site).teiWrapperFile) else {
      val tail: Seq[String] = parts.tail
      parts.head match {
        case Hierarchy       .directoryName => HierarchyObject .resolve(site, Path.empty, site.store, tail)
        case CollectionObject.directoryName => CollectionObject.resolve(site, tail)
        case EntityObject    .directoryName => EntityObject    .resolve(site, tail)
        case ReportObject    .directoryName => ReportObject    .resolve(site, tail)

        case file if parts.tail.isEmpty =>
          val (fileName: String, extension: Option[String]) = Files.nameAndExtension(file)
          val result: Option[SimpleSiteObject] = fileName match {
            case IndexObject    .fileName => Some(new IndexObject    (site))
            case TreeIndexObject.fileName => Some(new TreeIndexObject(site))
            case NamesObject    .fileName => Some(new NamesObject    (site))
            case _ => None
          }
          result.flatMap(k => SimpleSiteObject.resolve(extension, k))
            // Assume that this is a collection reference:
            .orElse(CollectionObject.resolve(site, parts))

        // Assume that this is a collection reference:
        case _ => CollectionObject.resolve(site, parts)
      }
    }
  }

  def withYaml(
    yaml: Seq[(String, String)],
    content: Seq[String] = Seq.empty
  ): String = {
    val result: Seq[String] =
      Seq("---") ++
      (for ((name, value) <- ("layout" -> "default") +: yaml) yield name + ": " + quote(value)) ++
      Seq("---") ++
      Seq("") ++ content

    result.mkString("", "\n", if (content.nonEmpty) "\n" else "")
  }

  private def quote(what: String): String = s"'$what'"
}

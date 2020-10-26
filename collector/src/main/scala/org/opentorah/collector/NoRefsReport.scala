package org.opentorah.collector

import org.opentorah.store.WithPath
import org.opentorah.tei.{EntityName, EntityReference, Tei}
import scala.xml.Elem

final class NoRefsReport(site: Site) extends ReportObject[WithPath[EntityReference]](site) {

  override def fileName: String = "no-refs"

  override protected def viewer: Viewer = Viewer.Names

  override def title: Option[String] = Some("Имена без атрибута /ref/")

  override protected def lines: Seq[WithPath[EntityReference]] =
    site.references.filter(_.value.ref.isEmpty)

  override protected def lineToXml(reference: WithPath[EntityReference]): Elem = {
    // TODO give a link to the ref:
    // TODO call one method to get the ref?
    val collectionName: String = Hierarchy.referenceCollectionName(reference)
    val documentName: String = Hierarchy.storeName(reference.path.last.store)
    val ref: String = s"/$collectionName/$documentName"

    <l xmlns={Tei.namespace.uri}>
      {EntityName.toXmlElement(EntityName.forReference(reference.value))} в {ref}
    </l>
  }

  override def simpleSubObjects: Seq[SimpleSiteObject] = Seq.empty
}

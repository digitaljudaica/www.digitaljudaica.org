package org.opentorah.collector

import org.opentorah.store.By
import org.opentorah.tei.Tei
import org.opentorah.xml.Caching

abstract class CollectionFacet(val collection: Collection) extends By[Facet]:

  final override def findByName(name: String): Caching.Parser[Option[Facet]] =
    collection.documents.findByName(name).map(_.map(of))

  final override def stores: Caching.Parser[Seq[Facet]] =
    collection.documents.stores.map(_.map(of))

  final def getTei(document: Document): Caching.Parser[Tei] =
    collection.documents.getFile(document)

  def of(document: Document): Facet

  def isText: Boolean

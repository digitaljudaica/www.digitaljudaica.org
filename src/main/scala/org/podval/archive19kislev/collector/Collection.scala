package org.podval.archive19kislev.collector

import java.io.{BufferedWriter, File, FileWriter}

import scala.xml.{Elem, Node, Text, TopScope}
import Xml.Ops

final class Collection(val directoryName: String, val title: String) {
  private val collectionDirectory = new File(Main.docsDirectory, directoryName)
  private val teiDirectory = new File(collectionDirectory, Collection.teiDirectoryName)
  private val facsimilesDirectory = new File(collectionDirectory, Collection.facsimilesDirectoryName)
  private val documentsDirectory = new File(collectionDirectory, Collection.documentsDirectoryName)
  private val viewersDirectory = new File(collectionDirectory, Collection.viewersDirectoryName)

  // Read

  val documents: Seq[Document] = {
    val names: Seq[String] = Collection.listNames(teiDirectory, ".xml", Page.checkBase)

    val namesWithSiblings: Seq[(String, (Option[String], Option[String]))] = {
      val documentOptions: Seq[Option[String]] = names.map(Some(_))
      val prev = None +: documentOptions.init
      val next = documentOptions.tail :+ None
      names.zip(prev.zip(next))
    }

    for ((name, (prev, next)) <- namesWithSiblings)
    yield new Document(Xml.load(teiDirectory, name), name, prev, next)
  }

  val pages: Seq[Page] = documents.flatMap(_.pages)

  val missingPages: Seq[String] = pages.filterNot(_.isPresent).map(_.displayName)

  /// Check consistency
  check()

  private def check(): Unit = {
    // Check for duplicates
    val name2page = collection.mutable.Map[String, Page]()
    for (page <- pages) {
      // TODO allow duplicates in consecutive documents
      //      if (name2page.contains(page.name)) throw new IllegalArgumentException(s"Duplicate page: ${page.name}")
      name2page.put(page.name, page)
    }

    // Check that all the images are accounted for
    val imageNames: Seq[String] = Collection.listNames(facsimilesDirectory, ".jpg", Page.check)
    val orphanImages: Seq[String] = (imageNames.toSet -- pages.map(_.name).toSet).toSeq.sorted
    if (orphanImages.nonEmpty) throw new IllegalArgumentException(s"Orphan images: $orphanImages")
  }

  // TODO check order

  def writeIndex(): Unit = {
    val content =
      <TEI xmlns="http://www.tei-c.org/ns/1.0">
        <teiHeader>
          <fileDesc>
            <publicationStmt>
              <publisher><ptr target="www.alter-rebbe.org"/></publisher>
              <availability status="free">
                <licence><ab><ref n="license" target="http://creativecommons.org/licenses/by/4.0/">
                  Creative Commons Attribution 4.0 International License </ref></ab></licence>
              </availability>
            </publicationStmt>
            <sourceDesc><p>Facsimile</p></sourceDesc>
          </fileDesc>
          <profileDesc><calendarDesc><calendar xml:id="julian"><p>Julian calendar</p></calendar></calendarDesc></profileDesc>
        </teiHeader>
        <text>
          <body>
            {Collection.table.toTei(documents)}
            {if (missingPages.isEmpty) Seq.empty
             else <p>Отсутствуют фотографии страниц: {missingPages.mkString(" ")}</p>}
          </body>
        </text>
      </TEI>


    // Index
    Collection.write(collectionDirectory, "index.xml", content =
      """<?xml version="1.0" encoding="UTF-8"?>""" + "\n" +
      """<?xml-model href="http://www.tei-c.org/release/xml/tei/custom/schema/relaxng/tei_all.rng" schematypens="http://relaxng.org/ns/structure/1.0"?>""" + "\n" +
      Xml.prettyPrinter.format(content)
    )

    // Index wrapper
    Collection.write(collectionDirectory, "index.html", Seq(
      "title" -> title,
      "layout" -> "tei",
      "tei" -> "index.xml",
      "wide" -> "true"
    ))
  }

  def writeWrappers(): Unit = {
    for (document <- documents) {
      def documentName(what: String, name: String): Seq[(String, String)] = Seq(what -> s"'$name'")

      val navigation: Seq[(String, String)] = documentName("self", document.name) ++
        document.prev.map(prev => documentName("prev", prev)).getOrElse(Seq.empty) ++
        document.next.map(next => documentName("next", next)).getOrElse(Seq.empty)

      // TEI wrapper
      Collection.write(documentsDirectory, s"${document.name}.html", Seq(
        "layout" -> "tei",
        "tei" -> s"'../${Collection.teiDirectoryName}/${document.name}.xml'"
      ) ++ navigation
      )

      // Facsimile viewer
      Collection.write(viewersDirectory, s"${document.name}.html", Seq(
        "layout" -> "facsimile",
        "images" -> document.pages.filter(_.isPresent).map(_.name).mkString("[", ", ", "]")
      ) ++ navigation
      )
    }
  }
}

object Collection {
  val teiDirectoryName: String = "tei"
  val facsimilesDirectoryName: String = "facsimiles"
  val documentsDirectoryName: String = "documents" // wrappers for TEI XML
  val viewersDirectoryName: String = "facs" // facsimile viewers

  private val table: Table[Document] = new Table[Document](
    _.partTitle.toSeq.map(partTitle =>  <span rendition="part-title">{partTitle.child}</span>),
    Column.elem("Описание", "description", _.description),
    Column.string("Дата", "date", _.date),
    Column.elem("Кто", "author", _.author),
    new Column("Кому", "addressee",  _.addressee.fold[Seq[Node]](Text(""))(addressee =>
      <persName ref={addressee.ref.map("#" + _).orNull}>{addressee.name}</persName>)),
    Column.string("Язык", "language", _.language),
    new Column("Документ", "document", document =>
      <ref target={documentPath(document)} role="documentViewer">{document.name}</ref>),
    new Column("Страницы", "pages", document => for (page <- document.pages) yield
      <ref target={documentPath(document) + s"#p${page.name}"} role="documentViewer"
           rendition={if (page.isPresent) "page" else "missing-page"}>{page.displayName}</ref>),
    Column.elem("Расшифровка", "transcriber", _.transcriber)
  )

  private def transplant(f: Document => Option[Elem]): Document => Seq[Node] = (document: Document) =>
    f(document).fold[Seq[Node]](Text("")) { _.child.map {
      case e: Elem => e.copy(scope = TopScope)
      case n => n
    }}

  private def documentPath(document: Document): String =
    s"${Collection.documentsDirectoryName}/${document.name}.html"

  private def listNames(directory: File, extension: String, check: String => Unit): Seq[String] = {
    val result = directory.listFiles.toSeq.map(_.getName).filter(_.endsWith(extension)).map(_.dropRight(extension.length))
    //    result.foreach(check)
    result.sorted
  }

  private def write(
    directory: File,
    fileName: String,
    yaml: Seq[(String, String)]
  ): Unit = {
    val result: Seq[String] =
      Seq("---") ++
        (for ((name, value) <- yaml) yield name + ": " + value) ++
        Seq("---") ++
        Seq("")

    Collection.write(
      directory,
      fileName,
      result.mkString("\n")
    )
  }

  private def write(
    directory: File,
    fileName: String,
    content: String
  ): Unit = {
    directory.mkdirs()
    val file = new File(directory, fileName)
    val writer: BufferedWriter = new BufferedWriter(new FileWriter(file))
    try {
      writer.write(content)
    } finally {
      writer.close()
    }
  }
}

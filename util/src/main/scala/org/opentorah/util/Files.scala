package org.opentorah.util

import java.io.{BufferedWriter, File, FileWriter}
import java.net.{URI, URL, URLDecoder}
import org.slf4j.{Logger, LoggerFactory}
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import scala.io.Source

object Files:
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def filesWithExtensions(directory: File, extension: String): Seq[String] =
    if !directory.exists then Seq.empty else for
      file <- directory.listFiles.toSeq
      result = nameAndExtension(file.getName)
      if result._2.contains(extension)
    yield result._1

  def dropAllowedExtension(nameWihtExtension: String, allowedExtension: String): String =
    val (name: String, extension: Option[String]) = nameAndExtension(nameWihtExtension)
    if extension.nonEmpty && !extension.contains(allowedExtension) then
      throw IllegalArgumentException(s"Extension must be '$allowedExtension' if present: $nameWihtExtension")
    name

  def nameAndExtension(fullName: String): (String, Option[String]) = Strings.split(fullName, '.')

  def write(file: File, replace: Boolean, content: String): Unit =
    if !replace && file.exists then logger.debug(s"Already exists: $file")
    else write(file, content)

  def write(file: File, content: String): Unit =
    logger.debug(s"Writing $file")
    file.getParentFile.mkdirs()
    val writer: BufferedWriter = BufferedWriter(new FileWriter(file))
    try writer.write(content) finally writer.close()

  def read(file: File): Seq[String] =
    val source = Source.fromFile(file)
    // Note: toList materializes the iterator before closing the source
    val result = source.getLines().toList
    source.close
    result

  def readBytes(file: File): Array[Byte] =
    java.nio.file.Files.readAllBytes(Paths.get(file.toURI))

  def writeBytes(file: File, content: Array[Byte]): Unit =
    java.nio.file.Files.write(Paths.get(file.toURI), content)

  def deleteFiles(directory: File): Unit = if directory.exists() then
    if directory.isDirectory then for file <- directory.listFiles() do deleteFiles(file)
    directory.delete()

  def url2file(url: URL): File = Paths.get(url.toURI).toFile

  def file2url(file: File): URL = file.toURI.toURL

  def string2url(string: String): URL = URI.create(string).toURL

  def subdirectory(url: URL, subdirectoryName: String): URL = subUrl(url, subdirectoryName + "/")

  def fileInDirectory(url: URL, fileName: String): URL = subUrl(url, fileName)

  def pathUnder(url: URL, path: String): URL = subUrl(url, if path.startsWith("/") then path.drop(1) else path)

  def subUrl(base: Option[URL], url: String): URL = base.fold(URI(url).toURL)(subUrl(_, url))

  private def subUrl(base: URL, url: String): URL = base.toURI.resolve(url).toURL

  //def getParent(url: URL): URL = new URL(url, "..")

  def splitUrl(urlRaw: String): Seq[String] =
    val url: String = if urlRaw.isEmpty then "/" else urlRaw
    val startsWithSlash: Boolean = url.startsWith("/")
    // TODO? require(startsWithSlash)
    (if startsWithSlash then url.substring(1) else url).split("/").toIndexedSeq.filterNot(_.isBlank)

  def splitAndDecodeUrl(url: String): Seq[String] = splitUrl(url).map(urlDecode)

  private def urlDecode(segment: String): String = URLDecoder.decode(segment, StandardCharsets.UTF_8)

  def mkUrl(segments: Seq[String]): String = segments.mkString("/", "/", "")

  def file(directory: File, segments: String*): File = fileSeq(directory, segments)

  @scala.annotation.tailrec
  def fileSeq(directory: File, segments: Seq[String]): File =
    if segments.isEmpty then directory
    else fileSeq(File(directory, segments.head), segments.tail)

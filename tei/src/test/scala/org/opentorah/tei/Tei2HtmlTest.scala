package org.opentorah.tei

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.opentorah.xml.{From, Parser, LinkResolver}
import scala.xml.Elem

final class Tei2HtmlTest extends AnyFlatSpec with Matchers {

  private def tei2html(element: Elem): Elem = {
//    println(Xhtml.prettyPrinter.render(element))
    val resolver = new LinkResolver {
      override def resolve(url: Seq[String]): Option[LinkResolver.Resolved] = None
      override def findByRef(ref:  String): Option[LinkResolver.Resolved] = None
      override def facs: LinkResolver.Resolved = LinkResolver.Resolved(
        url = Seq("facsimiles"),
        role = Some("facsViewer")
      )
    }

    Tei2Html.transform(resolver, element)
  }

  "905" should "work" in {
    val tei: Tei = Parser.parseDo(Tei.parse(From.resource(Tei, "905")))
    tei2html(Tei.toXmlElement(tei))
  }
}

package org.podval.fop.mathjax

import org.podval.fop.util.Util.mapValues
import com.eclipsesource.v8.V8
import scala.jdk.CollectionConverters._

object J2V8 {

  def setNativeLibraryLoaded(): Unit = {
    val field = classOf[V8].getDeclaredField("nativeLibraryLoaded")
    field.setAccessible(true)
    field.set(null, true)
  }

  def map2java(map: Map[String, Any]): java.util.Map[String, Any] =
    mapValues(map)(value2java).asJava

  def list2java(list: List[Any]): java.util.List[Any] =
    list.map(value2java).asJava

  private def value2java(value: Any): Any = value match {
    case value: Map[String, Any] => map2java(value)
    case value: List[Any] => list2java(value)
    case other => other
  }
}

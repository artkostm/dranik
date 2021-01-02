package io.github.artkostm.dranik.transport

import io.github.artkostm.dranik.transport.MapCrawler.Rectangle
import zio.{Task, ZIO}

trait MapCrawler {
  def sliding(call: Rectangle => Task[Unit]): Task[Unit] =
    ZIO.foreach_(MapCrawler.matrix((BigDecimal("53.760473464187534"), BigDecimal("27.263404244164413")), (BigDecimal("54.042088515398746"), BigDecimal("27.76189333742789")))) {
      row => ZIO.foreach_(row)(call)
    }
}

object MapCrawler {
  type LatLng = (BigDecimal, BigDecimal)
  type Rectangle = (LatLng, LatLng)
  type Matrix = Seq[Seq[Rectangle]]

  def matrix(start: LatLng, end: LatLng, minStep: LatLng = (BigDecimal("0.024050511222313276"), BigDecimal("0.06497383117675426"))): Matrix = {
    val (slat, slng) = start
    val (elat, elng) = end
    val (latStep, lngStep) = minStep
    val latSteps = (elat - slat) / latStep
    val lngSteps = (elng - slng) / lngStep

    Seq.tabulate(latSteps.intValue + 1, lngSteps.intValue + 1) {
      case (lt, ln) => ((latStep * lt + slat, lngStep * ln + slng), (latStep * (lt + 1) + slat, lngStep * (ln + 1) + slng))
    }
  }
  // 53.925014481308594 - 53.94906499253091 = abs 0.024050511222313276 lat
  // 27.582092247337055 - 27.64706607851381 = abs 0.06497383117675426 lng

  def main(args: Array[String]): Unit = {
    MapCrawler.matrix((BigDecimal("53.760473464187534"), BigDecimal("27.263404244164413")), (BigDecimal("54.042088515398746"), BigDecimal("27.76189333742789")))
      .foreach{ row =>
        row.foreach(cl => print(s"""\t[${cl._1}\\${cl._2}]"""))
        println()
      }
  }
}


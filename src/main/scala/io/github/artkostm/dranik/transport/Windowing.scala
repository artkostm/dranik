package io.github.artkostm.dranik.transport

import io.github.artkostm.dranik.transport.Windowing._
import zio.{Task, ZIO}

trait Windowing {

  def sliding(rootWindow: Window, minStep: LatLng)(call: Window => Task[Unit]): Task[Unit] =
    rootWindow match {
      case (start, end) =>
        // Onliner server here does not allow parallel request execution
        ZIO.foreach_(mainFrame(start, end, minStep)) { row => ZIO.foreach_(row)(call) }
    }
}

object Windowing {
  type LatLng = (BigDecimal, BigDecimal)
  type Window = (LatLng, LatLng)
  type Frame  = Seq[Seq[Window]]

  // todo: add to config
  def mainFrame(start: LatLng,
                end: LatLng,
                minStep: LatLng =
                (BigDecimal("0.01"), BigDecimal("0.04"))): Frame = {
    val (slat, slng)       = start
    val (elat, elng)       = end
    val (latStep, lngStep) = minStep
    val latSteps           = (elat - slat) / latStep
    val lngSteps           = (elng - slng) / lngStep

    Seq.tabulate(latSteps.intValue + 1, lngSteps.intValue + 1) {
      case (lt, ln) =>
        ((latStep * lt + slat, lngStep * ln + slng),
          (latStep * (lt + 1) + slat, lngStep * (ln + 1) + slng))
    }
  }
}

package io.github.artkostm.dranik.utils

import org.slf4j.Logger
import zio.UIO

object logger {

  def error(msg: String)(implicit log: Logger): UIO[Unit] = UIO.effectTotal(log.error(msg))
  def info(msg: String)(implicit log: Logger): UIO[Unit]  = UIO.effectTotal(log.info(msg))

  def error(msg: String, error: Throwable)(implicit log: Logger): UIO[Unit] =
    UIO.effectTotal(log.error(msg, error))
}

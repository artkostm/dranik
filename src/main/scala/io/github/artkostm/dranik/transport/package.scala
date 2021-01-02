package io.github.artkostm.dranik

import io.github.artkostm.dranik.client.OnlinerClient
import io.github.artkostm.dranik.fs.FS
import zio.{Has, Task, ZIO, ZLayer}

package object transport {
  type DataTransport = Has[DataTransport.Service]

  object DataTransport {
    trait Service {
      def downloadApartments(): Task[Unit]
    }

    val live = ZLayer.fromServices[OnlinerClient.Service, FS.Service, DataTransport.Service] {
      (client, fs) => new OnlinerDataTransport(fs, client)
    }
  }

  final def downloadApartments(): ZIO[DataTransport, Throwable, Unit] =
    ZIO.accessM(_.get.downloadApartments())
}

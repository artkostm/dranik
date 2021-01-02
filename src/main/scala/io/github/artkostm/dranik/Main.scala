package io.github.artkostm.dranik

import io.github.artkostm.dranik.Cli.TransportAppConfig
import io.github.artkostm.dranik.client.{ClientConfig, OnlinerBackend, OnlinerClient}
import io.github.artkostm.dranik.fs.FS
import io.github.artkostm.dranik.transport.DataTransport
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._

object Main extends App {
  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    Cli
      .parse(args)
      .flatMap { config =>
        dependencies(config).build.use { env =>
          transport.downloadApartments().provide(env)
        }
      }
      .exitCode

  def dependencies(config: TransportAppConfig): ZLayer[Any, Nothing, DataTransport] = {
    val appConfig = ZLayer.succeed[TransportAppConfig](config)
    val fs =
      if (config.debug) appConfig >>> FS.local
      else {
        val container = appConfig >>> FS.azureBlobContainer
        (appConfig ++ container) >>> FS.azure
      }

    val clientConfig = ZLayer.succeed(ClientConfig(config.url))
    val clientBackend: ZLayer[Any, Nothing, Has[OnlinerBackend]] =
      ZLayer.fromManaged(AsyncHttpClientZioBackend.managed()).orDie
    val client = (clientBackend ++ clientConfig) >>> OnlinerClient.live

    (client ++ fs) >>> DataTransport.live
  }
}

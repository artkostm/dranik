package io.github.artkostm.dranik

import io.github.artkostm.dranik.Cli.TransportAppConfig
import io.github.artkostm.dranik.client.{ClientConfig, OnlinerBackend, OnlinerClient}
import io.github.artkostm.dranik.fs.FS
import io.github.artkostm.dranik.transport.DataTransport
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._
import zio.logging.LogAnnotation.CorrelationId
import zio.logging._
import zio.logging.slf4j._

object Main extends App {
  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    Cli
      .parse(args)
      .flatMap { config =>
        dependencies(config).build.use { env =>
          log
            .locally(CorrelationId(config.correlationId)) {
              transport
                .downloadApartments()
                .tapError(error => log.throwable("Program finished with an error: ", error))
            }
            .provide(env)
        }
      }
      .exitCode

  def dependencies(config: TransportAppConfig): ZLayer[Any, Nothing, DataTransport with Logging] = {
    val appConfig = ZLayer.succeed[TransportAppConfig](config)
    val logger = Slf4jLogger.make { (context, message) =>
      "[correlation-id = %s] %s".format(CorrelationId.render(context.get(CorrelationId)), message)
    }
    val fs =
      if (config.debug) (appConfig ++ logger) >>> FS.local
      else {
        val container = appConfig >>> FS.azureBlobContainer
        (appConfig ++ container ++ logger) >>> FS.azure
      }

    val clientConfig = ZLayer.succeed(ClientConfig(config.url))
    val clientBackend: ZLayer[Any, Nothing, Has[OnlinerBackend]] =
      ZLayer.fromManaged(AsyncHttpClientZioBackend.managed()).orDie
    val client = (clientBackend ++ clientConfig) >>> OnlinerClient.live

    (client ++ fs) >>> DataTransport.live ++ logger
  }
}

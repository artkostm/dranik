package io.github.artkostm.dranik

import io.github.artkostm.dranik.ApplicationConfig.TransportConfig
import io.github.artkostm.dranik.client.{ClientConfig, OnlinerBackend, OnlinerClient, RateLimitingBackend}
import io.github.artkostm.dranik.fs.FS
import io.github.artkostm.dranik.transport.DataTransport
import io.github.resilience4j.ratelimiter.{RateLimiter, RateLimiterConfig}
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.logging.LogAnnotation.CorrelationId
import zio.logging._
import zio.logging.slf4j._
import zio.{ZLayer, _}

import java.time.Duration

object Main extends App {
  type ApplicationEnv = DataTransport with Logging with Has[ApplicationConfig]

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    dependencies(ApplicationConfig(args).toLayer.orDie).build.use { env =>
      log
        .locally(CorrelationId(env.get[ApplicationConfig].correlationId)) {
          transport
            .downloadApartments()
            .tapError(error => log.throwable("Program finished with an error: ", error))
        }
        .provide(env)
    }.exitCode

  def dependencies(
    configLayer: ZLayer[ZEnv, Nothing, Has[ApplicationConfig]]
  ): ZLayer[ZEnv, Nothing, ApplicationEnv] = {
    val logger = Slf4jLogger.make { (context, message) =>
      "[correlation-id = %s] %s".format(CorrelationId.render(context.get(CorrelationId)), message)
    }

    val fs: ZLayer[ZEnv, Nothing, FS] = (configLayer ++ logger) >>> FS.live

    val clientConfig: ZLayer[ZEnv, Nothing, Has[ClientConfig]] = configLayer.project(_.client)
    val clientBackend: ZLayer[ZEnv, Nothing, Has[OnlinerBackend]] =
      ZLayer.fromManaged(AsyncHttpClientZioBackend.managed()).project { delegate =>
        val rateLimiter = RateLimiter.of("OnlinerTransport", RateLimiterConfig.custom()
          .timeoutDuration(Duration.ofMillis(100))
          .limitRefreshPeriod(Duration.ofSeconds(1))
          .limitForPeriod(1)
          .build())
        RateLimitingBackend(rateLimiter, delegate)
      }.orDie
    val client: ZLayer[ZEnv, Nothing, OnlinerClient] = (clientBackend ++ clientConfig) >>> OnlinerClient.live

    val transportConfig: ZLayer[ZEnv, Nothing, Has[TransportConfig]] =
      configLayer.project(_.transport)
    val transport = ((client ++ fs ++ transportConfig) >>> DataTransport.live)
    transport ++ logger ++ configLayer
  }
}

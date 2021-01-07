package io.github.artkostm.dranik

import io.github.artkostm.dranik.ApplicationConfig.{Secret, TransportConfig}
import io.github.artkostm.dranik.client.ClientConfig
import zio.config._
import zio.config.magnolia.DeriveConfigDescriptor._
import zio.config.magnolia.describe
import zio.config.typesafe._
import zio.{ZEnv, ZIO}

import java.util.UUID
import scala.language.postfixOps

final case class ApplicationConfig(
  @describe("Client configuration") client: ClientConfig,
  @describe("Transport configuration") transport: TransportConfig,
  @describe("Set to true if you are going to run an application on a local machine") debug: Boolean =
    false,
  @describe("Azure Storage container name if debug=false") container: Option[String] = None,
  @describe("Azure Storage blob inside the container") blob: Option[String] = None,
  @describe("Azure Storage account name [KV secret name]") storageAccount: Option[Secret] = None,
  @describe("Azure Storage account key [KV secret name]") storageAccountKey: Option[Secret] = None,
  @describe("Local directory, required if debug=true") localDir: Option[String] = None,
  @describe("Correlation id for logging purposes") correlationId: Option[UUID] = Some(
    UUID.randomUUID()
  ),
)

object ApplicationConfig {

  def apply(args: List[String]): ZIO[ZEnv, Throwable, ApplicationConfig] =
    for {
      cli <- ZIO.succeed(
              ConfigSource
                .fromCommandLineArgs(args, keyDelimiter = Some('.'), valueDelimiter = Some(','))
            )
      props <- ConfigSource
                .fromSystemProperties(keyDelimiter = Some('_'), valueDelimiter = Some(','))
      env    <- ConfigSource.fromSystemEnv(keyDelimiter = Some('_'), valueDelimiter = Some(','))
      hocon  <- ZIO.fromEither(TypesafeConfigSource.unsafeDefaultLoader)
      source = cli <> props <> env <> hocon
      config <- ZIO
                 .fromEither(read(descriptor[ApplicationConfig].mapKey(toKebabCase) from source))
                 .mapError(e => new RuntimeException(e.prettyPrint()))
    } yield config

  final case class Secret(value: String) extends AnyVal {
    override def toString: String = "Secret(***)"
  }

  final case class TransportConfig(
    leftBoundLatitude: BigDecimal,
    leftBoundLongitude: BigDecimal,
    rightBoundLatitude: BigDecimal,
    rightBoundLongitude: BigDecimal,
    latitudeStep: BigDecimal,
    longitudeStep: BigDecimal
  )
}

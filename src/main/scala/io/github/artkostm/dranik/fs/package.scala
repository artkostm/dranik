package io.github.artkostm.dranik

import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.blob.CloudBlobContainer
import zio._
import zio.logging.{Logger, Logging}
import zio.stream.{Sink, Stream}

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer

package object fs {
  type FS = Has[FS.Service]

  object FS {

    def azureBlobContainer: ZLayer[Has[ApplicationConfig], Nothing, Has[CloudBlobContainer]] =
      ZLayer.fromServiceM[ApplicationConfig, Any, Nothing, CloudBlobContainer] { config =>
        blobContainer(config).orDie
      }

    private def blobContainer(config: ApplicationConfig): Task[CloudBlobContainer] =
      Task {
        val credentials = config.storageAccount
          .zip(config.storageAccountKey)
          .getOrElse(throw new RuntimeException("Cannot connect to ADLS"))
        val storageAccount = CloudStorageAccount.parse(
          s"DefaultEndpointsProtocol=https;AccountName=${credentials._1.value};AccountKey=${credentials._2.value}"
        )
        val blobClient = storageAccount.createCloudBlobClient()
        blobClient.getContainerReference(
          config.container
            .getOrElse(throw new RuntimeException("Please specify the Storage container name"))
        )
      }

    def azure: URLayer[Logging with Has[ApplicationConfig] with Has[CloudBlobContainer], FS] =
      ZLayer.fromServices[Logger[String], ApplicationConfig, CloudBlobContainer, FS.Service] {
        (logger, config, container) =>
          azureFs(logger, config, container)
      }

    def local: URLayer[Logging with Has[ApplicationConfig], FS] =
      ZLayer.fromServices[Logger[String], ApplicationConfig, FS.Service](
        (logger, config) => localFs(logger, config)
      )

    def live: URLayer[Logging with Has[ApplicationConfig], FS] =
      ZLayer.fromServicesM[Logger[String], ApplicationConfig, Any, Nothing, FS.Service] {
        (logger, config) =>
          if (config.debug) ZIO.succeed(localFs(logger, config))
          else
            blobContainer(config).orDie.map { container =>
              azureFs(logger, config, container)
            }
      }

    private def localFs(logger: Logger[String], config: ApplicationConfig): FS.Service =
      new Local(
        config.localDir
          .getOrElse(throw new RuntimeException("Please specify local root dir for debug")),
        logger
      )

    private def azureFs(logger: Logger[String],
                        config: ApplicationConfig,
                        container: CloudBlobContainer): FS.Service =
      new AzureBlob(
        container,
        config.blob.getOrElse(throw new RuntimeException("Please specify root blob")),
        logger
      )

    trait Service {

      def write(fileName: String, bytes: Array[Byte]): Task[String] =
        writeStream(fileName) { writer =>
          Task(writer.write(bytes))
        }

      def write(fileName: String, stream: Stream[Throwable, ByteBuffer]): Task[String] =
        writeStream(fileName) { writer =>
          stream.run(
            Sink.foldLeftM[Any, Throwable, ByteBuffer, OutputStream](writer) { (w, c) =>
              Task(w.write(c.array())).map(_ => w)
            }
          )
        }

      def writeStream(fileName: String)(resource: OutputStream => Task[Any]): Task[String]
      def read(fileName: String): Managed[Throwable, (Long, InputStream)]
    }
  }
}

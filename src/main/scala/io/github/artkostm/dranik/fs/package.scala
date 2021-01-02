package io.github.artkostm.dranik

import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.blob.CloudBlobContainer
import io.github.artkostm.dranik.Cli.TransportAppConfig
import zio.stream.{Sink, Stream}
import zio._

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer

package object fs {
  type FS = Has[FS.Service]

  object FS {

    def azureBlobContainer: ZLayer[Has[TransportAppConfig], Nothing, Has[CloudBlobContainer]] =
      ZLayer.fromServiceM[TransportAppConfig, Any, Nothing, CloudBlobContainer] { config =>
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
        }.orDie
      }

    def azure: URLayer[Has[TransportAppConfig] with Has[CloudBlobContainer], FS] =
      ZLayer.fromServices[TransportAppConfig, CloudBlobContainer, FS.Service] {
        (config, container) =>
          new AzureBlob(
            container,
            config.blob.getOrElse(throw new RuntimeException("Please specify root blob"))
          )
      }

    def local: URLayer[Has[TransportAppConfig], FS] =
      ZLayer.fromService[TransportAppConfig, FS.Service](
        config =>
          new Local(
            config.localDir
              .getOrElse(throw new RuntimeException("Please specify local root dir for debug"))
        )
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

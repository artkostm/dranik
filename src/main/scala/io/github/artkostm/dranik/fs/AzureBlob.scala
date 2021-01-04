package io.github.artkostm.dranik.fs

import com.microsoft.azure.storage.blob.{CloudBlobContainer, CloudBlockBlob}
import zio.blocking.{Blocking, blocking}
import zio.logging.Logger
import zio.{Managed, Task, UIO}

import java.io.{InputStream, OutputStream}

protected[fs] class AzureBlob(blockContainer: CloudBlobContainer, blobRoot: String, logger: Logger[String])
  extends FS.Service {

  override def writeStream(
    blobName: String
  )(blobResource: OutputStream => Task[Any]): Task[String] =
    for {
      _     <- logger.info(s"Saving data to the blob: $blobName...")
      bName = Seq(blobRoot, blobName).mkString("/")
      _ <- blocking(
            AzureBlob.getBlobWriter(blockContainer.getBlockBlobReference(bName)).use(blobResource)
          ).provideLayer(Blocking.live)
      _ <- logger.info(s"Saved blob: $bName")
    } yield bName

  override def read(fileName: String): Managed[Throwable, (Long, InputStream)] =
    for {
      _ <- logger.info(s"Streaming data from the blob $fileName...").toManaged_
      _ <- if (blockContainer.exists()) Managed.unit
          else
            Managed.fail(
              new RuntimeException(s"Container ${blockContainer.getName} doesn't exist...")
            )
      blob     = blockContainer.getBlockBlobReference(fileName)
      _        <- Managed.effectTotal(blob.downloadAttributes())
      fileSize <- Managed.effectTotal(blob.getProperties.getLength)
      stream   <- AzureBlob.getBlobReader(blob)
    } yield (fileSize, stream)
}

protected object AzureBlob {

  def getBlobWriter(blockBlob: CloudBlockBlob): Managed[Throwable, OutputStream] =
    Managed.make(Task(blockBlob.openOutputStream()))(w => Task(w.close()).ignore)

  def getBlobReader(blockBlob: CloudBlockBlob): Managed[Throwable, InputStream] =
    Managed.make(Task(blockBlob.openInputStream()))(r => UIO(r.close()))
}

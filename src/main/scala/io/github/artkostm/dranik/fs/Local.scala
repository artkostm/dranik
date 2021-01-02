package io.github.artkostm.dranik.fs

import io.github.artkostm.dranik.utils.logger

import java.io.{File, FileInputStream, FileOutputStream, InputStream, OutputStream}
import org.slf4j.LoggerFactory
import zio.blocking.{Blocking, blocking}
import zio.{Managed, Task, UIO}

protected[fs] class Local(rootDir: String) extends FS.Service {
  private implicit val LOGGER = LoggerFactory.getLogger(getClass)

  override def writeStream(fileName: String)(resource: OutputStream => Task[Any]): Task[String] =
    for {
      _    <- logger.info(s"Saving data to $fileName...")
      file = new File(rootDir, fileName)
      _    <- Task(file.getParentFile.mkdirs())
      _    <- blocking(Local.getFileWriter(file).use(resource)).provideLayer(Blocking.live)
      _    <- logger.info("Saved: " + fileName)
    } yield file.getAbsolutePath

  override def read(fileName: String): Managed[Throwable, (Long, InputStream)] =
    for {
      _     <- logger.info(s"Streaming data from the file $fileName...").toManaged_
      file = new File(rootDir, fileName)
      stream <- Local.getFileReader(file)
    } yield (stream.available(), stream)
}

protected object Local {

  def getFileWriter(file: File): Managed[Throwable, FileOutputStream] =
    Managed.make(Task(new FileOutputStream(file)))(w => UIO(w.close()))

  def getFileReader(file: File): Managed[Throwable, FileInputStream] =
    Managed.make(Task(new FileInputStream(file)))(r => UIO(r.close()))
}

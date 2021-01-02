package io.github.artkostm.dranik

import scopt.OParser
import zio.Task

object Cli {
  final def parse(args: List[String]): Task[TransportAppConfig] = {
    val builder = OParser.builder[TransportAppConfig]
    import builder._
    OParser.parse(
      OParser.sequence(
        head("Transport", "1.0"),
        cmd("load").children(
          opt[Unit]("debug").hidden().action((_, c) => c.copy(debug = true)),
          opt[String]("account")
            .abbr("a")
            .action((x, c) => c.copy(storageAccount = Some(Secret(x))))
            .text("Storage Account name"),
          opt[String]("key")
            .abbr("k")
            .action((x, c) => c.copy(storageAccountKey = Some(Secret(x))))
            .text("Storage Account key"),
          opt[String]("container")
            .abbr("c")
            .action((x, c) => c.copy(container = Some(x)))
            .text("Azure Blob container name"),
          opt[String]("blob")
            .abbr("b")
            .action((x, c) => c.copy(blob = Some(x)))
            .text("Azure Blob name"),
          opt[String]("local-dir")
            .abbr("l")
            .action((x, c) => c.copy(localDir = Some(x)))
            .text("Root directory name (debug=true)"),
          opt[String]("url")
            .abbr("u")
            .hidden()
            .action((x, c) => c.copy(url = x))
            .text("Onliner REST API URL"),
          checkConfig(
            c =>
              if (c.debug && c.localDir.isDefined) success
              else failure("debug mode only works with local FS")
          )
        )
      ),
      args,
      TransportAppConfig()
    ) match {
      case Some(config) => Task.succeed(config)
      case _            => Task.fail(new RuntimeException("Cannot parse cli args"))
    }
  }

  final case class Secret(value: String) {
    override def toString: String = "Secret(***)"
  }

  final case class TransportAppConfig(
    url: String = "https://pk.api.onliner.by",
    debug: Boolean = false,
    container: Option[String] = None,
    blob: Option[String] = None,
    storageAccount: Option[Secret] = None,
    storageAccountKey: Option[Secret] = None,
    localDir: Option[String] = None,
  )
}

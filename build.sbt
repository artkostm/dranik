name := "dranik"

version := "0.1"

scalaVersion := "2.13.4"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "1.0.3",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % "3.0.0-RC13",
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"   % "2.6.2",
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.6.2" % Provided,
  "com.microsoft.azure" % "azure-storage" % "8.3.0",
  "com.github.scopt" %% "scopt" % "4.0.0",
  "org.slf4j" % "slf4j-api" % "1.7.30",
  "org.slf4j" % "slf4j-log4j12" % "1.7.30"
)
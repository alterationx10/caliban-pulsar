ThisBuild / scalaVersion := "2.13.6"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.alterationx10"
ThisBuild / organizationName := "alterationx10"

val calibanVersion = "1.2.1"

val deps = Seq(
  "com.github.ghostdogpr" %% "caliban" % calibanVersion,
  "com.github.ghostdogpr" %% "caliban-zio-http" % calibanVersion,
  "org.apache.pulsar" % "pulsar-client" % "2.8.1",
  "dev.zio" %% "zio-json" % "0.2.0-M1",
  "redis.clients" % "jedis" % "3.7.0"
)

lazy val root = (project in file("."))
  .settings(
    name := "caliban-pulsar",
    libraryDependencies ++= deps
  )

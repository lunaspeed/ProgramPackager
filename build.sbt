name := "ProgramPackager"

version := "1.0"

jfxSettings

JFX.mainClass := Some("com.bi.programlist.ui.javafx.MainFx")

JFX.title := "Program Packager"

// `all` attempts to create all the package types build-able on the current platform.
JFX.nativeBundles := "all"

scalaVersion := "2.11.3"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/groups/public/"
//resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

resolvers += "spray" at "http://repo.spray.io/"

val log4jVersion = "2.1"

libraryDependencies ++= Seq(
    "org.apache.poi" % "poi-ooxml" % "3.11-beta3",
    "com.jsuereth" %% "scala-arm" % "1.4",
    "io.spray" %%  "spray-json" % "1.2.6",
    "org.apache.commons" % "commons-lang3" %"3.1",
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
    "org.squeryl" %% "squeryl" % "0.9.5-7",
    "org.xerial" % "sqlite-jdbc" % "3.7.2",
    "org.slf4j" % "slf4j-ext" % "1.7.7",
    "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
    "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
    "org.apache.logging.log4j" % "log4j-jcl" % log4jVersion,
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion)

//libraryDependencies += "com.jsuereth" % "scala-arm_2.10" % "1.3"
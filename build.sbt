import sbt.Keys._
import sbt._
import sbtassembly.AssemblyPlugin.autoImport._


// Project name (artifact name in Maven)
name := "mix"

// organization name (e.g., the package name of the project)
organization := "com.vesperin"

version := "0.2"

// project description
description := "Utilities (base mix) needed by vesperin's features"

// Enables publishing to maven repo
publishMavenStyle := true

// Do not append Scala versions to the generated artifacts
crossPaths := false

scalaVersion := "2.13.0"

// This forbids including Scala related libraries into the dependency
autoScalaLibrary := false

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case PathList("about_files", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

assemblyJarName in assembly:= "mix-0.1-SNAPSHOT.jar"
//target in assembly := file(".")

test in assembly := {}

// library dependencies. (organization name) % (project name) % (version)
libraryDependencies ++= Seq(
   "org.eclipse.jdt" % "org.eclipse.jdt.core" % "3.10.0",
    "com.novocode" % "junit-interface" % "0.11" % "test",
    "junit" % "junit" % "4.12"
)

assemblyExcludedJars in assembly := {
  val cp = (fullClasspath in assembly).value

  val excludes = Set(
    "junit-4.12.jar",
    "hamcrest-core-1.3.jar",
    "junit-interface-0.11.jar",
    "org.eclipse.core.commands-3.6.0.jar",
    "org.eclipse.core.contenttype-3.4.100.jar",
    "org.eclipse.core.expressions-3.4.300.jar",
    "org.eclipse.core.filesystem-1.3.100.jar",
    "org.eclipse.core.jobs-3.5.100.jar",
    "org.eclipse.core.resources-3.7.100.jar",
    "org.eclipse.core.resources-runtime-3.7.0.jar",
    "org.eclipse.equinox.app-1.3.100.jar",
    "org.eclipse.equinox.common-3.6.0.jar",
    "org.eclipse.equinox.preferences-3.4.1.jar",
    "org.eclipse.equinox.registry-3.5.101.jar",
    "org.eclipse.core.runtime-3.7.0.jar",
    "org.eclipse.jdt.core-3.10.0.jar",
    "org.eclipse.osgi-3.7.1.jar",
    "org.eclipse.text-3.5.101.jar"
  )

  cp filter {jar => excludes(jar.data.getName) }
}

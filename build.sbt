import sbt._
import Keys._
import sbtassembly.AssemblyPlugin.autoImport._


// Project name (artifact name in Maven)
name := "mix"

// organization name (e.g., the package name of the project)
organization := "com.vesperin"

version := "0.1"

// project description
description := "Utilities (base mix) needed by vesperin's features"

// Enables publishing to maven repo
publishMavenStyle := true

// Do not append Scala versions to the generated artifacts
crossPaths := false

scalaVersion := "2.11.7"

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
    "com.google.guava" % "guava" % "19.0",
    "com.novocode" % "junit-interface" % "0.11" % "test",
    "junit" % "junit" % "4.12",
    "org.mockito" % "mockito-core" % "1.10.19"
)

assemblyExcludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  val excludes = Set(
    "mockito-core-1.10.19.jar",
    "junit-4.12.jar",
    "junit-interface-0.11.jar"
  )
  cp filter { jar => excludes(jar.data.getName) }
}

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.3",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Hello",
    libraryDependencies += "info.bliki.wiki" % "bliki-core" % "3.1.0",
    libraryDependencies += "io.suzaku" %% "boopickle" % "1.2.6",
    libraryDependencies += "com.softwaremill.sttp" %% "core" % "1.1.5"
  )

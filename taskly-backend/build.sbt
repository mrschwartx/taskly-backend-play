lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(
    name := "taskly-backend",
    organization := "com.taskly.backend",
    version := "2.0.0-SNAPSHOT",
    scalaVersion := "2.13.18",
    PlayKeys.playDefaultPort := sys.env.get("APP_PORT").flatMap(_.toIntOption).getOrElse(8080),
    PlayKeys.playDefaultAddress := sys.env.getOrElse("APP_HOST", "0.0.0.0"),
    libraryDependencies ++= Seq(
      guice,
      javaJdbc,
      filters,
      "org.postgresql" % "postgresql" % "42.7.7",
      "org.flywaydb" % "flyway-core" % "11.20.2",
      "org.flywaydb" % "flyway-database-postgresql" % "11.20.2",
      "org.bouncycastle" % "bcprov-jdk18on" % "1.78.1",
      "io.jsonwebtoken" % "jjwt-api" % "0.12.6",
      "io.jsonwebtoken" % "jjwt-impl" % "0.12.6" % Runtime,
      "io.jsonwebtoken" % "jjwt-jackson" % "0.12.6" % Runtime,
      "org.assertj" % "assertj-core" % "3.27.7" % Test,
      "com.jayway.jsonpath" % "json-path" % "2.9.0" % Test
    ),
    javacOptions ++= Seq("--release", "21", "-encoding", "UTF-8", "-parameters"),
    PlayKeys.externalizeResources := false,
    Test / fork := true,
    Test / testOptions := Seq(Tests.Argument(TestFrameworks.JUnit, "-a", "-v"))
  )

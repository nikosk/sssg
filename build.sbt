name := "scala-static-site-generator"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += Resolver.sonatypeRepo("public")

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.0",
  "org.pegdown" % "pegdown" % "1.5.0",
  "de.neuland-bfi" % "jade4j" % "0.4.3",
  "com.github.scopt" %% "scopt" % "3.3.0",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
)

fork := true

testGrouping in Test := {
  val original: Seq[Tests.Group] = (testGrouping in Test).value

  original.map { group =>
    val forkOptions = ForkOptions(
      bootJars = Nil,
      javaHome = javaHome.value,
      connectInput = connectInput.value,
      outputStrategy = outputStrategy.value,
      runJVMOptions = javaOptions.value,
      workingDirectory = Some(new File("src/test/resources")),
      envVars = envVars.value
    )

    group.copy(runPolicy = Tests.SubProcess(forkOptions))
  }
}

assemblyJarName in assembly := "sssg.jar"
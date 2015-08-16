lazy val gatherJavaScripts = taskKey[Unit]("get the output of building js")

lazy val `data-model` = crossProject
  .settings()
  .jvmSettings()
  .jsSettings()

lazy val `data-model-jvm` = `data-model`.jvm
lazy val `data-model-js` = `data-model`.js

lazy val `ui-client` = project
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(`data-model-js`)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.japgolly.scalajs-react" %%% "core" % "0.9.2"
    ),
    jsDependencies ++= Seq(
      "org.webjars" % "react" % "0.12.2" / "react-with-addons.js" commonJSName "React"
    )
  )

lazy val `server-common` = project
  .settings(
    libraryDependencies ++= Seq(
      Boilerplate.Modules.akka("http-experimental", Boilerplate.Modules.akkaStreamsVersion),
      Boilerplate.Modules.slf4j_api
    )
  )

lazy val `backend-server` = project
  .dependsOn(`data-model-jvm`)

lazy val `ui-server` = project
  .dependsOn(`server-common`)
  .aggregate(`server-common`)
  .settings(
    libraryDependencies ++= Seq(
      Boilerplate.Modules.akka("http-experimental", Boilerplate.Modules.akkaStreamsVersion),
      Boilerplate.Modules.slf4j_api
    ),
    gatherJavaScripts := {
      (org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport.fullOptJS in(`ui-client`, Compile)).value
      val outputDir = (classDirectory in Compile).value / "js"
      (Seq.empty[File] /: List("*.js", "*.map")) { (files, pattern) ⇒
        files ++ ((crossTarget in `ui-client`).value ** pattern).get
      } foreach { source ⇒
        streams.value.log.info(s"$source ⇒ ${outputDir / source.name}")
        IO.copyFile(source, outputDir / source.name)
      }
    },
    compile in Compile := {
      gatherJavaScripts.value
      (compile in Compile).value
    }
  )

lazy val `todo` = project.in(file("."))
  .dependsOn(`backend-server`, `ui-server`, `server-common`)
  .aggregate(`backend-server`, `ui-server`, `server-common`)
  .settings(Revolver.settings)
  .settings(
    libraryDependencies ++= Boilerplate.Modules.logging,
    libraryDependencies ++= Seq(
      Boilerplate.Modules.akka("actor"),
      Boilerplate.Modules.akka("http-experimental", Boilerplate.Modules.akkaStreamsVersion)
    )
  )
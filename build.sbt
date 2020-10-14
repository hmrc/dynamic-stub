import PlayCrossCompilation._
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.SbtArtifactory

val microservice = Project("dynamic-stub", file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    majorVersion                     := 0,
    libraryDependencies              ++= AppDependencies(),
    makePublicallyAvailableOnBintray := true,

  )
  .settings(scalaVersion := "2.12.12")
  .settings(crossScalaVersions := Seq("2.11.12", "2.12.12"))
  .settings(scalacOptions ++= Seq("-Xfatal-warnings", "-feature", "-Xlint:-missing-interpolator,_"))

   .configs(IntegrationTest)
  .settings(playCrossCompilationSettings)
  .settings(integrationTestSettings(): _*)
  .settings(SilencerSettings())
  .settings(ScoverageSettings())
  .settings(ScalariformSettings())

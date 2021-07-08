import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings

val microservice = Project("dynamic-stub", file("."))
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    majorVersion                     := 0,
    libraryDependencies              ++= AppDependencies(),
    isPublicArtefact                 := true,

  )
  .settings(scalaVersion := "2.12.12")
  .settings(scalacOptions ++= Seq("-Xfatal-warnings", "-feature", "-Xlint:-missing-interpolator,_"))
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(SilencerSettings())
  .settings(ScoverageSettings())
  .settings(ScalariformSettings())

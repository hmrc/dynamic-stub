/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt.Keys._
import sbt._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import uk.gov.hmrc.SbtArtifactory.autoImport.makePublicallyAvailableOnBintray

object HmrcBuild extends Build {

  import uk.gov.hmrc._

  val appName = "dynamic-stub"

  lazy val microservice = Project(appName, file("."))
    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
    .settings(
      scalaVersion := "2.11.7",
      libraryDependencies ++= AppDependencies(),
      crossScalaVersions := Seq("2.11.7"),
      resolvers := Seq(
        Resolver.url("HMRC Sbt Plugin Releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(Resolver.ivyStylePatterns),
        "HMRC Releases" at "https://dl.bintray.com/hmrc/releases"
      ),
      makePublicallyAvailableOnBintray := true,
      majorVersion := 0

  )
    .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
}

private object AppDependencies {

  import play.core.PlayVersion

  val compile = Seq(
    "com.typesafe.play" %% "play" % PlayVersion.current % "provided",
    "uk.gov.hmrc" %% "simple-reactivemongo" % "7.20.0-play-25" % "provided",
    "uk.gov.hmrc" %% "time" % "3.6.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalatest" %% "scalatest" % "2.2.4" % scope,
        "org.scalactic" %% "scalactic" % "2.2.2" % scope,
        "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-25" % scope,
        "org.pegdown" % "pegdown" % "1.5.0" % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}

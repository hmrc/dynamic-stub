import sbt.ModuleID
import sbt._

object AppDependencies {

  import play.core.PlayVersion

  val compile: Seq[ModuleID] = Seq(
    "com.typesafe.play" %% "play"                 % PlayVersion.current % "provided",
    "uk.gov.hmrc"       %% "simple-reactivemongo" % "7.30.0-play-26"    % "provided",
    "uk.gov.hmrc"       %% "time"                 % "3.9.0"
  )

  val test: Seq[ModuleID] = Seq(
        "com.typesafe.play"      %% "play-test"          % PlayVersion.current % "test",
        "uk.gov.hmrc"            %% "hmrctest"           % "3.9.0-play-26"     % "test",
        "org.pegdown"             %  "pegdown"           % "1.6.0"             % "test",
        "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2"             % "test",
        "org.mockito"            %% "mockito-scala"      % "1.15.0"            % "test",
        "uk.gov.hmrc"            %% "reactivemongo-test" % "4.21.0-play-26"    % "test"
  )

  def apply(): Seq[ModuleID] = compile ++ test
}

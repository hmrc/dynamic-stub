resolvers += Resolver.url("hmrc-sbt-plugin-releases",
  url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"

addSbtPlugin("com.typesafe.play"  % "sbt-plugin"          % "2.7.6")

addSbtPlugin("uk.gov.hmrc"        % "sbt-auto-build"      % "2.9.0")
        
addSbtPlugin("uk.gov.hmrc"        % "sbt-git-versioning"  % "2.1.0")
        
addSbtPlugin("uk.gov.hmrc"        % "sbt-artifactory"     % "1.3.0")

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.3")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")

addSbtPlugin("uk.gov.hmrc" % "sbt-settings" % "4.5.0")

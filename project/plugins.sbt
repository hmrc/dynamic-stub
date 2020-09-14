resolvers += Resolver.url("hmrc-sbt-plugin-releases",
  url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play"  % "sbt-plugin"          % "2.5.19")

addSbtPlugin("uk.gov.hmrc"        % "sbt-auto-build"      % "2.6.0")
        
addSbtPlugin("uk.gov.hmrc"        % "sbt-git-versioning"  % "2.1.0")
        
addSbtPlugin("uk.gov.hmrc"        % "sbt-artifactory"     % "1.2.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.3")

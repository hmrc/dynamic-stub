resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"

addSbtPlugin("com.typesafe.play"  % "sbt-plugin"          % "2.7.6")

addSbtPlugin("uk.gov.hmrc"        % "sbt-auto-build"      % "3.2.0")
        
addSbtPlugin("uk.gov.hmrc"        % "sbt-artifactory"     % "2.0.0")

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.3")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")

addSbtPlugin("uk.gov.hmrc" % "sbt-settings" % "4.9.0")

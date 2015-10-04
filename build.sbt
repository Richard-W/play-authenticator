name := "play-authenticator"

organization := "xyz.wiedenhoeft"

version := "0.3.0"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/richard-w/play-authenticator"),
    "scm:git:https://github.com/richard-w/play-authenticator",
    Some("scm:git:https://github.com/richard-w/play-authenticator")
  )
)

scalaVersion := "2.11.6"

scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked")

useGpg := true

usePgpKeyHex("CB8F8B69")

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.typesafeRepo("releases"),
  Resolver.jcenterRepo
)

libraryDependencies ++= {
  val playVersion = "2.4.1"
  Seq(
    "com.typesafe.play"       %% "play"                 % playVersion     % "provided",
    "com.typesafe.play"       %% "play-ws"              % playVersion     % "compile",
    "xyz.wiedenhoeft"         %% "play-reactivemongo"   % "0.1.0"         % "compile",
    "xyz.wiedenhoeft"         %% "scalacrypt"           % "0.4.0"         % "compile",
    "org.scalatest"           %% "scalatest"            % "2.2.4"         % "test",
    "com.github.simplyscala"  %% "scalatest-embedmongo" % "0.2.2"         % "test"
  )
}

name := "pg-wordvecs"
organization := "net.janvsmachine"

scalaVersion := "2.12.4"
scalacOptions += "-Ypartial-unification"

lazy val doobieVersion = "0.5.1"

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core"     % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "org.tpolecat" %% "doobie-specs2"   % doobieVersion
)

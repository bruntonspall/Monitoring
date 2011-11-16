name := "GU-Monitoring"

version := "1.0"

organization := "com.gu"

scalaVersion := "2.9.1"

libraryDependencies ++= {
val liftVersion = "2.4-M4"
Seq(
  "net.liftweb" %% "lift-json" % liftVersion,
  "net.liftweb" %% "lift-util" % liftVersion,
  "net.liftweb" %% "lift-webkit" % liftVersion,
  "net.liftweb" %% "lift-widgets" % liftVersion,
  "ch.qos.logback" % "logback-classic" % "0.9.26",
  "com.weiglewilczek.slf4s" %% "slf4s" % "1.0.7",
  "com.google.appengine" % "appengine-api-1.0-sdk" % "1.6.0",
  "org.eclipse.jetty" % "jetty-webapp" % "7.5.1.v20110908" % "jetty"
)
}

seq(appengineSettings: _*)


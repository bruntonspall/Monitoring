import sbt._

class LiftProject(info: ProjectInfo)  extends DefaultWebProject(info) {
  val liftVersion = "2.2"

  // uncomment the following if you want to use the snapshot repo
  val scalatoolsSnapshot = ScalaToolsSnapshots

  // If you're using JRebel for Lift development, uncomment
  // this line
  // override def scanDirectories = Nil
  val slf4j = "org.slf4j" % "slf4j-api" % "1.6.1" withSources()
  val slf4jOverJul= "org.slf4j" % "slf4j-jdk14" % "1.6.1" withSources()

  val liftCommon = "net.liftweb" %% "lift-common" % liftVersion withSources()
  val liftJson = "net.liftweb" %% "lift-json" % liftVersion withSources()
  val liftUtil = "net.liftweb" %% "lift-util" % liftVersion withSources()
  val liftWebkit = "net.liftweb" %% "lift-webkit" % liftVersion withSources()
  val liftWidgets = "net.liftweb" %% "lift-widgets" % liftVersion withSources()

  val jetty7 = "org.eclipse.jetty" % "jetty-webapp" % "7.0.2.v20100331" % "test"
  val jetty7jsp = "org.eclipse.jetty" % "jetty-jsp-2.1" % "7.1.1.v20100517" % "test"

  val appengine = "com.google.appengine" % "appengine-api-1.0-sdk" % "1.3.8"
  val appengineLabs = "com.google.appengine" % "appengine-api-labs" % "1.3.8"

  lazy val updateAppengine = task {FileUtilities.touch(sourcePath / "main" /"webapp"/"WEB-INF"/"appengine-web.xml", log) } dependsOn(compile) describedAs("Touches appengine-web.xml to force reload")
  override def prepareWebappAction = super.prepareWebappAction dependsOn(updateAppengine)
}

import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
  val appenginePlugin = "net.stbbs.yasushi" % "sbt-appengine-plugin" % "2.1"  
  val sbtIdeaRepo = "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"
  val sbtIdea = "com.github.mpeltonen" % "sbt-idea-plugin" % "0.1-SNAPSHOT"
}

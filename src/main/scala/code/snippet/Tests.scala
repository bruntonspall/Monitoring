package code.snippet
import _root_.net.liftweb.util._
import Helpers._
import scala.collection.JavaConversions._
import com.google.appengine.api.datastore._
import com.google.appengine.api.datastore.Query.FilterOperator
import net.liftweb.widgets.flot._
import net.liftweb.http.js.JsCmds._
import com.google.appengine.api.datastore.FetchOptions.Builder.withLimit

class Test(e: Entity) {

  def requestId = e.getProperty("requestId").toString
//  def requestId_=(s: String) = e.setProperty("requestId", s)
  def xmlUrl = e.getProperty("xmlUrl").toString
  def summaryCSV = e.getProperty("summaryCSV").toString
  def detailsCSV = e.getProperty("detailsCSV").toString
  def rawXml = e.getProperty("rawXml").asInstanceOf[Text].getValue
  def ready = e.getProperty("ready") match { case "0" => false; case "1" => true}
  def runs = DatastoreServiceFactory.getDatastoreService.prepare(
    new Query("Run").addFilter("requestId", FilterOperator.EQUAL, requestId).addSort("rawDate")
  ).asIterable.map(new Run(_))
}

class Run(e: Entity) {
  def requestId = e.getProperty("requestId").toString
  def rawXml = e.getProperty("rawXml").asInstanceOf[Text].getValue
  def runId = e.getProperty("runId").toString
  def url = e.getProperty("url").toString
  def loadTime = e.getProperty("loadTime").toString
  def requests = e.getProperty("requests").toString
  def render = e.getProperty("render").toString
  def fullyLoaded = e.getProperty("fullyLoaded").toString
  def docTime = e.getProperty("docTime").toString
  def rawDate = Option(e.getProperty("rawDate")).getOrElse(1290695200).toString
  def date = e.getProperty("date").toString
}

class Tests {
  def allTests:List[Test] = {
    val query = new Query("Test")
    query.addSort("requestId")

    val datastore = DatastoreServiceFactory.getDatastoreService
    datastore.prepare(query).asIterator.toList.map(new Test(_))
  }

  def unparsedTests:List[Test] = {
    val query = new Query("Test")
    query.addFilter("ready", Query.FilterOperator.EQUAL, "0")
    query.addSort("requestId")
    val datastore = DatastoreServiceFactory.getDatastoreService
    datastore.prepare(query).asIterator(withLimit(5)).toList.map(new Test(_))
  }

  def failedTests:List[Test] = {
    allTests.filter(_.runs.size == 0).take(5)
  }

  def fully_loaded_data(series:String) = new FlotSerie() {
    override val data = allTests.flatMap(_.runs.filter(_.runId.endsWith(series)).map(run => (run.rawDate.toDouble, run.fullyLoaded.toDouble)))
  }

  def dom_ready_data(series:String) = new FlotSerie() {
    override val data = allTests.flatMap(_.runs.filter(_.runId.endsWith(series)).map(run => (run.rawDate.toDouble, run.render.toDouble)))
  }

  def graph = {
    ".graph_area" #> Flot.render("none", List(fully_loaded_data("A"), fully_loaded_data("B"), dom_ready_data("A"), dom_ready_data("B")), new FlotOptions {}, Noop)
  }

  def all= {
    ".request" #> allTests.map(test =>
      ".requestId *" #> test.requestId &
      ".run" #> (test.runs.map(run =>
        ".domrender *" #> run.render &
        ".loadtime *" #> run.loadTime &
        ".requests *" #> run.requests &
        ".fullyloaded *" #> run.fullyLoaded &
        ".runid *" #> run.runId
       ))
     )
  }
}
package code.snippet
import _root_.net.liftweb.util._
import Helpers._
import scala.collection.JavaConversions._
import com.google.appengine.api.datastore._
import com.google.appengine.api.datastore.Query.FilterOperator


class Test(e: Entity) {

  def requestId = e.getProperty("requestId").toString
//  def requestId_=(s: String) = e.setProperty("requestId", s)
  def xmlUrl = e.getProperty("xmlUrl").toString
  def summaryCSV = e.getProperty("summaryCSV").toString
  def detailsCSV = e.getProperty("detailsCSV").toString
  def rawXml = e.getProperty("rawXml").asInstanceOf[Text].getValue
  def ready = e.getProperty("ready") match { case "0" => false; case "1" => true}
  def runs = DatastoreServiceFactory.getDatastoreService.prepare(
    new Query("Run").addFilter("requestId", FilterOperator.EQUAL, requestId).addSort("runId")
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
  def rawDate = e.getProperty("rawDate").toString
  def date = e.getProperty("date").toString
}

class Tests {
  private def allTests:List[Test] = {
    val query = new Query("Test")
    query.addSort("requestId")

    val datastore = DatastoreServiceFactory.getDatastoreService
    datastore.prepare(query).asIterator.toList.map(new Test(_))

  }

  def all= {
    "li" #> allTests.map(test =>
      ".requestId *" #> test.requestId &
      ".arun" #> (test.runs.map(run =>
        ".domrender *" #> run.render &
        ".loadtime *" #> run.loadTime &
        ".runid *" #> run.runId 
       ))
     )
  }
}
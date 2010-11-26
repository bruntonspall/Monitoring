package code.snippet
import _root_.net.liftweb.util._
import Helpers._
import scala.collection.JavaConversions._
import com.google.appengine.api.datastore._


class Test(e: Entity) {

  def requestId = e.getProperty("requestId").toString
//  def requestId_=(s: String) = e.setProperty("requestId", s)
  def xmlUrl = e.getProperty("xmlUrl").toString
  def summaryCSV = e.getProperty("summaryCSV").toString
  def detailsCSV = e.getProperty("detailsCSV").toString
  def rawXml = e.getProperty("rawXml").asInstanceOf[Text].getValue
  def ready = e.getProperty("ready") match { case "0" => false; case "1" => true}
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
      "a [href]" #> test.requestId &
      "a *" #> test.rawXml
     )
  }
}
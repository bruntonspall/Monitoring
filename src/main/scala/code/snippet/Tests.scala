package code.snippet
import _root_.scala.xml.{NodeSeq, Text}
import _root_.net.liftweb.util._
import Helpers._
import com.google.appengine.api.datastore.FetchOptions.Builder.withDefaults
import scala.collection.JavaConversions._
import com.google.appengine.api.datastore.{Entity, FetchOptions, Query, DatastoreServiceFactory}


class Tests {
  private def allTests:List[Entity] = {
    var query = new Query("Test")
    query.addSort("requestId")

    val datastore = DatastoreServiceFactory.getDatastoreService
    datastore.prepare(query).asIterator.toList

  }
  def all= {
    "li" #> allTests.map(test =>
      "a [href]" #> test.getProperty("requestId") &
      "a *" #> test.getProperty("requestId")
     )
    //"li" #> allTests.map(test => <a href={"/"+test.getProperty("requestId")}>{test.getProperty("requestId")}</a>)
  }
}
package code {
package snippet {

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
  def all(xhtml: NodeSeq):NodeSeq = {
    allTests flatMap( test => <li>
      <a href={"/"+test.getProperty("requestId")} >
              {test.getProperty("requestId")}
      </a></li>)
  }

/*    {
    allTests flatMap( test =>
            bind ("f", xhtml,
              "requestId" --> test.getProperty("requestId"),
              "ready" --> test.getProperty("ready"))
    )
  } */
}

}
}

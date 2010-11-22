package bootstrap.liftweb

import net.liftweb._
import common.{Logger, Full, Box}
import net.liftweb.util.Helpers._
import http._
import java.net.URL
import com.google.appengine.api.urlfetch.{URLFetchServiceFactory, URLFetchService}
import scala.xml
import util.{TimeHelpers, HttpHelpers}
import xml.{Node, XML}
import java.util.Date
import org.joda.time.DateTime
import com.google.appengine.api.labs.taskqueue.QueueFactory
import com.google.appengine.api.labs.taskqueue.TaskOptions.Builder._
import com.google.appengine.api.labs.taskqueue.TaskOptions.Method.GET
import com.google.appengine.api.datastore._
import java.io.InputStreamReader

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {

    // where to search snippet
    LiftRules.addToPackages("code")
    //LiftRules.statelessTest.append({ case _ => true })
    LiftRules.dispatch.append {
      case Req("poll" :: Nil, _, GetRequest) => () => doPoll
      case Req("callback" :: Nil, _, GetRequest) => () => doCallback      
      case Req("fetch" :: Nil, _, GetRequest) => () => doFetch      
    }
    LiftRules.liftRequest.append {
      case Req("_ah" :: _, _, _) => false
    }
    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

  }

  def doFetch = {
    val requestId = S.param("requestId").open_!
    val runId = S.param("runId").open_!
    val filetype = S.param("type").open_!
    val testUrl = S.param("url").open_!
    val datastore = DatastoreServiceFactory.getDatastoreService
    val url = new URL(testUrl)
    val response = URLFetchServiceFactory.getURLFetchService.fetch(url)

    val entity = new Entity("RunSupportFiles", runId+filetype)
    entity.setProperty("runId", runId)
    entity.setProperty("type", filetype)
    entity.setProperty("contents", new Blob(response.getContent))
    datastore.put(entity)
    Full(InMemoryResponse(response.getContent, List(), List(), 200))
  }


  def handleRun(datastore:DatastoreService, run:Node, requestId:String) = {
    val results = run \ "firstView" \ "results"
    val runId = run \ "id" text
    val entity = new Entity("Run", runId)
    entity.setProperty("test", requestId)
    entity.setProperty("url", results \ "URL" text)
    entity.setProperty("loadTime", results \ "loadTime" text)
    entity.setProperty("requests", results \ "requests" text)
    entity.setProperty("render", results \ "render" text)
    entity.setProperty("fullyLoaded", results \ "fullyLoaded" text)
    entity.setProperty("docTime", results \ "docTime" text)
    entity.setProperty("date", new DateTime( (results \ "date").text.toLong).toDate)
    entity.setProperty("rawXml", new Text(results toString))
    datastore.put(entity)
    val rawDataFiles = run \ "firstView" \ "rawData"
    val headersFile = rawDataFiles \ "headers" text
    val pageDataFile = rawDataFiles \ "pageData" text
    val requestsFile = rawDataFiles \ "requestsData" text
    val utilizationFile = rawDataFiles \ "utilization" text
    val queue = QueueFactory.getDefaultQueue();
    queue.add(url("/fetch").param("requestId", requestId).param("runId", runId).param("type","headers").param("url", headersFile).method(GET))
    queue.add(url("/fetch").param("requestId", requestId).param("runId", runId).param("type","pagedata").param("url", pageDataFile).method(GET))
    queue.add(url("/fetch").param("requestId", requestId).param("runId", runId).param("type","requests").param("url", requestsFile).method(GET))
    queue.add(url("/fetch").param("requestId", requestId).param("runId", runId).param("type","utilization").param("url", utilizationFile).method(GET))
  }

  def doCallback = {
    val requestId = S.param("id").open_!
    val datastore = DatastoreServiceFactory.getDatastoreService
    val entity = datastore.get(KeyFactory.createKey("Test", requestId))
    entity.setProperty("ready", "1")
    val xmlDetails = XML.load(new URL(entity.getProperty("xmlUrl").asInstanceOf[String]))
    xmlDetails \\ "run" foreach (run => handleRun(datastore, run, requestId))
    Full(XmlResponse(xmlDetails))
  }

  def doPoll = {
    val params=List(("url", "http://www.guardian.co.uk"), ("private", "1"), ("f", "xml"), ("runs", "0"), ("callback", "http://gu-monitoring.appspot.com/callback") )
    val testUrl = "http://localhost:8081/runtest.xml?"+paramsToUrlParams(params)
//    val testUrl = "http://www.webpagetest.org/runtest.xml?"+paramsToUrlParams(params)
    val response = XML.load(new URL(testUrl))
    val datastore = DatastoreServiceFactory.getDatastoreService
    val requestId = response \\ "testId" text
    val entity = tryo {
      datastore.get(KeyFactory.createKey("Test", requestId))
    } openOr(new Entity("Test", requestId))
    entity.setProperty("requestId", requestId)
    entity.setProperty("xmlUrl", response \\ "xmlUrl" text)
    entity.setProperty("summaryCSV", response \\ "summaryCSV" text)
    entity.setProperty("detailsCSV", response \\ "detailsCSV" text)
    entity.setProperty("rawXml", response toString)
    entity.setProperty("ready", "0")
    datastore.put(entity)
    Full(XmlResponse(response))
  }
}

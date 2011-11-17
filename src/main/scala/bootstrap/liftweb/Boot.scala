package bootstrap.liftweb

import net.liftweb._
import common.Full
import net.liftweb.util.Helpers._
import http._
import java.net.URL
import com.google.appengine.api.urlfetch.URLFetchServiceFactory
import scala.xml
import org.joda.time.DateTime
import com.google.appengine.api.taskqueue.QueueFactory
import com.google.appengine.api.taskqueue.TaskOptions.Builder._
import com.google.appengine.api.taskqueue.TaskOptions.Method.GET
import com.google.appengine.api.datastore._
import net.liftweb.common.Logger
import xml.{NodeSeq, Node, XML}
import net.liftweb.widgets.flot._
import net.liftweb.util.Props
import code.snippet.Tests

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
      case Req("debug" :: "create-test" :: Nil, _, GetRequest) => () => doDebugCreate
      case Req("debug" :: "refetch" :: Nil, _, GetRequest) => () => doDebugRefetch
      case Req("debug" :: "reverify" :: Nil, _, GetRequest) => () => doDebugReverify
    }
    LiftRules.liftRequest.append {
      case Req("_ah" :: _, _, _) => false
    }
    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))
    Flot.init
    Logger("Boot").info("API Key is "+Props.get("apikey"))
  }

  def doFetch = {
    val requestId = S.param("requestId").open_!
    val runId = S.param("runId").open_!
    val filetype = S.param("type").open_!
    val testUrl = S.param("url").open_!
    val datastore = DatastoreServiceFactory.getDatastoreService
    val url = new URL(testUrl+"&k="+Props.get("apikey"))
    val response = URLFetchServiceFactory.getURLFetchService.fetch(url)

    val entity = new Entity("RunSupportFiles", requestId+":"+runId+":"+filetype)
    entity.setProperty("requestId", requestId)
    entity.setProperty("runId", runId)
    entity.setProperty("type", filetype)
    entity.setProperty("contents", new Blob(response.getContent))
    datastore.put(entity)
    Full(InMemoryResponse(response.getContent, List(), List(), 200))
  }

  def handleRunResults(requestId:String, runId:String, results:NodeSeq) = {
    val datastore = DatastoreServiceFactory.getDatastoreService
    val entity = new Entity("Run", requestId+":"+runId)
    entity.setProperty("requestId", requestId)
    entity.setProperty("runId", runId)
    entity.setProperty("url", results \ "results" \ "URL" text)

    entity.setProperty("loadTime", results \ "results" \ "loadTime" text)
    entity.setProperty("requests", results \ "results" \ "requests" text)
    entity.setProperty("render", results \ "results" \ "render" text)
    entity.setProperty("fullyLoaded", results \ "results" \ "fullyLoaded" text)
    entity.setProperty("docTime", results \ "results" \ "docTime" text)
    val rawDate:Long = (results \ "results" \ "date").text.toLong
    entity.setProperty("rawDate", rawDate)
    entity.setProperty("date", new DateTime(rawDate).toDate)
    entity.setProperty("rawXml", new Text(results toString))
    datastore.put(entity)

    val queue = QueueFactory.getDefaultQueue();
    val rawDataFiles = results \ "rawData"
    queue.add(withUrl("/fetch").param("requestId", requestId).param("runId", runId).param("type","headers").param("url", rawDataFiles \ "headers" text).method(GET))
    queue.add(withUrl("/fetch").param("requestId", requestId).param("runId", runId).param("type","pagedata").param("url", rawDataFiles \ "pageData" text).method(GET))
    queue.add(withUrl("/fetch").param("requestId", requestId).param("runId", runId).param("type","requests").param("url", rawDataFiles \ "requestsData" text).method(GET))
    queue.add(withUrl("/fetch").param("requestId", requestId).param("runId", runId).param("type","utilization").param("url", rawDataFiles \ "utilization" text).method(GET))
  }

  def handleRun(datastore:DatastoreService, run:Node, requestId:String) = {
    val runId = (run \ "id").text
    handleRunResults(requestId, runId+"A", run \ "firstView")
    handleRunResults(requestId, runId+"B", run \ "repeatView")
  }

  def doCallback = {
    val requestId = S.param("id").open_!
    val datastore = DatastoreServiceFactory.getDatastoreService
    val entity = datastore.get(KeyFactory.createKey("Test", requestId))
    entity.setProperty("ready", "1")
    datastore.put(entity)
    val xmlDetails = XML.load(new URL(entity.getProperty("xmlUrl").asInstanceOf[String]))
    xmlDetails \ "data" \ "run" foreach (run => handleRun(datastore, run, requestId))
    Full(XmlResponse(xmlDetails))
  }

  def doPoll = {
    val params=List(("url", "http://www.guardian.co.uk"), ("private", "1"), ("f", "xml"), ("runs", "3"), ("callback", "http://gu-monitoring.appspot.com/callback"), ("k", Props.get("apikey").open_!) )
    val testUrl =
      if ((S.request.open_!.hostName) == "localhost")
        "http://localhost:8081/runtest.xml?"+paramsToUrlParams(params)
      else
        "http://www.webpagetest.org/runtest.php?"+paramsToUrlParams(params)
    Logger("doPoll").info("Getting results from "+ testUrl)
    val response = XML.load(new URL(testUrl))
    Logger("doPoll").info("Got response "+ response)
    val datastore = DatastoreServiceFactory.getDatastoreService
    val requestId = response \\ "testId" text
    val entity = tryo {
      datastore.get(KeyFactory.createKey("Test", requestId))
    } openOr(new Entity("Test", requestId))
    entity.setProperty("requestId", requestId)
    entity.setProperty("xmlUrl", response \\ "xmlUrl" text)
    entity.setProperty("summaryCSV", response \\ "summaryCSV" text)
    entity.setProperty("detailsCSV", response \\ "detailsCSV" text)
    entity.setProperty("rawXml", new Text(response toString))
    entity.setProperty("ready", "0")
    datastore.put(entity)
    Full(XmlResponse(response))
  }

  def doDebugCreate = {
    val datastore = DatastoreServiceFactory.getDatastoreService
    val requestId = S.param("requestId").open_!
    val entity = tryo {
      datastore.get(KeyFactory.createKey("Test", requestId))
    } openOr(new Entity("Test", requestId))
    entity.setProperty("requestId", requestId)
    entity.setProperty("xmlUrl", "http://www.webpagetest.org/xmlResult/"+requestId+"/")
    entity.setProperty("summaryCSV", "http://www.webpagetest.org/result/"+requestId+"/page_data.csv")
    entity.setProperty("detailsCSV", "http://www.webpagetest.org/result/"+requestId+"/requests.csv")
    entity.setProperty("rawXml", new Text(""))
    entity.setProperty("ready", "1")
    datastore.put(entity)
    Full(PlainTextResponse("OK"))
  }

  def doDebugRefetch = {
    val datastore = DatastoreServiceFactory.getDatastoreService
    val queue = QueueFactory.getDefaultQueue();
    val ids = new Tests().unparsedTests.map(_.requestId);
    for (id <- ids) {
      queue.add(withUrl("/callback").param("id", id).method(GET))
    }
    Full(PlainTextResponse(ids.mkString("\r\n")))
  }

  def doDebugReverify = {
    val datastore = DatastoreServiceFactory.getDatastoreService
    val queue = QueueFactory.getDefaultQueue();
    val ids = new Tests().failedTests.map(_.requestId);
    for (id <- ids) {
      queue.add(withUrl("/callback").param("id", id).method(GET))
    }
    Full(PlainTextResponse(ids.mkString("\r\n")))
  }
}

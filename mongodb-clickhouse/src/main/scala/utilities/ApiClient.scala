

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory
import dispatch.{Http, Req, as}
import utilities.{DispatchUtil, LoggerMagnet, Path}

import scala.concurrent.{ExecutionContext, Future}

/** Parent class for API. Implements some convenience methods. In a Play project, it is better to use the com.makazi.utils.play.ApiClient which extends this
  * one with more methods.
  *
  * As a side note : be careful when using the '/' operator from Dispatch: it encodes the right-side argument, so any slashes presents in int will be encoded to
  * '%2F'. This is why we are rather using the '/' operator from Path.
  */
abstract class ApiClient {

  private val config = ConfigFactory.load().getConfig("mongodb-clickhouse.utils.commons.api")

  private val LoggedBodyLimit = 1000

  implicit val log: LoggerMagnet

  val url: String

  lazy val urlPath = Path(url)

  val charset = StandardCharsets.UTF_8

  /* For the next two attributes : the simpler getDuration method (with one argument only) is not accessible from Spark jobs, because the older version of
  Typesafe-Config (1.2.1) is loaded from Spark's libraries rather than our version (1.3).
  TODO check for Spark 2, was true for Spark 1 (Scala 2.10) which is no longer supported */

  /** The maximum time the ApiClient can wait when connecting to the remote host */
  val connectTimeoutInMillis: Long = config.getDuration("timeouts.connect", TimeUnit.MILLISECONDS)

  /** The maximum time the ApiClient waits for the response to be completed. Some API calls can be pretty long. */
  val requestTimeoutInMillis: Long = config.getDuration("timeouts.request", TimeUnit.MILLISECONDS)

  /** Configured Http object to launch requests. Do not directly use Http (with an uppercase) ! */
  val http: Http = Http.withConfiguration(_.setConnectTimeout(connectTimeoutInMillis.toInt).setRequestTimeout(requestTimeoutInMillis.toInt))

  private def traceRequest(req: Req) = log.trace {
    val httpRequest = req.toRequest
    val bodyStart = Option(httpRequest.getStringData) map (_.take(LoggedBodyLimit)) map ("\n" + _) getOrElse ""
    s"${httpRequest.getMethod} ${req.url}$bodyStart"
  }

  def close(): Unit = http.shutdown()

  /** Makes a call, expecting a String response with a status code of 2xx. If the status is not OK, the Future will fail with a WrongStatusCodeException. */
  protected def executeExpectingOk(req: Req)
                                  (implicit ec: ExecutionContext): Future[String] = {
    traceRequest(req)
    val handler = DispatchUtil.Handlers.alwaysOk(req, as.String.charset(charset))
    http(req.toRequest, handler)
  }

  /** Makes a call, expecting a String response with a status code of 2xx or a 404. If the status is neither, the Future will fail with a WrongStatusCodeException. */
  protected def executeExpectingOption(req: Req)
                                      (implicit ec: ExecutionContext): Future[Option[String]] = {
    traceRequest(req)
    val handler = DispatchUtil.Handlers.option(req, as.String.charset(charset))
    http(req.toRequest, handler)
  }

  /** Makes a call. Returns a tuple of the status code and the body. */
  protected def execute(req: Req)
                       (implicit ec: ExecutionContext): Future[(Int, String)] = {
    traceRequest(req)
    val handler = DispatchUtil.Handlers.default(req, identity)
    http(req.toRequest, handler) map {
      response => (response.getStatusCode, response.getResponseBody(charset))
    }
  }

  /** Sends an HTTP req without a body.
    *
    * @param method Sets the HTTP method on the request. For instance : _.GET */
  protected def callNoContent(method: Req => Req)(actionUrl: String, queryParameters: Map[String, String])
                             (implicit ec: ExecutionContext): Future[String] = {
    val request: Req = dispatch.url(urlPath / actionUrl) <<? queryParameters
    val response: Future[String] = executeExpectingOk(method(request))
    response
  }

  /** Sends an HTTP req with a body composed of JSON string.
    *
    * @param method Sets the HTTP method on the request.  For instance : _.GET */
  protected def callJsonContent(method: Req => Req)(actionUrl: String, json: String, queryParameters: Map[String, String])
                               (implicit ec: ExecutionContext): Future[String] = {
    val request: Req = dispatch.url(urlPath / actionUrl) <<? queryParameters << json
    val typedRequest = request.setContentType("application/json", StandardCharsets.UTF_8)
    val response: Future[String] = executeExpectingOk(method(typedRequest))
    response
  }

  /** Sends an HTTP req with a body composed of JSON string.
    *
    * @param method Sets the HTTP method on the request.  For instance : _.GET */
  protected def callStringContent(method: Req => Req)(actionUrl: String, string: String, queryParameters: Map[String, String])
                                 (implicit ec: ExecutionContext): Future[String] = {
    val request: Req = dispatch.url(urlPath / actionUrl) <<? queryParameters << string
    val response: Future[String] = executeExpectingOk(method(request))
    response
  }

  /**
    * Uses the DELETE method. Does nothing special with the response.
    *
    * @return the reponse as a String
    */
  protected def delete(actionUrl: String, queryParameters: Map[String, String] = Map())
                      (implicit ec: ExecutionContext): Future[String] =
    callNoContent(_.DELETE)(actionUrl, queryParameters)

  /**
    * Uses the DELETE method. If the response code was a 2xx, returns true. If the response code was 404, returns false.
    */
  protected def deleteIfExists(actionUrl: String, queryParameters: Map[String, String] = Map())
                              (implicit ec: ExecutionContext): Future[Boolean] = {
    val request: Req = dispatch.url(urlPath / actionUrl) <<? queryParameters
    val response: Future[Option[String]] = executeExpectingOption(request.DELETE)
    response map (_.isDefined)
  }

  /**
    * Uses the GET method. Does nothing special with the response.
    *
    * @return the reponse as a String
    */
  protected def getString(actionUrl: String, queryParameters: Map[String, String] = Map())
                         (implicit ec: ExecutionContext): Future[String] =
    callNoContent(_.GET)(actionUrl, queryParameters)

  /**
    * Uses the GET method. Does nothing special with the response.
    *
    * @return the reponse as a Option[String]
    */
  protected def getStringOption(actionUrl: String, queryParameters: Map[String, String] = Map())
                               (implicit ec: ExecutionContext): Future[Option[String]] = {
    val request: Req = dispatch.url(urlPath / actionUrl) <<? queryParameters
    val response: Future[Option[String]] = executeExpectingOption(request.GET)
    response
  }

  /**
    * Uses the POST method and sends a JSON String. Do not use this operation to send any String, a the MIME type in the req headers will be JSON. Does
    * nothing special with the response.
    *
    * @return the reponse as a String
    */
  protected def postJson(actionUrl: String, json: String, queryParameters: Map[String, String] = Map())
                        (implicit ec: ExecutionContext): Future[String] =
    callJsonContent(_.POST)(actionUrl, json, queryParameters)

  /**
    * Uses the POST method and sends a JSON String. Do not use this operation to send any String, a the MIME type in the req headers will be JSON. Does
    * nothing special with the response.
    *
    * @return the reponse as a String
    */
  protected def postString(actionUrl: String, string: String, queryParameters: Map[String, String] = Map())
                          (implicit ec: ExecutionContext): Future[String] =
    callStringContent(_.POST)(actionUrl, string, queryParameters)
  /**
    * Uses the PUT method and sends a JSON String. Do not use this operation to send any String, a the MIME type in the req headers will be JSON. Does
    * nothing special with the response.
    *
    * @return the reponse as a String
    */
  protected def putJson(actionUrl: String, json: String, queryParameters: Map[String, String] = Map())
                       (implicit ec: ExecutionContext): Future[String] =
    callJsonContent(_.PUT)(actionUrl, json, queryParameters)

  /**
    * Uses the PUT method and sends a JSON String. Do not use this operation to send any String, a the MIME type in the req headers will be JSON. Does
    * nothing special with the response.
    *
    * @return the reponse as a String
    */
  protected def patchJson(actionUrl: String, json: String, queryParameters: Map[String, String] = Map())
                         (implicit ec: ExecutionContext): Future[String] =
    callJsonContent(_.PATCH)(actionUrl, json, queryParameters)


  /** Uses the GET method on a given URL, and returns only the status code. */
  protected def getStatusCode(actionUrl: String, queryParameters: Map[String, String] = Map())
                             (implicit ec: ExecutionContext): Future[Int] = {
    val req: Req = dispatch.url(urlPath / actionUrl) <<? queryParameters
    http(req.GET.toRequest, DispatchUtil.getStatusCodeHandler(req)) map (_._1)
  }

  /** Uses the HEAD method on a given URL, and returns only the status code. */
  protected def headStatusCode(actionUrl: String, queryParameters: Map[String, String] = Map())
                              (implicit ec: ExecutionContext): Future[Int] = {
    val req: Req = dispatch.url(urlPath / actionUrl) <<? queryParameters
    http(req.HEAD.toRequest, DispatchUtil.getStatusCodeHandler(req)) map (_._1)
  }

  /**
    * Uses the PUT method without a body. Does nothing special with the response.
    *
    * @return the reponse as a String
    */
  protected def putNoContent(actionUrl: String, queryParameters: Map[String, String] = Map())
                            (implicit ec: ExecutionContext): Future[String] =
    callNoContent(_.PUT)(actionUrl, queryParameters)

  /**
    * Uses the POST method without a body. Does nothing special with the response.
    *
    * @return the reponse as a String
    */
  protected def postNoContent(actionUrl: String, queryParameters: Map[String, String] = Map())
                             (implicit ec: ExecutionContext): Future[String] =
    callNoContent(_.POST)(actionUrl, queryParameters)

}

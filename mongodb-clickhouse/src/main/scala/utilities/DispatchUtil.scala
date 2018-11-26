package utilities

import dispatch.{FunctionHandler, Req}
import io.netty.handler.codec.http.HttpHeaders
import org.asynchttpclient._

/**
  * Additional utilities for the Dispatch framework.
  */
object DispatchUtil {

  /** Log as much as we can on a legacy Dispatch call. Prefer making a call using getBetterRequestHandler. */
  def logCallError(request: dispatch.Req)(implicit log: LoggerMagnet): PartialFunction[Throwable, Nothing] = {
    case e =>
      log.error(s"Exception when handling request ${request.url}", e)
      throw e
  }

  /** The default handlers of Dispatch presents two nasty flaws : errors during the connection are logged at DEBUG level, and when we filter on the OK status
    * the message body is lost whenever the actual status is not as expected.
    *
    * With these handkers, errors will be logged at the ERROR level. In case of a status code that is filtered out, a WrongStatusCodeException will be thrown,
    * which  contains the response body as a String.
    */
  object Handlers {

    private abstract class BaseHandler[T](request: Req)(implicit log: LoggerMagnet) extends AsyncCompletionHandler[T] {

      def workOnStatus: PartialFunction[Int, (Response => T)]

      override def onCompleted(response: Response): T = {
        val status = response.getStatusCode
        if (workOnStatus.isDefinedAt(status)) {
          workOnStatus(status)(response)
        } else {
          val body = response.getResponseBody
          log.debug(s"Wrong status $status on request ${request.url}: \n$body")
          // don't log in error level, might be an expected exception and caught later
          throw WrongStatusCodeException(status, request.url, body)
        }
      }

      override def onThrowable(th: Throwable): Unit = {
        log.error(s"Exception when handling request ${request.url}", th)
      }
    }

    def default[T](request: Req, f: Response => T)(implicit log: LoggerMagnet): AsyncCompletionHandler[T] =
      new BaseHandler[T](request) {
        override def workOnStatus: PartialFunction[Int, (Response => T)] = {
          case _ => f
        }
      }

    /**
      * Rejects any status code that is not in the 2xx family.
      *
      * @param f Function to transforme the response in the expected type. For instance, dispatch.as.String.
      */
    def alwaysOk[T](request: Req, f: Response => T)(implicit log: LoggerMagnet): AsyncCompletionHandler[T] =
      new BaseHandler[T](request) {
        override def workOnStatus: PartialFunction[Int, (Response => T)] = {
          case status if (status / 100 == 2) => f
        }
      }

    /**
      * Any status code that is in the 2xx family returns a Some, and a 404 returns a None. Other status throw an exception.
      *
      * @param f Function to transforme the response in the expected type. For instance, dispatch.as.String.
      */
    def option[T](request: Req, f: Response => T)(implicit log: LoggerMagnet): AsyncCompletionHandler[Option[T]] =
      new BaseHandler[Option[T]](request) {
        override def workOnStatus: PartialFunction[Int, (Response => Option[T])] = {
          case status if (status / 100 == 2) => f andThen (Some(_))
          case 404 => (_ => None)
        }
      }

    /**
      * Any status code that is in the 2xx family returns a Right, other status returns a LEft. In both case, the Either contain a tuple with the status code
      * and the body transformed by the appropriate function.
      *
      * @param successFunction Function to transforme the response in the expected type. For instance, dispatch.as.String.
      * @param errorFunction   Function to transforme the body to whatever in case of an error. For instance, dispatch.as.String.
      */
    def successOrFailure[T](request: Req, successFunction: Response => T, errorFunction: Response => T)(implicit log: LoggerMagnet): AsyncCompletionHandler[Either[(Int, T), (Int, T)]] =
      new BaseHandler[Either[(Int, T), (Int, T)]](request) {
        override def workOnStatus: PartialFunction[Int, (Response) => Either[(Int, T), (Int, T)]] = {
          case success if (success / 100 == 2) => successFunction andThen (Right(success, _))
          case error => errorFunction andThen (Left(error, _))
        }
      }
  }

  /** The default handlers of Dispatch presents two nasty flaws : errors during the connection are logged at DEBUG level, and when we filter on the OK status
    * the message body is lost whenever the actual status is not as expected.
    *
    * If any error occurs, it will be logged at the ERROR level.
    *
    * If we filter on the OK status, a WrongStatusCodeException will be thrown, which contains the response body as a String.
    *
    * @param f             Function to transforme the response in the expected type. For instance, dispatch.as.String.
    * @param filterOk      Set true to transofmr any response which is not in the 2xx status family in an exception. If set to true, the second filter has no effect.
    * @param filterOkOr404 Set true to transform any response which is not in the 2xx status family in an exception.
    */
  def getBetterHandler[T](request: Req, f: Response => T, filterOk: Boolean, filterOkOr404: Boolean = false)(implicit log: LoggerMagnet) = new FunctionHandler[T](f) {

    override def onCompleted(response: Response): T = {
      val status = response.getStatusCode
      if (!filterOk || status / 100 == 2) {
        super.onCompleted(response)
      } else {
        val body = response.getResponseBody
        log.error(s"Wrong status $status on request ${request.url}: \n$body")
        throw WrongStatusCodeException(status, request.url, body)
      }
    }

    override def onThrowable(th: Throwable): Unit = {
      log.error(s"Exception when handling request ${request.url}", th)
    }
  }

  /** This handler will return only the status code and text of the response. This means we do not need to wait for the entire response to be received, we can
    * abort the communication as soon as the status has been received.
    *
    * If any error occurs, it will be logged at the ERROR level.
    */
  def getStatusCodeHandler(request: Req)(implicit log: LoggerMagnet) = new AsyncHandler[(Int, String)] {

    var status: Option[(Int, String)] = None

    override def onStatusReceived(st: HttpResponseStatus) = {
      status = Some(st.getStatusCode, st.getStatusText)
      AsyncHandler.State.ABORT
    }

    override def onBodyPartReceived(bodyPart: HttpResponseBodyPart) = AsyncHandler.State.ABORT

    override def onHeadersReceived(headers: HttpHeaders) = AsyncHandler.State.ABORT

    override def onCompleted = status.getOrElse {
      val msg = s"No status found for request ${request.url}"
      log.error(msg)
      throw new UnexpectedApiClientException(msg, request.url)
    }

    override def onThrowable(th: Throwable): Unit = {
      val msg = s"Exception when handling request ${request.url}"
      log.error(msg, th)
      throw new UnexpectedApiClientException(msg, request.url, th);
    }

  }

}

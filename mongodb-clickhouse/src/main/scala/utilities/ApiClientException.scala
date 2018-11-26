package utilities

sealed abstract class ApiClientException(msg: String, url: String, cause: Throwable = null) extends java.io.IOException(msg, cause)

/** Exception thrown when the status code is not as expected (typically, is not in the 2xx family). */
case class WrongStatusCodeException(status: Int, url: String, body: String)
  extends ApiClientException(s"Status code $status on $url.\n" + (if (body == null) "" else body), url, null) {

  /** Status code category indicated by the first digit of the code */
  val statusClass = status / 100

  /** Returns `true` if the status code corresponds to an information response (1xx status) */
  val isInformational: Boolean = statusClass == 1

  /** Returns `true` if the status code indicates a success (2xx status) */
  val isSuccess: Boolean = statusClass == 2

  /** Returns `true` if the status code indicates a redirection (3xx status) */
  val isRedirection: Boolean = statusClass == 3

  /** Returns `true` if the status code indicates a client error (4xx status) */
  val isClientError: Boolean = statusClass == 4

  /** Returns `true` if the status code indicates a server error (5xx status) */
  val isServerError: Boolean = statusClass == 5
}

/** Exception thrown when the clients fails in a weird way. */
case class UnexpectedApiClientException(msg: String, url: String, cause: Throwable = null) extends ApiClientException(msg, url, cause)





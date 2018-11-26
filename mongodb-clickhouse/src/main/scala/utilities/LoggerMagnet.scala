package utilities

/**
  *  Implementation of the Magnet pattern for loggers. All loggers from various logging frameworks are forced to this type, via implicit conversions.
  */
trait LoggerMagnet {
  def trace(msg: => String): Unit
  def trace(msg: => String, th: => Throwable): Unit
  def debug(msg: => String): Unit
  def debug(msg: => String, th: => Throwable): Unit
  def info(msg: => String): Unit
  def info(msg: => String, th: => Throwable): Unit
  def warn(msg: => String): Unit
  def warn(msg: => String, th: => Throwable): Unit
  def error(msg: => String): Unit
  def error(msg: => String, th: => Throwable): Unit
  def isDebugEnabled: Boolean
}

/** LoggerLike Object.
  *
  * @author Gaël Renoux
  */
object LoggerMagnet {

  /* So as not to depends on Play : use a structural type */
  type PlayLogger = {
    def trace(msg: => String): Unit
    def trace(msg: => String, th: => Throwable): Unit
    def debug(msg: => String): Any
    def debug(msg: => String, th: => Throwable): Any
    def info(msg: => String): Any
    def info(msg: => String, th: => Throwable): Any
    def warn(msg: => String): Any
    def warn(msg: => String, th: => Throwable): Any
    def error(msg: => String): Any
    def error(msg: => String, th: => Throwable): Any
    def isDebugEnabled: Boolean
  }

  implicit def fromSlf4j(log : org.slf4j.Logger) : LoggerMagnet = new Slf4jAdapter(log)
  implicit def fromScalaLogger(log : com.typesafe.scalalogging.Logger) : LoggerMagnet = new ScalaLoggerAdapter(log)
  implicit def fromPlay(log : PlayLogger) : LoggerMagnet = new PlayAdapter(log)

  private class Slf4jAdapter(val log : org.slf4j.Logger) extends LoggerMagnet {
    def trace(msg: => String) = if (log.isTraceEnabled()) log.trace(msg)
    def trace(msg: => String, th: => Throwable) = if (log.isTraceEnabled) log.trace(msg, th)
    def debug(msg: => String) = if (log.isDebugEnabled) log.debug(msg)
    def debug(msg: => String, th: => Throwable) = if (log.isDebugEnabled) log.debug(msg, th)
    def info(msg: => String) = if (log.isInfoEnabled) log.info(msg)
    def info(msg: => String, th: => Throwable) = if (log.isInfoEnabled) log.info(msg, th)
    def warn(msg: => String) = if (log.isWarnEnabled) log.warn(msg)
    def warn(msg: => String, th: => Throwable) = if (log.isWarnEnabled) log.warn(msg, th)
    def error(msg: => String) = if (log.isErrorEnabled) log.error(msg)
    def error(msg: => String, th: => Throwable) = if (log.isErrorEnabled) log.error(msg, th)
    def isDebugEnabled = log.isDebugEnabled()
  }

  private class ScalaLoggerAdapter(val log : com.typesafe.scalalogging.Logger) extends LoggerMagnet {
    def trace(msg: => String) = log.trace(msg)
    def trace(msg: => String, th: => Throwable) = log.trace(msg, th)
    def debug(msg: => String) = log.debug(msg)
    def debug(msg: => String, th: => Throwable) = log.debug(msg, th)
    def info(msg: => String) = log.info(msg)
    def info(msg: => String, th: => Throwable) = log.info(msg, th)
    def warn(msg: => String) = log.warn(msg)
    def warn(msg: => String, th: => Throwable) = log.warn(msg, th)
    def error(msg: => String) = log.error(msg)
    def error(msg: => String, th: => Throwable) = log.error(msg, th)

    /* Mostly useless on Scala logger because using by-name arguments */
    def isDebugEnabled = true
  }

  private class PlayAdapter(val log : PlayLogger) extends LoggerMagnet {
    def trace(msg: => String) = { val _ = log.trace(msg) }
    def trace(msg: => String, th: => Throwable) = { val _ = log.trace(msg, th) }
    def debug(msg: => String) = { val _ = log.debug(msg) }
    def debug(msg: => String, th: => Throwable) = { val _ = log.debug(msg, th) }
    def info(msg: => String) = { val _ = log.info(msg) }
    def info(msg: => String, th: => Throwable) = { val _ = log.info(msg, th) }
    def warn(msg: => String) = { val _ = log.warn(msg) }
    def warn(msg: => String, th: => Throwable) = { val _ = log.warn(msg, th) }
    def error(msg: => String) = { val _ = log.error(msg) }
    def error(msg: => String, th: => Throwable) = { val _ = log.error(msg, th) }
    def isDebugEnabled = log.isDebugEnabled
  }

}
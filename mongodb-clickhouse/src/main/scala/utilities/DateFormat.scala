package utilities

import java.text.SimpleDateFormat
import java.util.Date

object  DateFormat {
  val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"

  def getDateAsString(d: Date): String = {
    val dateFormat = new SimpleDateFormat(DATE_FORMAT)
    dateFormat.format(d)
  }

  def convertStringToDate(s: String): Date = {
    val dateFormat = new SimpleDateFormat(DATE_FORMAT)
    dateFormat.parse(s)
  }
}

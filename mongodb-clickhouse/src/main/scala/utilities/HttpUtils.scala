package utilities

object HttpUtils {
  def isSuccessCode(code: Int) = (code / 100 == 2)
}

package utilities

/**
  * Represents a path : path on the disk, URL, etc. Allows for utilities to compose paths, change extension, etc.
  *
  * @author Gaël Renoux
  */
case class Path(content: String, separator: Char = '/') {

  val extensionPoint = '.'

  /** Returns this path as a String. */
  override val toString: String = content

  /** Returns true if this path ends with a separator. */
  lazy val endsWithSeparator: Boolean = content.last == separator

  /** Returns this path ending with a separator if it isn't already the case. */
  def / : Path = if (endsWithSeparator) this else Path(content + separator)

  /** Compose a path with a new element and merges the separator. I.e., concatenating /some/root/ and /whatever produces
    * /some/root/whatever (and not /some/root///whatever). If there are consecutive separators in the string that are submitted though, they will not be
    * removed. Therefore, concatenating /some/root// and //whatever produces /some/root///whatever (only the final / of the left operand and the
    * initial / of the right operand were merged). */
  def /(suffix: String): Path = {
    val left = if (endsWithSeparator) content else content + separator
    val right = if (suffix.head == separator) suffix.tail else suffix
    Path(left + right)
  }

  /** Composes two paths together, following the same rules as the methode taking a String. */
  def /(suffix: Path): Path = this / suffix.content

  lazy val isEmpty: Boolean = content.isEmpty

  lazy val isRoot: Boolean = content == separator.toString

  def parent: Path = {
    val newContent = content.substring(0, content.lastIndexOf(separator))
    this.copy(content = newContent)
  }

  /**
    * @deprecated last is too ambiguous a method name. Prefer using lastElement or afterLastSeparator
    */
  @deprecated("last is too ambiguous a method name. Prefer using lastElement or afterLastSeparator", "2.8.0")
  lazy val last: String = content.substring(content.lastIndexOf(separator))

  /** Whatever is left after the last instance of the separator. Will be empty if this Path ends with a separator. */
  lazy val afterLastSeparator: String = content.substring(content.lastIndexOf(separator) + 1)

  /** Last non-empty element. Will end with whatever number of separators needed to reach the end of the Path. Will not start with a separator. If none existe
    * (this Path is made of separators only, or is empty, returns the full content of this Path). */
  lazy val lastElement: String = {
    val reversedContent = content.view.reverse
    val (endingSeparators, reverseContenWithoutEndingSeparators) = reversedContent span (_ == separator)
    val reversedLastElementWithoutSeparators = reverseContenWithoutEndingSeparators takeWhile (_ != separator)
    /* no need to reverse ending separators, it's the same characters repeatedly */
    reversedLastElementWithoutSeparators.reverse.force + endingSeparators.force
  }


  /** Short version of withExtension (for the DSL). Cannot use point. */
  def *(ext: String): Path = withExtension(ext)

  /** Short version of withoutExtension (for the DSL). Cannot use point. */
  def *(n: None.type): Path = withoutExtension

  /** Returns this path with the extension in parameter. Any existing extension is replaced. */
  def withExtension(ext: String): Path = contentWithoutExtension match {
    case None => copy(content = content + extensionPoint + removePoint(ext))
    case Some(newContent) => copy(content = newContent + extensionPoint + removePoint(ext))
  }

  /** Returns this path without the extension. */
  def withoutExtension: Path = contentWithoutExtension match {
    case None => this
    case Some(newContent) => copy(content = newContent)
  }

  lazy val withoutRoot: Path = if (content.nonEmpty && content.head == separator) Path(content.drop(1)) else this

  /** Removes the initial point from an extension. */
  private def removePoint(ext: String) = if (ext(0) == extensionPoint) ext.drop(1) else ext

  /** Returns None if the content is already without an extension (avoids creating several path with identical content), or Some(content) where the extension
    * was stripped otherwise. */
  private lazy val contentWithoutExtension = {
    val lastIndexOfPoint = content.lastIndexOf(extensionPoint)
    val lastIndexOfSeparator = content.lastIndexOf(separator)
    if (lastIndexOfSeparator > lastIndexOfPoint) {
      /* the last point is within a folder, it's not considered an extension */
      None
    } else {
      Some(content.substring(0, lastIndexOfPoint))
    }
  }

  lazy val extension: Option[String] = {
    val lastIndexOfPoint = content.lastIndexOf(extensionPoint)
    val lastIndexOfSeparator = content.lastIndexOf(separator)
    if (lastIndexOfSeparator > lastIndexOfPoint) {
      /* the last point is within a folder, it's not considered an extension */
      None
    } else {
      Some(content.substring(lastIndexOfPoint + 1))
    }
  }

}

class PathStart(separator: Char) {
  def apply(elements: String*): Path = if (elements.isEmpty) Path("", separator) else {
    val pathStart = new Path(elements.head, separator)
    elements.tail.foldLeft(pathStart)(_ / _)
  }

  /* More elegant way to start a root Path */
  def /(element: String) = new Path(separator + element, separator)

  /* More elegant way to start a relative Path */
  def %(element: String) = new Path(element, separator)

  val / = new Path(separator.toString, separator)

  val Empty = new Path("", separator)
}

/** Path companion object */
object Path extends PathStart('/') {

  def separator(sep: Char) = new PathStart(sep)

  /** Implicit conversion to String */
  implicit def pathToString(p: Path): String = p.toString
}

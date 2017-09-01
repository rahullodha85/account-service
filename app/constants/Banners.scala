package constants

object Banners extends Enumeration {
  val Saks, Off5th, LordAndTaylor = Value

  def apply(name: String): Banners.Value = {
    name.toLowerCase match {
      case "s5a" => Saks
      case "o5a" => Off5th
      case "lat" => LordAndTaylor
      case _     => throw UnknownBannerException(s"Unknown banner $name")
    }
  }
}

case class UnknownBannerException(banner: String) extends Exception

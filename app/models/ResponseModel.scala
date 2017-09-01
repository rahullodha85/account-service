package models

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.Cookie

trait CookieSerializer {
  implicit val cookieFormat = Json.format[Cookie]
}

sealed trait ResponseData

case class SuccessfulResponse[T](
  body:    T,
  cookies: Seq[Cookie] = Seq.empty
) extends ResponseData

case class FailureResponse(
  errors:  Seq[ApiErrorModel],
  code:    Int,
  cookies: Seq[Cookie]        = Seq.empty
) extends ResponseData

object SuccessfulResponse extends CookieSerializer {
  implicit def successfulResponseFormat[T: Format]: Format[SuccessfulResponse[T]] = (
    (__ \ "body").format[T] and
    (__ \ "cookies").format[Seq[Cookie]]
  )(SuccessfulResponse.apply, unlift(SuccessfulResponse.unapply))
}

object FailureResponse extends CookieSerializer {
  implicit val failureResponseFormat: Format[FailureResponse] = Json.format[FailureResponse]
}

package helpers

import constants.Constants._
import models._
import play.Logger
import play.api.libs.json.{ JsResultException, JsSuccess, _ }
import play.api.mvc.{ Result, _ }
import net.logstash.logback.argument.StructuredArguments._

import scala.util.control.NonFatal

trait ControllerPayload extends Controller {

  ////////////////////////
  //      RESPONSE      //
  ////////////////////////

  def writeResponseStore[T](result: T)(implicit request: Request[_], writes: Writes[T]): Result =
    writeResponseSuccess(result, Created)

  def writeResponseStores[T](results: Seq[T])(implicit request: Request[_], writes: Writes[T]): Result =
    writeResponses(results, Created)

  def writeResponseGet[T](response: T, errors: Seq[ApiErrorModel] = Seq())(implicit request: Request[_], writes: Writes[T]): Result =
    writeResponseSuccess(response, Ok, errors)

  def writeResponseUpdate[T](result: T)(implicit request: Request[_], writes: Writes[T]): Result =
    writeResponseSuccess(result, Ok)

  def writeResponseUpdates[T](results: Seq[T])(implicit request: Request[_], writes: Writes[T]): Result =
    writeResponses(results, Ok)

  def writeResponseRemove[T](result: T)(implicit request: Request[_], writes: Writes[T]): Result =
    writeResponseSuccess(result, Ok)

  def writeResponseSuccess[T](result: T, status: Status, errors: Seq[ApiErrorModel] = Seq())(implicit request: RequestHeader, writes: Writes[T]): Result =
    writeResponse(status, constructResponseModel(result, errors))

  def writeResponseError(errors: Seq[ApiErrorModel], status: Status)(implicit request: RequestHeader): Result =
    formatResponse(constructErrorResponseModel(errors), status)

  def writeResponse(responseStatus: Status, body: ApiModel): Result =
    responseStatus.apply(Json.prettyPrint(Json.toJson(body))).as(JSON)

  def writeResponse[T](res: (T, Seq[Cookie], Seq[ApiErrorModel], Int))(implicit request: Request[_], writes: Writes[T]) = {
    res._4 match {
      case 200 => writeResponseGet(res._1, res._3).withCookies(res._2: _*)
      case _   => formatResponse(constructResponseModel(res._1, res._3), ControllerPayload.this.Status(res._4)).withCookies(res._2: _*)
    }
  }

  def constructResultModel[T](result: T)(implicit writes: Writes[T]): ApiResultModel = ApiResultModel(Json.toJson(result))

  def constructResponseModel[T](result: T, errs: Seq[ApiErrorModel] = Seq())(implicit request: RequestHeader, writes: Writes[T]): ApiModel =
    ApiModel.apply(
      ApiRequestModel.fromReq(request),
      constructResultModel(result),
      errs
    )

  def constructErrorResponseModel(errs: Seq[ApiErrorModel])(implicit request: RequestHeader): ApiModel =
    ApiModel.apply(
      ApiRequestModel.fromReq(request),
      EmptyApiResultModel,
      errs
    )

  private def formatResponse(responseModel: ApiModel, response: Status): Result =
    response.apply(Json.prettyPrint(Json.toJson(responseModel))).as(JSON)

  private def writeResponses[T](
    results: Seq[T],
    status:  Status
  )(implicit request: Request[_], writes: Writes[T]): Result =
    formatResponse(constructResponseModel(results), status)

  ////////////////////////
  //     GET ITEMS      //
  ////////////////////////

  def getRequestItem[T: Format](implicit request: Request[AnyContent]): T = {
    val readJsonObject: Format[JsValue] = (__ \ "item").format[JsValue]
    getRequestBodyAsJson(request).validate(readJsonObject) match {
      case JsError(e) => throw new JsResultException(e)
      case JsSuccess(hbcStatusObject, _) =>
        //Validate the hbcStatus object
        hbcStatusObject.validate[T] match {
          case JsSuccess(hbcStatus, _) => hbcStatus
          case JsError(e)              => throw new JsResultException(e)
        }
    }
  }

  def getRequestItems[T: Format](implicit request: Request[AnyContent]): Seq[T] = {
    val readJsonObject: Format[Seq[JsValue]] = (__ \ "items").format[Seq[JsValue]]
    getRequestBodyAsJson(request).validate(readJsonObject) match {
      case JsError(e) => throw new JsResultException(e)
      case JsSuccess(hbcStatusObjectList, _) =>
        hbcStatusObjectList.map(hbcStatusObject =>
          hbcStatusObject.validate[T] match {
            case JsSuccess(hbcStatus, _) => hbcStatus
            case JsError(e)              => throw new JsResultException(e)
          })
    }
  }

  ////////////////////////
  //      HELPERS       //
  ////////////////////////

  private def getRequestBodyAsJson(implicit request: Request[AnyContent]): JsValue =
    request.body.asJson.fold(throw new IllegalArgumentException("no json found"))(x => x)

  def findResponseStatus(implicit request: RequestHeader): PartialFunction[Throwable, (Status, ApiErrorModel, Seq[Cookie])] = {
    case e: UnauthorizedException =>
      Logger.warn(e.getMessage, e)
      (Unauthorized, ApiErrorModel.fromException(e), e.cookies)
    case NonFatal(e) =>
      if (ConfigHelper.getBooleanProp(DEV_MODE)) {
        throw e
      } else {
        Logger.error(e.getMessage, e)
        (InternalServerError, ApiErrorModel(GENERIC_ERROR_MESSAGE, GENERIC_ERROR), Seq.empty)
      }
  }

  def handlerForRequest(implicit req: RequestHeader): (Status, ApiErrorModel, Seq[Cookie]) => Result = {
    (status, err, cookies) =>
      writeResponse(
        status,
        constructErrorResponseModel(Seq(err))
      ).withCookies(cookies: _*)
  }

  def getJsessionId(implicit request: Request[AnyContent]): Option[String] = request.cookies.get(JSESSIONID).map(_.value)

  def defaultExceptionHandler(implicit req: RequestHeader): PartialFunction[Throwable, Result] =
    findResponseStatus andThen handlerForRequest(req).tupled
}

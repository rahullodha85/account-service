package validations

import javax.inject.Inject

import messages.HBCMessagesApi
import models.ApiErrorModel
import play.api.data.validation.ValidationError
import play.api.i18n.{ I18nSupport, Messages }
import play.api.libs.json._

class Validator @Inject() (val messagesApi: HBCMessagesApi) extends I18nSupport {

  def validate[T](payload: Option[JsValue])(implicit r: Reads[T]): Either[Seq[ApiErrorModel], T] = {
    payload.map { value =>
      value.validate[T] match {
        case JsSuccess(v, _) => Right(v)
        case JsError(e)      => Left(transformToApiErrors(e))
      }
    }.getOrElse {
      Left(Seq(ApiErrorModel("Unable to find json payload, expected content-type is application/json", "Invalid content type")))
    }
  }

  /**
   * Performs the validation of the payload according to the rule set.
   * it collects the errors for all fields specified in the rule set returning for each field only the first error found and convert them into APIError.
   */
  def validate(ruleSet: Map[String, List[(Rule, String, String)]], payload: JsValue): Seq[ApiErrorModel] = {
    validate(ruleSet, payload, false, false).flatMap {
      case (key, value) =>
        value.flatMap { errorMap =>
          errorMap.map {
            case (k, v) => ApiErrorModel(v, s"$key.$k")
          }
        }
    }.toSeq.distinct
  }

  def transformToApiErrors(errors: Seq[(JsPath, Seq[ValidationError])]): Seq[ApiErrorModel] = {
    errors.flatMap { error =>
      error._2.map { validationError =>
        val jsonKey: String = getJsonKey(error)
        val message: String = Messages(jsonKey + "." + validationError.message)
        ApiErrorModel(message, jsonKey)
      }
    }
  }

  def getJsonKey(error: (JsPath, Seq[ValidationError])): String = {
    error._1.toJsonString.replaceFirst("^obj.", "")
  }

  /**
   * Performs the validation of the payload according to the rule set and the preferences given by the boolean parameters.
   *
   * @param ruleSet validation rules set expressed as a map where each entry's key is the field name and value is the list of validation rules.
   *                Along with each validation rule the error code and error message for that rule has to be specified.
   *                Convention on key names: "multi_" means validation is related to more than just one field (some custom validation).
   * @param payload payload to be validated
   * @param returnFirstErrorFoundOnly when true it returns only one error (the first) for the first field failing a validation rule.
   * @param returnFirstErrorFoundForEachField when true it returns only the first error encountered for each field failing validation rules.
   * @return validation errors found expressed as a map where keys are the field names and values are the list of errors found each one expressed
   *         as a map having the error code and the error message.
   */
  def validate(ruleSet: Map[String, List[(Rule, String, String)]], payload: JsValue, returnFirstErrorFoundOnly: Boolean, returnFirstErrorFoundForEachField: Boolean): Map[String, List[Map[String, String]]] = {
    val validationErrors = ruleSet map {
      case (fieldName, ruleList) => {
        val validationResults = ruleList map {
          case (validator: Rule, error, message) => {
            validator.process(payload) match {
              case true  => null
              case false => Map(error -> message)
            }
          }
        } filter (_ != null) //filter out successful validation elements
        (fieldName, validationResults)
      }
    } filterNot (_._2.isEmpty) //filter out entries where all rules were validate successfully (empty list of errors)

    //Filtering preferences
    returnFirstErrorFoundOnly match {
      case true =>
        //take the first error of the error list for the first field having errors
        validationErrors.take(1) map {
          case (fieldName, errorsList) => (fieldName, errorsList.take(1))
        }
      case _ =>
        if (returnFirstErrorFoundForEachField) {
          //take the first error for each error list of all fields having errors
          validationErrors map {
            case (fieldName, errorsList) => (fieldName, errorsList.take(1))
          }
        } else {
          validationErrors
        }
    }
  }

  def paymentMethodValidation = Map(
    "month" -> List((PaymentMethodExpiryDate, "invalid_expiration_date", Messages("payment_expiration_date_invalid"))),
    "year" -> List((FutureOrCurrentYear, "invalid_expiration_year", Messages("year.error.invalid")))
  )
}

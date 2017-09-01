package validations

import org.joda.time.YearMonth
import play.api.libs.json._

trait Rule {
  def process(dataPoint: JsValue): Boolean
}

object FutureOrCurrentYear extends Rule {
  def process(dataPoint: JsValue): Boolean = {
    if ((dataPoint \ "brand").asOpt[String].exists(v => "SAKS".equals(v)))
      true
    else {
      (dataPoint \ "year").asOpt[Int].exists(_ >= new YearMonth().getYear)
    }
  }
}

object PaymentMethodExpiryDate extends Rule {
  private def validateExpirationDate(payload: JsValue): Boolean = {
    def currentDate = YearMonth.now()
    def getIntValue(payload: JsLookupResult) = {
      payload.getOrElse(JsNull) match {
        case v: JsNumber => v.value
        case _           => BigDecimal(0)
      }
    }.toInt
    val year = getIntValue(payload \ "year")
    val month = getIntValue(payload \ "month")
    if (year == 0 || month == 0) return false
    val creditCardExpirationDate = new YearMonth(year, month)
    !creditCardExpirationDate.isBefore(currentDate)
  }

  override def process(dataPoint: JsValue): Boolean = {
    if ((dataPoint \ "brand").asOpt[String].exists(_.equals("SAKS")))
      true
    else
      validateExpirationDate(dataPoint)
  }
}

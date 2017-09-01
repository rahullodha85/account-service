package models

import models.website.PaymentMethodType
import play.api.libs.json._
import play.api.libs.json.Reads._

trait FieldConstraints {
  def email = pattern("""^[\s]*[A-Za-z0-9]+[\w-_.]*[\w-_]*@[\w-]+(?:\.[\w-_]+)+[\s]*$""".r)
  def password = pattern("""^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[ !"#$%&'()*+,-./:;<=>?@\[\]\\^_`{|}~])[0-9a-zA-Z !"#$%&'()*+,-./:;<=>?@\[\]\\^_`{|}~]{8,}$""".r)
  def zip = pattern("""^[a-zA-Z0-9\-\s]{3,16}$""".r)
  def empty = pattern("""^\s*$""".r)
  def name = pattern("""^[\s]*[a-zA-Z0-9\-\'\p{L}\p{M}]{1,40}[\s]*$""".r)
  def requiredPhone = pattern("""^[+]?[\d \s\.\-]{7,20}+$""".r)
  def any40Characters = pattern("""[^;\*]{1,40}""".r)
  def title = pattern("""^[a-zA-Z\s\p{L}]{1,40}$""".r)
  def any80Characters = pattern("""[^;\*]{1,80}""".r)
  def alphaNumeric = pattern("""^[a-zA-Z0-9]{1,50}$""".r)
  def fullName = pattern("""^[a-zA-Z\s]{1,80}$""".r)
  def numbersAndSpaces = pattern("""^[\d\s]*$""".r)
  def lengthBetween(min: Int, max: Int) = Reads.verifying[String](v => {
    val length = v.trim.replaceAll("\\s", "").length
    length >= min && length <= max
  })
  def awardAmount = Reads.verifying[Long](awardAmount => {
    val isEvenlyDivisibleBy25 = awardAmount % 25 == 0
    isEvenlyDivisibleBy25 && awardAmount > 0
  })
  def digitsOnly = pattern("""^\d+$""".r)
  def minLength(length: Int) = Reads.minLength[String](length)
  def maxLength(length: Int) = Reads.maxLength[String](length)
  def TorFValue = Reads.verifying[String](v => Seq("F", "T").exists(_.equals(v.trim)))
  def validBrand = Reads.verifying[String](v => PaymentMethodType.isValidBrandName(v))

  def pattern(regex: => scala.util.matching.Regex, error: String = "error.pattern")(implicit reads: Reads[String]) = {
    Reads[String](jsValue => reads.reads(jsValue).flatMap { value =>
      regex.unapplySeq(value.trim).map(_ => JsSuccess(value.trim)).getOrElse(JsError(error))
    })
  }
}


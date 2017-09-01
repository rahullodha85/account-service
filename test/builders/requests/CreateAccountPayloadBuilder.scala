package builders.requests

import models.website.CreateAccountRequest
import play.api.libs.json.Json

class CreateAccountPayloadBuilder {
  var first_name: String = "Crouton"
  var last_name: String = "Smith"
  var password: String = "Test.ing1"
  var confirm_password: String = "Test.ing1"
  var email: String = "csmith@obs1.com"
  var canadian_customer: String = "T"
  var canadian_customer_opt_in: Boolean = true
  val ip_address: String = "127.0.0.1"
  val saks_opt_status: String = "T"
  val off5th_opt_status: String = "T"
  var phoneNumber = "2122223333"
  var postalCode = "12345"
  var preferences = Seq.empty[String]

  def build() = {
    CreateAccountRequest(first_name, last_name, password, confirm_password, email, Option(canadian_customer), Option(canadian_customer_opt_in),
      Option(saks_opt_status), Option(off5th_opt_status), Option(phoneNumber), Option(postalCode), Option(preferences))
  }

  def buildJson() = {
    Json.obj(
      "first_name" -> first_name,
      "middle_name" -> "",
      "canadian_customer" -> true,
      "last_name" -> last_name,
      "password" -> "Password#1",
      "confirm_password" -> "Password#1",
      "email" -> email,
      "canadian_customer" -> "F",
      "receive_email" -> true,
      "saksfirst_member" -> false,
      "off5th_opt_status" -> off5th_opt_status,
      "saks_opt_status" -> saks_opt_status,
      "phone_number" -> phoneNumber,
      "zip" -> postalCode
    )
  }

  def getCreateProfilePayload = Json.obj(
    "first_name" -> first_name,
    "last_name" -> last_name,
    "canadian_customer" -> true,
    "email_address" -> email,
    "subscribed_date" -> "2016-04-11T20:14:32.376Z",
    "opt_status" -> off5th_opt_status,
    "saks_opt_status" -> saks_opt_status,
    "ip_address" -> ip_address
  )

  def viewOrRegisterMoreAccount = Json.obj(
    "first_name" -> first_name,
    "last_name" -> last_name,
    "email_address" -> email,
    "phone_number" -> phoneNumber,
    "postal_code" -> postalCode
  )

  def withPhoneNumber(number: String): CreateAccountPayloadBuilder = {
    this.phoneNumber = number
    this
  }

  def withPostalCode(code: String): CreateAccountPayloadBuilder = {
    this.postalCode = code
    this
  }

  def withEmail(email: String): CreateAccountPayloadBuilder = {
    this.email = email
    this
  }

  def withPassword(password: String): CreateAccountPayloadBuilder = {
    this.password = password
    this
  }

  def withFirstName(name: String): CreateAccountPayloadBuilder = {
    this.first_name = name
    this
  }

  def withLastName(name: String): CreateAccountPayloadBuilder = {
    this.last_name = name
    this
  }

  def withPreferences(preferences: Seq[String]): CreateAccountPayloadBuilder = {
    this.preferences = preferences
    this
  }

}

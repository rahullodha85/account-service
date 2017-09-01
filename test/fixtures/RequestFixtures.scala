package fixtures

import org.joda.time.YearMonth
import play.api.libs.json.Json

trait RequestFixtures {

  val validResetPasswordPayload =
    Json.parse("""{
        "password" : "Password*1",
        "confirm_password" : "Password*1"
        }""")

  val validChangePasswordPayload =
    Json.parse("""{
        "old_password" : "account1",
        "new_password" : "Password!1",
        "confirm_password" : "Password!1"
        }""")

  val validRegisterPayload =
    Json.parse("""{
        "first_name" : " incredibles",
        "last_name" : " incredibles",
        "middle_name" : "",
        "saks_opt_status": "F",
        "off5th_opt_status": "T",
        "password" : "Incredibles@1",
        "canadian_customer": "F",
        "confirm_password" : "Incredibles@1",
        "password_hint" : "",
        "email" : "incredibles@incredibles.com"
        }""")

  val invalidRegisterPayload =
    Json.parse("""{
        "first_name" : " spaced name",
        "middle_name": "",
        "last_name" : "",
        "middle_name" : "",
        "password" : "saks",
        "canadian_customer" : "F",
        "confirm_password" : "Saks@124",
        "password_hint" : "",
        "email" : "abc235#abc235.com"
        }""")

  val validUpdateProfilePayload =
    Json.parse("""{
        "first_name" : "DDDDD ",
        "last_name" : "Ccccc ",
        "email" : "incredibles@incredibles.com"
        }""")

  val validAddressRequestPayload = Json.parse("""
      {
          "is_default": false,
          "address1": "253 vesey street",
          "address2": "",
          "city": "new york",
          "state": "NY",
          "zip": "10007",
          "country": "US",
          "address_type": "shipping",
          "title": "",
          "first_name": "incredibles",
          "middle_name": "I",
          "last_name": "incredibles",
          "phone": "2123334444",
          "company": "incredibles"
        }
                                              """)

  val validCreateAddressWithCanadianPostalCodePayload = Json.parse("""
      {
          "is_default": false,
          "address1": "253 vesey street",
          "address2": "",
          "city": "new york",
          "state": "ON",
          "zip": "m2h 0k8",
          "country": "CA",
          "address_type": "shipping",
          "title": "",
          "first_name": "incredibles",
          "middle_name": "I",
          "last_name": "incredibles",
          "phone": "2123334444",
          "company": "incredibles"
        }

                                                                    """)
  val createAddressWithInvalidCanadianPostalCodePayload = Json.parse("""
      {
          "is_default": false,
          "address1": "253 vesey street",
          "address2": "",
          "city": "new york",
          "state": "ON",
          "zip": "11210",
          "country": "CA",
          "address_type": "shipping",
          "title": "",
          "first_name": "incredibles",
          "middle_name": "I",
          "last_name": "incredibles",
          "phone": "2123334444",
          "company": "incredibles"
        }

                                                                      """)

  val invalidCreateAddressPayloadMissingFields = Json.parse("""
      {
          "address2": "",
          "city": "new york",
          "state": "NY",
          "zip": "10007",
          "country": "US",
          "address_type": "shipping",
          "title": "",
          "first_name": "incredibles",
          "middle_name": "I",
          "last_name": "incredibles",
          "phone": "2123334444",
          "company": "incredibles"
        }
                                                             """)

  val validCreateAddressPayloadMissingOpt = Json.parse("""
      {
          "is_default": false,
          "address_type": "shipping",
          "address1": "253 vesey street",
          "city": "new york",
          "zip": "10007",
          "country": "US",
          "title": "",
          "first_name": "incredibles",
          "middle_name": "I",
          "last_name": "incredibles",
          "phone": "2123334444"
        }
     """)

  def buildCardPayload(card: String, year: Integer, month: Integer) =
    Json.parse(s"""
      {
        "brand" : "$card",
        "id" : 50612324,
        "is_default" : false,
        "month" : $month,
        "name" : "MockValidSinglePaymentCMP",
        "number" : " 6011 000990911199",
        "year" : $year
      }
    """)

  val inValidSinglePaymentMethodRequestPayload =
    Json.parse("""
      {
        "is_default" : false,
        "name" : "MockValidSinglePaymentCMP"
      }
  """)

  val nonExpiredCreditCardSinglePaymentMethodRequestPayload = {
    val date = YearMonth.now()
    Json.obj(
      "brand" -> "DISC",
      "id" -> 506,
      "is_default" -> false,
      "month" -> date.getMonthOfYear,
      "name" -> "MockValidSinglePaymentCMP",
      "number" -> "6011000990911199",
      "year" -> date.getYear
    )
  }
}

package unit.model

import models.website.EmailPreferencesModel
import org.scalatest.{ ShouldMatchers, WordSpec }
import play.api.libs.json.Json

class EmailPreferencesModelSpec extends WordSpec with ShouldMatchers {

  "An  EmailPreferencesModel" should {
    "return user preferred options as a sequence of strings" in {
      EmailPreferencesModel().toResponsePayload should be(empty)

      val preferencesModel: EmailPreferencesModel = EmailPreferencesModel(off5th_opt_status = Some("T"), off5th_canada_opt_status = Some("F"))
      preferencesModel.toResponsePayload should be(Seq("off5th_opt_status"))

      Json.toJson(preferencesModel)
    }

    "parse from json" in {
      val preferencesModel: EmailPreferencesModel = Json.obj("saks_opt_status" -> "T", "opt_status" -> "F", "off5th_canada_opt_status" -> "F", "saks_canada_opt_status" -> "T").as[EmailPreferencesModel]

      preferencesModel.off5th_opt_status should be(Some("F"))
      preferencesModel.saks_opt_status should be(Some("T"))
      preferencesModel.saks_canada_opt_status should be(Some("T"))
      preferencesModel.off5th_canada_opt_status should be(Some("F"))
    }
  }
}

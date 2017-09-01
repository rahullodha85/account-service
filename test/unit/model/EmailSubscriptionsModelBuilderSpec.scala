package unit.model

import constants.{ Banners, Constants }
import models.website.{ AvailableOptions, EmailSubscriptionsModel, EmailSubscriptionsModelBuilder }
import org.scalatest.{ ShouldMatchers, WordSpec }

class EmailSubscriptionsModelBuilderSpec extends WordSpec with ShouldMatchers {

  "an email subscription model" can {
    "produce register option" should {
      "for lord and taylor" in {
        val expected = EmailSubscriptionsModel(
          Seq(Constants.LordAndTaylorOptStatus),
          Seq(AvailableOptions.lordAndTaylor)
        )
        val options = EmailSubscriptionsModelBuilder.buildRegisterOptions(Banners.LordAndTaylor)

        options should be(expected)
      }

      "for saks" in {
        val expected = EmailSubscriptionsModel(
          Seq(Constants.SaksOptStatus, Constants.Off5thOptStatus),
          Seq(AvailableOptions.saks, AvailableOptions.saksOff5th)
        )
        val options = EmailSubscriptionsModelBuilder.buildRegisterOptions(Banners.Saks)

        options should be(expected)
      }

      "for saks off fifth" in {
        val expected = EmailSubscriptionsModel(
          Seq(Constants.SaksOptStatus, Constants.Off5thOptStatus),
          Seq(AvailableOptions.saksOff5th, AvailableOptions.saks)
        )
        val options = EmailSubscriptionsModelBuilder.buildRegisterOptions(Banners.Off5th)

        options should be(expected)
      }
    }

    "produce gilt register option" should {
      "for saks off fifth" in {
        val expected = EmailSubscriptionsModel(
          Seq(Constants.SaksOptStatus, Constants.Off5thOptStatus, Constants.GiltOptStatus),
          Seq(AvailableOptions.saksOff5th, AvailableOptions.saks, AvailableOptions.gilt)
        )
        val options = EmailSubscriptionsModelBuilder.buildGiltRegisterOptions(Banners.Off5th)

        options should be(expected)
      }

      "for saks" in {
        val expected = EmailSubscriptionsModel(
          Seq(Constants.SaksOptStatus, Constants.Off5thOptStatus),
          Seq(AvailableOptions.saks, AvailableOptions.saksOff5th)
        )
        val options = EmailSubscriptionsModelBuilder.buildGiltRegisterOptions(Banners.Saks)

        options should be(expected)
      }
    }

    "produce update preferences option" should {
      "for saks off fifth" in {
        val preferences = Seq("preferences")
        val expected = EmailSubscriptionsModel(
          preferences,
          Seq(AvailableOptions.saksOff5th, AvailableOptions.saks, AvailableOptions.saksOff5thCanada, AvailableOptions.saksCanada)
        )
        val options = EmailSubscriptionsModelBuilder.buildUpdatePreferencesOptions(Banners.Off5th, preferences)

        options should be(expected)
      }

      "for saks" in {
        val preferences = Seq("preferences")
        val expected = EmailSubscriptionsModel(
          preferences,
          Seq(AvailableOptions.saks, AvailableOptions.saksOff5th, AvailableOptions.saksCanada, AvailableOptions.saksOff5thCanada)
        )
        val options = EmailSubscriptionsModelBuilder.buildUpdatePreferencesOptions(Banners.Saks, preferences)

        options should be(expected)
      }

      "for lord and taylor" in {
        val preferences = Seq("preferences")
        val expected = EmailSubscriptionsModel(
          preferences,
          Seq(AvailableOptions.lordAndTaylor)
        )
        val options = EmailSubscriptionsModelBuilder.buildUpdatePreferencesOptions(Banners.LordAndTaylor, preferences)

        options should be(expected)
      }
    }
  }

}

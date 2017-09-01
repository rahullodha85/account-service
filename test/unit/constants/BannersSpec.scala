package unit.constants

import constants.Banners
import org.scalatest.{ Matchers, WordSpec }

class BannersSpec extends WordSpec with Matchers {

  "Banners" can {
    "lord and taylor" should {
      "have lat banner" in {
        val banner = Banners("lat")

        banner should be(Banners.LordAndTaylor)
      }
    }

    "saks fifth avenue" should {
      "have s5a banner" in {
        val banner = Banners("s5a")

        banner should be(Banners.Saks)
      }
    }

    "saks off fifth" should {
      "have o5a banner" in {
        val banner = Banners("o5a")

        banner should be(Banners.Off5th)
      }
    }
  }
}

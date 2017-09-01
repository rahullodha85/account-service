package models.website

import play.api.libs.json.Json

case class SignOutWebsiteModel(
  links: HomePageLink
)

case class HomePageLink(
  home_page_link: String
)

object HomePageLink {
  implicit val homePageLinkFormat = Json.format[HomePageLink]
}

object SignOutWebsiteModel {
  implicit val signOutWebsiteModelFormat = Json.format[SignOutWebsiteModel]
}

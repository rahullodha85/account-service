package models.servicemodel

import play.api.libs.json.Json

case class UpdateAccountModel(
  user_id: String,
  profile: EditProfileModel
)

case class EditProfileModel(
  first_name: String,
  last_name:  String,
  email:      String
)

object EditProfileModel {
  implicit val profileModelFormat = Json.format[EditProfileModel]
}

object UpdateAccountModel {
  implicit val updateAccountModelFormat = Json.format[UpdateAccountModel]
}


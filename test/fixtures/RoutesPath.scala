package fixtures

trait RoutesPath {
  val route_getPaymentMethod = "/account-service/accounts/123/payment-methods"
  val route_updatePaymentMethod = "/account-service/accounts/123/payment-methods/567"
  val route_deletePaymentMethod = "/account-service/accounts/123/payment-methods/567"
  val route_createPaymentMethod = "/account-service/accounts/123/payment-methods"
  val route_getAddressBook = "/account-service/accounts/123/addresses?address_type=shipping"
  val route_createAddress = "/account-service/accounts/123/addresses"
  val route_updateDeleteAddress = "/account-service/accounts/123/addresses/100000?address_type=shipping"
}

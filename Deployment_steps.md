# Deployment Steps

In order for Account App to be deployed please follow these steps.

## Create toggles

In your environment create the following feature toggles:

- ENABLE_ACCOUNTS_API
- ACCOUNT_FULL_HTTP

Example environment: [http://qaslot10.saksdirect.com/v1/toggle-service/ui/togglesList](http://qaslot10.saksdirect.com/v1/toggle-service/ui/togglesList)

## Deploy microservices

List of required microservices:

- blue-martini-service
- account-service
- user-account-service
- more-account-service
- user-authentication-service
- address-service
- payment-method-service
- email-marketing-service
- order-service

Install `hbc-go` with good old npm:

```
npm install hbc-go -g
```

Now deploy account services:

```
hbc-go deploy devslot10 -t accounts
```

## Set up Apache rules

Create a `qaslot30-rewrite.inc` file in your environment with the following contents:

```
## Account Rewrites - BEGIN

  RequestHeader set NewAccountsOn "true" env=accounts

  RewriteCond %{HTTPS} !=on
  RewriteRule ^/v1/account-service/account/logout http://ms-qaslot30.digital.hbc.com:8080%{REQUEST_URI} [P,L]

  RewriteCond %{HTTPS} !=on
  RewriteRule ^/account https://%{HTTP_HOST}%{REQUEST_URI} [L,R=302]

  RewriteCond %{HTTPS} !=on
  RewriteRule ^/v1/account-service/(.*) https://%{HTTP_HOST}%{REQUEST_URI} [L,R=302]

  RewriteRule .* - [E=accounts:enable]
  RequestHeader set NewAccountsOn "true" env=accounts
  RewriteRule ^/v1/account-service https://ms-qaslot30.digital.hbc.com:8443%{REQUEST_URI} [P,L]

  #RewriteCond %{REMOTE_ADDR} 10.233.138.* [or]
  #RewriteCond %{REMOTE_ADDR} 10.233.138.140


  RewriteCond %{ENV:accounts} enable
  RewriteRule /account/order_history.jsp /account/order-history [L,R=301]

  RewriteCond %{ENV:accounts} enable
  RewriteRule /account/settings.jsp /account/settings [L,R=301]

  RewriteCond %{ENV:accounts} enable
  RewriteRule /account/mng_shipAddress.jsp /account/shipping-address-book [L,R=301]

  RewriteCond %{ENV:accounts} enable
  RewriteRule /account/mng_billAddress.jsp /account/billing-address-book [L,R=301]

  RewriteCond %{ENV:accounts} enable
  RewriteRule /account/mng_CreditCard.jsp /account/payment [L,R=301]

  RewriteCond %{ENV:accounts} enable
  RewriteRule /account/login_csr.jsp /account/login-csr [L,R=301]

  RewriteCond %{ENV:accounts} enable
  RewriteRule /account/login.jsp /account/login [L,R=301]

  RewriteCond %{ENV:accounts} enable
  RewriteRule /account/forgotpassword.jsp /account/forgot-password [L,R=301]

  RewriteCond %{ENV:accounts} enable
  RewriteRule /account/.*$ /account_aem.jsp  [PT,L]

## Account Rewrites - END
```
Then in your `3_website.conf` file include `qaslot30-rewrite.inc`:

```
<VirtualHost *:80>
    ...
    Include conf.d/qaslot30-rewrite.inc
    ...
</VirtualHost>
...
<VirtualHost *:443>
    ...
    Include conf.d/qaslot30-rewrite.inc
    ...
</VirtualHost>
```

**Note:** Make sure there are no intercepting rules before this include. For example in `seo.vars` file.

## Deploy Contact Center

Use appropriate pipeline for environment:
 - Contact_Center_Refresh
 - Off5th_QA_Contact_Center
 - Off5th_Production_Contact_Center

After deploying, be sure to restart the Contact Center process.

## Smoke Test Services

1. Login to the website either through old account. (If you face problem in opening old account page, comment out the last redirect and reload the cache configuration)
2. Open Following links(change the hostname according to the slot you are on ), and verify the result is of status code 200, scroll down to the end of json and verify that the error[] array is empty.
3. If you see errors in any of the links, the last section will tell you which services are not behaving correctly). At that point jump in to the logs and see if their is connectivity issues on https/hornetq.

List of new Account URLs:

- [https://qa.saksoff5th.com/account/login](https://www.off5th.com/account/login)
- [https://qa.saksoff5th.com/account/login-csr?site_refer=CSR124423432&_k=ymaf80](https://www.off5th.com/account/login-csr?site_refer=CSR124423432&_k=ymaf80)
- [https://qa.saksoff5th.com/account/register](https://www.off5th.com/account/register)
- [https://qa.saksoff5th.com/account/forgot-password](https://www.off5th.com/account/forgot-password)
- [https://www.off5th.com/account/reset-password?Loc=ek5:1ac0424c4eb027f2522e073fc5b34587035f623d3d122aa5920d77be39e1011a45ca6fd5fd44622ea1dfdef61fdcb138a8ed2b07e6d1c9ae67453462f0ae34ee&site_refer=EML5165TRIG_ACCT_NONE_NONE_NONE_0](https://www.off5th.com/account/reset-password?Loc=ek5:1ac0424c4eb027f2522e073fc5b34587035f623d3d122aa5920d77be39e1011a45ca6fd5fd44622ea1dfdef61fdcb138a8ed2b07e6d1c9ae67453462f0ae34ee&site_refer=EML5165TRIG_ACCT_NONE_NONE_NONE_0)
- [https://qa.saksoff5th.com/account/order-status?order_num=10000044&billing_zip_code=10038](https://www.off5th.com/account/order-status?order_num=10000044&billing_zip_code=10038)
- [https://qa.saksoff5th.com/account/summary](https://www.off5th.com/account/summary)
- [https://qa.saksoff5th.com/account/order-history](https://www.off5th.com/account/order-history)
- [https://qa.saksoff5th.com/account/shipping-address-book](https://www.off5th.com/account/shipping-address-book)
- [https://qa.saksoff5th.com/account/billing-address-book](https://www.off5th.com/account/billing-address-book)
- [https://qa.saksoff5th.com/account/payment](https://www.off5th.com/account/payment)
- [https://qa.saksoff5th.com/account/settings](https://www.off5th.com/account/settings)


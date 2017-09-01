#!/bin/sh
HOST=$(hostname --ip-address)
PORT=9805
consul-template -consul $CONSUL_HOST -template "/opt/account-service-0.1/conf/account-application.ctmpl:/opt/account-service-0.1/conf/account-application.conf" -once;
consul-template -consul $CONSUL_HOST -template "/opt/newrelic/newrelic.ctmpl:/opt/newrelic/newrelic.yml" -once

#!/bin/sh

echo "Tag (override if necessary) the account-service..."
sudo docker tag -f hbcdigital/service:account-service-0.1 hd1cutl01lx.saksdirect.com:5000/account-service-0.1

echo "Push account-service image to Internal Docker Registry..."
sudo docker push hd1cutl01lx.saksdirect.com:5000/account-service-0.1

echo "Completed pushing account-service image to Internal Docker Registry."

#!/usr/bin/env bash
echo "Starting the account-service on port 9000 ..."
docker run -d --name account-service -p 9000:9000 hbcdigital/service:account-service-0.1
echo "... done."

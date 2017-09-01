#!/usr/bin/env bash
echo "Stopping the account-service ..."
docker stop account-service; docker rm account-service
echo "... done."

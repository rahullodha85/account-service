#!/usr/bin/env bash

echo "Building Distribution ZIP for account-service..."
if ! [ -a ../target/universal/account-service-0.1.zip ]  ; then
	echo "Distribution ZIP not found, building from source..."
	cd ../ && sbt "project account-service" dist
	cd docker
fi

echo "Copying account-service ZIP to current directory..."
cp ../target/universal/account-service-0.1.zip .

tag=hbcdigital/service:account-service-0.1

echo "Building account-service Docker Container.."
sudo docker build -t ${tag} .

echo "Removing ZIP..."
rm account-service-0.1.zip

echo "Completed building image: $tag"


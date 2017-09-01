#!/bin/bash

echo 'Refreshing services'
vagrant ssh << EOF
cd /vagrant
sudo docker-compose stop currentservice
yes | sudo docker-compose rm
sudo docker-compose build currentservice
service docker-compose start currentservice
EOF
exit
echo 'Done'


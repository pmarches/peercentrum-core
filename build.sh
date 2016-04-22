#!/bin/bash
mvn clean package
sudo docker build -t pmarches/peercentrum .


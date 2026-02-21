#!/bin/bash

export JAVA_HOME=`/usr/libexec/java_home -v 21`

( cd ..; mvn clean package  )
cp ../target/l9g-uidgen.jar .
docker build -t l9g-uidgen:latest .

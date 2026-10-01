#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"

export JAVA_HOME=`/usr/libexec/java_home -v 25`

( cd ..; mvn clean package )
cp ../target/l9g-cardinfo.jar .
docker build -t l9g-cardinfo:latest .

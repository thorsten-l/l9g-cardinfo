#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"

if (( $# == 0 )); then
  echo "usage: $0 tag [tag ...]   e.g. $0 1.2.0 1.2 latest"
  exit 1
fi

export JAVA_HOME=`/usr/libexec/java_home -v 25`

( cd ..; mvn clean package )
cp ../target/l9g-cardinfo.jar .

TAGS=()

while (( $# )); do
  TAGS+=(--tag "ghcr.io/thorsten-l/l9g-cardinfo:$1")
  TAGS+=(--tag "tludewig/l9g-cardinfo:$1")
  shift
done

../private/LOGIN.sh

docker buildx build --progress plain --no-cache \
  --push \
  --platform linux/arm64,linux/amd64 "${TAGS[@]}" .

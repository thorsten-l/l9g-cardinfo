#!/bin/bash
set -euo pipefail

if (( $# != 1 )); then
  echo "usage: $0 \"clear text\""
  exit 1
fi

IMAGE="${IMAGE:-ghcr.io/thorsten-l/l9g-cardinfo:latest}"
DATA_DIR="$(cd "$(dirname "$0")/../data" && pwd)"

docker run --rm \
  -v "$DATA_DIR:/data:ro" \
  -it "$IMAGE" -e "$1"

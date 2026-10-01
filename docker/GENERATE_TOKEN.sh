#!/bin/bash
set -euo pipefail

IMAGE="${IMAGE:-ghcr.io/thorsten-l/l9g-cardinfo:latest}"
DATA_DIR="$(cd "$(dirname "$0")/../data" && pwd)"

docker run --rm \
  -v "$DATA_DIR:/data:ro" \
  -it "$IMAGE" -g

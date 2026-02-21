#!/bin/bash

docker run --rm \
  -v ../data:/data \
  -it ghcr.io/thorsten-l/l9g-uidgen:latest -i

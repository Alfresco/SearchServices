#!/usr/bin/env bash
set -eux

# Start Alfresco and Solr.

# The location for the docker-compose files.
DOCKER_RESOURCE_FOLDER=$1
# The search docker image.
SEARCH_IMAGE=$2

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export DOCKER_CLIENT_TIMEOUT=120
export COMPOSE_HTTP_TIMEOUT=120

# Build the images and call docker compose.
cd "$DOCKER_RESOURCE_FOLDER"
docker compose up -d --build --force-recreate

$SCRIPT_DIR/wait-service-to-start.sh

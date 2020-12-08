#!/bin/bash

set -euo pipefail

cd "$(dirname "$0")/.."

healthwait() {
  local id=$1
  local expected=${2:-undefined}
  local maxtry=${3:-25}
  local strict=${4:-1}
  local try=0

  until health=$(docker inspect "$id" --format='{{.State.Health.Status}}') && [ "$health" = "$expected" ] && docker inspect "$cid" --format='{{json .State.Health.Log}}' | jq -r '. as $raw | try (.[-1].Output | fromjson) catch $raw'; do
    let try+=1
    echo "container health: $health, expecting $expected"
    if [ "$strict" = 1 ] && [ "$health" != starting ]; then
      echo "FAIL: strict check will only permit the expected status '$expected' or 'starting'"
      return 1
    fi
    if [ $try -ge $maxtry ]; then
      echo "FAIL: Timed out waiting for $expected status"
      return 1
    fi
    sleep 1
  done
  echo "OK, container health: $health"
}

compose='docker-compose -f docker-compose.yml -f docker-compose-test.yml -f secore-template-test/docker-compose.yml'

$compose down --timeout 0 --volumes --remove-orphans

$compose up -d secore-template
cid=$($compose ps -q secore-template)

echo Check it goes straight to unhealthy status
healthwait "$cid" unhealthy

echo Check container reached wait point
awk '{print;if($0~/Waiting\./){FOUND=1;exit}}END{if(!FOUND){exit 1}}' <(timeout 60 docker logs -f "$cid")

echo Give it postgres and Kafka and check we can get a healthy container healthcheck
$compose up -d zookeeper kafka postgres

echo Wait for health status to change to healthy state
healthwait "$cid" healthy 60 0

echo Now run another secore-template instance but this time be strict as postgres and Kafka should already be ready so it should go straight to healthy state
healthwait "$($compose run -dT --rm secore-template wait)" healthy

echo Now run another secore-template instance but this time with a startup delay which should stop it returning a healthy state until the app has started even though postgres and Kafka are fine
cid=$(START_DELAY=75000 $compose run -dT --rm secore-template wait)
if healthwait "$cid" healthy 60 0; then
  echo "This should not have returned healthy, application startup was delayed 75 seconds and we only waited 60"
  exit 1
fi
echo "Should become healthy in the next 15-30 seconds..."
healthwait "$cid" healthy 30 0

#!/bin/sh
#
# Boot script for microservices
#

echo "Starting $SERVICE: $0"

HEAPDUMPDIR=${HEAPDUMPDIR:-'/tmp'}

export DS_URL=${DS_URL:-jdbc:postgresql://postgres:5432/tagging}
export DS_USERNAME=${DS_USERNAME:-developer}
export DS_PASSWORD=${DS_PASSWORD:-developer}
export KAFKA=${KAFKA:-kafka:9092}

export IDP=${IDP:-'http://idp:8080/auth'}

echo "Starting $SERVICE with environment:"
env | grep -v ^DS_PASSWORD
echo

echo "Starting $SERVICE with command:"
set -x
exec java -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$HEAPDUMPDIR -Xms128M -Xmx256M $JAVA_OPTS -jar "$@" "$APPLICATION_WAR"

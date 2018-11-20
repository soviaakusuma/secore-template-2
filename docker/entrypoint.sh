#!/bin/bash
#
# Boot script for microservices
#

set -e

echo "Starting $SERVICE: $0"

HEAPDUMPDIR=${HEAPDUMPDIR:-'/tmp'}

export DS_URL=${DS_URL:-jdbc:postgresql://postgres:5432/$SERVICE}
export DS_USERNAME=${DS_USERNAME:-developer}
export DS_PASSWORD=${DS_PASSWORD:-developer}
export KAFKA=${KAFKA:-kafka:9092}
export IDP=${IDP:-'http://idp:8080/auth'}

if [ "$VERBOSE" = true ]; then
  echo "Starting $SERVICE with environment:"
  # Don't show passwords in the logs. This is case insensitive.
  env | sort | sed 's/^\(.*PASSWORD.*\)=.*$/\1=(hidden)/i'
  echo
fi

if [ -f /etc/grow/GROW/com.inomial/grow/bin/grow.sh ]
then
  echo "Upgrading the database schema via grow..."
  su-exec ${GROW_UID:-500}:${GROW_GID:-500} /etc/grow/GROW/com.inomial/grow/bin/grow.sh --dir /etc/grow
fi
if [ "$GROW" = init ]; then
  echo '$GROW=init, exiting after running grow'
  exit
fi

start="exec su-exec ${UID:-1000}:${GID:-1000}"
if ! [ "$($start id -u)" -gt 0 -a "$($start id -g)" -gt 0 ]; then
  echo "UID or GID of 0 not permitted." >&2
  exit 1
fi

echo "Starting $SERVICE with command:"
set -x
$start java \
       -XX:+ExitOnOutOfMemoryError \
       -XX:+HeapDumpOnOutOfMemoryError \
       -XX:HeapDumpPath=$HEAPDUMPDIR \
       -Xms128M \
       -Xmx256M \
       $JAVA_OPTS \
       -jar $APPLICATION_JAR_PATH/$APPLICATION_JAR "$@"

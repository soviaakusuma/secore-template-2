#!/bin/sh
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

# Don't show passwords in the logs. This is case insensitive.
env | sort | sed 's/^\(.*PASSWORD.*\)=.*$/\1=(hidden)/i'

if [ -f /etc/grow/GROW/com.inomial/grow/bin/grow.sh ]
then
  echo "Upgrading the database schema via grow..."
  bash /etc/grow/GROW/com.inomial/grow/bin/grow.sh --dir /etc/grow
fi

echo "Starting $SERVICE with command:"
set -x
exec java \
       -XX:+ExitOnOutOfMemoryError \
       -XX:+HeapDumpOnOutOfMemoryError \
       -XX:HeapDumpPath=$HEAPDUMPDIR \
       -Xms128M \
       -Xmx256M \
       $JAVA_OPTS \
       -jar $APPLICATION_JAR_PATH/$APPLICATION_JAR "$@" 

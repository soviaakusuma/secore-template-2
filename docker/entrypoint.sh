#!/bin/sh -e
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

# Uncomment grow invocation here if the microservice uses a database.
#echo "Upgrading the database schema via grow..."
#bash /etc/grow/GROW/com.inomial/grow/bin/grow.sh --dir /etc/grow

echo "Starting $SERVICE with environment:"
# Need to avoid printing any passwords to the Docker logs, but make it clear that the variables are set.
env | sed '/^[_0-9A-Za-z]*[Pp][Aa][Ss][Ss][Ww][Oo][Rr][Dd][_0-9A-Za-z]*=/ s/=.*$/=<redacted>/'
echo

echo "Starting $SERVICE with command:"
set -x
exec java -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$HEAPDUMPDIR -Xms128M -Xmx256M $JAVA_OPTS -jar "$@" "$APPLICATION_JAR"

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

# If your microservice maintains a database schema, you'll need to uncomment the following lines
# so that Grow can manage your database objects.
# (n.b. the need to detect for greenfields installs will be redundant once "--grow auto" support
# is properly implemented in grow).

#echo "Invoking grow (check for/apply database schema updates)"
#postgres_conninfo_url=${DS_URL#jdbc:}
#export PGUSER=$DS_USERNAME
#export PGPASSWORD=$DS_PASSWORD
#growdir=`mktemp -d`
#(
#  # Need to run this in a subshell due to having to change the current directory.
#  # The inomial.io/openjdk8-jre Docker base image lacks the jar(1) command, but
#  # has the BusyBox version of unzip(1), so we can use that to unpack the GROW script
#  # from the application .jar.
#  unzip -q -d $growdir $APPLICATION_JAR 'GROW/*'
#  cd $growdir
#
#  # Check if we're doing a regular upgrade or greenfields installation (installing on a blank DB)
#  is_greenfields_install=`psql -d $postgres_conninfo_url -qAtc "select to_regnamespace('grow') is null;"`
#  if [ "$is_greenfields_install" = t ]; then
#    # We're installing to a blank database, so grow must be invoked in "init" mode
#    echo "Greenfields install; grow will be run in 'init' mode for this startup to initialise the DB."
#    grow_mode=init
#  else
#    # Grow is doing a regular update on an existing grow-managed DB
#    grow_mode=upgrade
#  fi
#  # Currently the grow.sh script doesn't have the executable bit set, so we have to manually invoke it with bash.
#  bash ./GROW/com.inomial/grow/bin/grow.sh --grow $grow_mode
#)
#rm -rf $growdir

echo "Starting $SERVICE with environment:"
# Need to avoid printing any passwords to the Docker logs, but make it clear that the variables are set.
env | sed '/^[_0-9A-Za-z]*[Pp][Aa][Ss][Ss][Ww][Oo][Rr][Dd][_0-9A-Za-z]*=/ s/=.*$/=<redacted>/'
echo

echo "Starting $SERVICE with command:"
set -x
exec java -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$HEAPDUMPDIR -Xms128M -Xmx256M $JAVA_OPTS -jar "$@" "$APPLICATION_JAR"

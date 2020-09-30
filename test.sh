#!/bin/bash

set -ex

cd "$(dirname "$0")"

if [ -e build/version.properties ]; then
  . build/version.properties
  app=${app:-$project}
else
  app=${app:-$(basename "`pwd`")}
fi

docker_compose_opts="--project-name=${app}-test -f docker-compose.yml -f docker-compose-test.yml"

# cleanup anything that could be left behind from a previous run
docker-compose $docker_compose_opts --no-ansi down --volumes --remove-orphans

return_status() {
  status=$?
  { set +x; } 2>/dev/null

  # if running on Jenkins then archive the logs + db and destroy the environment
  if [ "${BUILD_URL:-}" ]; then
    mkdir -p test-results
    docker-compose $docker_compose_opts logs --no-color --timestamps > test-results/docker-compose.log 2>&1
    docker-compose $docker_compose_opts run --rm -T testsql sh -c 'pg_dump -Fc $TEST_DB' > test-results/test.pgc
    docker-compose $docker_compose_opts --no-ansi down --volumes --remove-orphans
  fi

  if [ $status -ne 0 ] && [ "`uname`" = Darwin ]; then
    echo
    echo "Run the following to connect to the test database:"
    echo "  docker-compose $docker_compose_opts run --rm testsql psql $(. .env && echo $APP_POSTGRES_DATABASE)"
  fi

  exit $status
}
trap return_status EXIT

# start postgres
docker-compose $docker_compose_opts --no-ansi up -d postgres

timeout=`which timeout` || true
timeout() {
  if [ "$timeout" ]; then
    $timeout "$@"
  else
    shift
    "$@"
  fi
}

# wait for postgres to ready up
timeout 60 docker-compose $docker_compose_opts exec -Tu postgres postgres sh -c "until psql -h 127.0.0.1 -U \"\$POSTGRES_USER\" \"\$POSTGRES_DB\" -ec '\q'; do echo 'Postgres is unavailable - sleeping'; sleep 1; done" </dev/null

# start container with GROW=init and wait for it to exit (start attached with timeout)
if [ "$1" = upgrade ] && [ ! "$INITVERSION" ]; then
  INITVERSION=$(git tag -n | grep -E '^[2-9][0-9]\.(0[1-9]|1[0-2])\.[1-9]([0-9]+)?\s+Jenkins build from master commit ' | sort -V | tail -n1 | awk '{print$1}')
fi
if [ "$INITVERSION" ]; then
  echo "Running Grow init with $INITVERSION"
fi
VERSION=${INITVERSION:-latest} GROW=init timeout 60 docker-compose $docker_compose_opts --no-ansi up --exit-code-from $app $app
if [ "$INITVERSION" ]; then
  expectedVer=$(awk -F. '{printf "%.2d%.2d%.3d\n", $1, $2, $3}' <<<"$INITVERSION")
  dbVer=$(docker-compose $docker_compose_opts run -T --rm testsql psql "$(. .env && echo $APP_POSTGRES_DATABASE)" -qAtc 'select version from grow.log')
  if [ "$expectedVer" != "$dbVer" ]; then
    echo "Expected Grow version $expectedVer from $INITVERSION but got $dbVer"
    exit 1
  fi
fi

# run sql tests
TIMEOUT=300
COMPOSE_HTTP_TIMEOUT=$(($TIMEOUT*2)) timeout $TIMEOUT docker-compose $docker_compose_opts --no-ansi up --exit-code-from testsql testsql

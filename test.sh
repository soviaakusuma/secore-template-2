#!/bin/bash

set -ex

cd "$(dirname "$0")"

if [ -e build/version.properties ]; then
  . build/version.properties
  app=${app:-$project}
else
  app=${app:-$(basename "`pwd`")}
fi

export COMPOSE_PROJECT_NAME=${app}-test

docker_compose_opts='-f docker-compose.yml -f docker-compose-test.yml'

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
timeout 60 docker-compose $docker_compose_opts exec -Tu postgres postgres sh -c "until psql -h 127.0.0.1 -U \"\$POSTGRES_USER\" \"\$POSTGRES_DB\" -ec '\q'; do echo 'Postgres is unavailable - sleeping'; sleep 1; done"

# start container with GROW=init and wait for it to exit (start attached with timeout)
GROW=init timeout 60 docker-compose $docker_compose_opts --no-ansi up --exit-code-from $app $app

# run sql tests
timeout 300 docker-compose $docker_compose_opts --no-ansi up --exit-code-from testsql testsql

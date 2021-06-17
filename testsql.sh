#!/bin/bash

# By default this script will look for sql tests under src/test/sql, you can override by providing a path as a parameter.
# Example: 'testsql.sh git/otherproject' will run tests in $PWD/git/otherproject/src/test/sql

finish() {
  status=$?

  if [ -e "$grow_dir" ]; then
    rm -rf "$grow_dir"
  fi

  echo
  if [ "${#sqltests[@]}" != 0 ]; then
    echo "PASSED: ${#passed[@]}/${#sqltests[@]}"
    [ "${passed[*]}" ] && printf "%s\n" "${passed[@]}" | cat -n
    echo "FAILED: ${#failed[@]}/${#sqltests[@]}" >&2
    [ "${failed[*]}" ] && printf "%s\n" "${failed[@]}" | cat -n >&2
    echo
  fi

  if [ $status -ne 0 ]; then
    echo SCRIPT ERROR OCCURRED >&2
    exit 1
  fi

  if [ ${#passed[@]} -eq ${#sqltests[@]} ]; then
    echo PASSED
    exit 0
  else
    echo FAILED >&2
    exit 1
  fi
}

set -e

trap finish EXIT

dirname=$(dirname "$0")

if [ "$1" ]; then
  testdir="$1/src/test/sql"
else
  testdir="src/test/sql"
fi

if [ -d "$testdir" ]; then
  # cd into sql dir so .sql files can source other files by using a relative path to the sourcing .sql
  cd "$testdir"
else
  echo "Directory '$testdir' not found, aborting." >&2
  exit 1
fi

gradle_settings=../../../settings.gradle
if [ -e "$gradle_settings" ]; then
  project=`expr "$(grep "^\s*rootProject\.name\s*=" "$gradle_settings")" : "[^=]*= *['\"]\([^']*\)"`
else
  project=`basename "$(cd "$dirname" && pwd)"`
fi
export PGDATABASE=${TEST_DB:-$project}

if [ "`which java`" ] && [ ../../../gradlew ]; then
  ../../../gradlew build
elif [ "`which gradle`" ]; then
  (cd ../../.. && gradle build)
fi
if [ -e ../../../build/version.properties ]; then
  . "../../../build/version.properties"
  for grow_jar in ../../../build/libs/$project-$version-grow.jar ../../../build/libs/$project-$version.jar; do
    [ -e "$grow_jar" ] && break
  done
else
  grow_jar=$(ls -1t ../../../build/libs/$project-*-grow.jar|head -n1)
fi

if [ ! -e "$grow_jar" ]; then
  echo "grow_jar '$grow_jar' not found, aborting." >&2
  exit 1
fi

export PGOPTIONS='--client-min-messages=warning'

psqlfile() {
  psql -X -q -a -1 -v ON_ERROR_STOP=1 --pset pager=off -f "$1"
}

printline() {
  printf '%*s\n' "${COLUMNS:-150}" '' | tr ' ' -
}

sqltests=( $(find . -type f -name '*.sql' -and ! -path './init.sql' | sort) )

if [ "$sqltests" ]; then
  echo -n "Checking database connection... "
  if ! psql -qAtc "select 'OK'" </dev/null; then
    echo "There was a problem connecting to the database, you may need to set the PGHOST, PGUSER, PGPORT or PGPASSWORD environment variables" >&2
    exit 1
  fi

  # setup temp dir to unpack grow jar into and determine the absolute path of the grow jar (in case it is relative) so it can be found after changing into the new temp dir
  grow_dir=$(mktemp -d)
  orig_grow_jar=$grow_jar
  if [ "`which python`" ]; then
    # for Mac
    grow_jar=$(python -c 'import sys; import os.path; print os.path.realpath(os.path.abspath(sys.argv[1]))' "$grow_jar")
  else
    # for Linux
    grow_jar=$(readlink -f "$grow_jar")
  fi
  if [ ! "$grow_jar" ]; then
    echo "Failed to determine absolute path of $orig_grow_jar" >&2
    exit 1
  fi

  cd "$grow_dir"

  echo "Unpacking grow ($grow_jar)..."
  if [ "`which jar`" ]; then
    jar -xf "$grow_jar" GROW
  elif [ "`which unzip`" ]; then
    unzip -q "$grow_jar" 'GROW/*'
  else
    echo "jar or unzip tool required to unpack $grow_jar but is not present" >&2
    exit 1
  fi

  echo "Running grow..."
  grow_bin="$grow_dir/GROW/com.inomial/grow/bin/grow.sh"
  chmod +x "$grow_bin"
  DS_USERNAME=$PGUSER DS_PASSWORD=$PGPASSWORD "$grow_bin" --url "jdbc:postgresql://${PGHOST:-localhost}:${PGPORT:-5432}/$PGDATABASE" --grow auto

  export grow_dir
  cd -

  initsql="init.sql"
  if [ -f "$initsql" ]; then
    echo "Running $initsql..."
    psqlfile "$initsql"
  fi

  echo "Running ${#sqltests[@]} tests..."
  for test in "${sqltests[@]}"; do
    printline
    echo "Running $test:"
    echo
    if psqlfile "$test"; then
      passed+=( "$test" )
      echo "PASSED: $test"
    else
      failed+=( "$test" )
      echo "FAILED: $test" >&2
    fi
  done
  printline
else
  echo 'No tests found, aborting' >&2
  exit 1
fi

exit 0

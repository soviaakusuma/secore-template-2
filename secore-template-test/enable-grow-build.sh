#!/bin/bash

set -euo pipefail

cd "$(dirname "$0")/.."

sed=sed
if [ "$(uname)" = Darwin ]; then
  sed=gsed
fi

$sed -i "s|^//\( *dependsOn 'growJar'\)|\1|" build.gradle
$sed -i "s|//\( *runtimeOnly 'com.inomial:sql-core:.*\)|\1|" build.gradle
$sed -i "s|//\( *runtimeOnly 'com.inomial:sql-test:.*\)|\1|" build.gradle
mkdir -pv src/main/grow
schema=tpltest
echo $schema >> src/main/grow/schema.grow
echo PACKAGE=com.inomial.secore.template > src/main/grow/package.grow

mkdir src/main/grow/objects
cat <<EOF >>src/main/grow/objects/9999_func_tenant_init.sql
create or replace function $schema.tenant_init(_tid integer) returns int language plpgsql as \$\$
  begin
    return _tid;
  end;
\$\$;
EOF

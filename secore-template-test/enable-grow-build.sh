#!/bin/bash

set -euo pipefail

cd "$(dirname "$0")/.."

sed -i "s|^//\( *dependsOn 'growJar'\)|\1|" build.gradle
sed -i "s|//\( *runtimeOnly 'com.inomial:sql-core:.*\)|\1|" build.gradle
mkdir -pv src/main/grow
echo test > src/main/grow/schema.grow
echo PACKAGE=com.inomial.secore.template > src/main/grow/package.grow

#!/bin/bash

set -exuo pipefail

cd "$(dirname "$0")/.."

sed=sed
if [ "$(uname)" = Darwin ]; then
  sed=gsed
fi

# enable healthchecks and start in non-Consul mode
rm src/main/java/com/inomial/secore/template/ConsulApplication.java
rm src/main/java/com/inomial/secore/template/Consul.java
$sed -e "/Option 2: when use default values to initialize/,/delete the following line and start your/ { //! s/^\/\/// }" \
     -e 's/.*ConsulApplication().start().*/\/\/&\
            System.out.println("sleep start");\
            Thread.sleep(Integer.parseInt(System.getenv("START_DELAY")));\
            System.out.println("sleep end");/' \
     -e 's|^\s*health\.addManagedConsumerHealthcheck|//&|' \
     -i.orig src/main/java/com/inomial/secore/template/Main.java

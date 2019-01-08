#
# secore template dockerfile
#

FROM inomial.io/secore-base

ARG project
ARG version

ENV SERVICE $project
ENV VERSION $version
# ENV UID 1000
# ENV GID 1000
ENV APPLICATION_JAR_PATH "/opt/inomial.com/$project/lib"
ENV APPLICATION_JAR "$project-$version.jar"

RUN addgroup -g $GID $project \
 && adduser -D -h /data -u $UID -G $project $project

COPY lib $APPLICATION_JAR_PATH
COPY $APPLICATION_JAR $APPLICATION_JAR_PATH
COPY GROW /opt/inomial.com/GROW

# VOLUME /data

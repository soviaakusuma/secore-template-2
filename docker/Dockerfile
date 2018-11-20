#
# secore template dockerfile
#

FROM inomial.io/openjdk8-jre

ARG project
ARG version

ENV SERVICE $project
ENV UID 1000
ENV GID 1000
ENV GROW_UID 500
ENV GROW_GID 500
ENV APPLICATION_JAR_PATH "/opt/inomial.com/$project/lib"
ENV APPLICATION_JAR "$project-$version.jar"

RUN adduser -D -h /data -u $UID $project \
 && adduser -D -h /etc/grow -u $GROW_UID grow \
 && apk --no-cache add su-exec

COPY lib $APPLICATION_JAR_PATH
COPY entrypoint.sh /opt/inomial.com/
COPY $APPLICATION_JAR $APPLICATION_JAR_PATH
COPY GROW /etc/grow/GROW

ENTRYPOINT [ "/opt/inomial.com/entrypoint.sh" ]

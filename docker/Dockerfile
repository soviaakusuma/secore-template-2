#
# secore template dockerfile
#

FROM inomial.io/openjdk8-jre
ENV SERVICE=secore-template
ARG version
RUN adduser -D -u 1000 $SERVICE
# RUN apk update && apk add libc6-compat
ENV APPLICATION_JAR "/opt/inomial.com/$SERVICE/$SERVICE-$version-all.jar"
COPY $SERVICE-$version-all.jar $APPLICATION_JAR
COPY entrypoint.sh /opt/inomial.com/

USER 1000
ENTRYPOINT [ "/opt/inomial.com/entrypoint.sh" ]

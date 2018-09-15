#
# secore template dockerfile
#

FROM inomial.io/openjdk8-jre
ENV SERVICE=secore-template
ARG version
RUN adduser -D -u 1000 $SERVICE
ENV APPLICATION_JAR "/opt/inomial.com/$SERVICE/$SERVICE-$version-all.jar"
COPY $SERVICE-$version-all.jar $APPLICATION_JAR
##GROW COPY GROW /etc/grow/GROW
COPY entrypoint.sh /opt/inomial.com/

USER 1000
ENTRYPOINT [ "/opt/inomial.com/entrypoint.sh" ]

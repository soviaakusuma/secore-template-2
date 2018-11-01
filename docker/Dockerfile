#
# secore template dockerfile
#

FROM inomial.io/openjdk8-jre
ARG SERVICE
ARG version
ENV APPLICATION_JAR_PATH "/opt/inomial.com/$SERVICE/lib"
ENV MAIN_CLASS "com.inomial.$SERVICE.Main"
RUN adduser -D -u 1000 $SERVICE

COPY lib $APPLICATION_JAR_PATH
COPY entrypoint.sh /opt/inomial.com/
COPY $SERVICE-$version.jar $APPLICATION_JAR_PATH
##GROW COPY GROW /etc/grow/GROW

USER 1000
ENTRYPOINT [ "/opt/inomial.com/entrypoint.sh" ]

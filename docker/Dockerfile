#
# secore template dockerfile
#

FROM inomial.io/openjdk8-jre
ARG project
ARG version
ENV APPLICATION_JAR_PATH "/opt/inomial.com/$project/lib"
ENV APPLICATION_JAR "$project-$version.jar"
RUN adduser -D -u 1000 $project

COPY lib $APPLICATION_JAR_PATH
COPY entrypoint.sh /opt/inomial.com/
COPY $APPLICATION_JAR $APPLICATION_JAR_PATH
COPY GROW /etc/grow/GROW

USER 1000
ENTRYPOINT [ "/opt/inomial.com/entrypoint.sh" ]

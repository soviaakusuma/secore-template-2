#
# secore template dockerfile
#

FROM inomial.io/secore-base

ARG project
ARG version
ARG BUILD_DATE
ARG BUILD_VERSION
ARG VCS_URL
ARG VCS_REF

ENV SERVICE $project
ENV VERSION $BUILD_VERSION

LABEL org.label-schema.schema-version="1.0"
LABEL org.label-schema.build-date=$BUILD_DATE
LABEL org.label-schema.name=$project
SET_THIS_LINE LABEL org.label-schema.description="<description of this micro-service>"
LABEL org.label-schema.vcs-url=$VCS_URL
LABEL org.label-schema.vcs-ref=$VCS_REF
LABEL org.label-schema.vendor="DGIT Systems"
LABEL org.label-schema.version=$BUILD_VERSION

# Override the default database name which would otherwise match the SERVICE (project) name if DS_URL is not set
# For example, if you want turnstile-server to default to using the database called turnstile then set:
# ENV DS_DATABASE turnstile

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

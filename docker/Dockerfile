#
# secore template dockerfile
#

FROM inomial.io/secore-base

ARG project
ARG version
ARG description
ARG BUILD_DATE
ARG BUILD_VERSION
ARG VCS_URL
ARG VCS_REF

ENV SERVICE $project
ENV VERSION $BUILD_VERSION

# Override the default database name which would otherwise match the SERVICE (project) name if DS_URL is not set
# For example, if you want turnstile-server to default to using the database called turnstile then set:
# ENV DS_DATABASE turnstile

# If QL needs to access the database of this service then you can set the following to trigger the fix-roles 
# component in grow to grant the readonly role to the ql@ql role (this can be a space delimited list).
# ENV GROW_ROLE_MEMBERS_RO ql@ql

# ENV UID 1000
# ENV GID 1000
ENV APPLICATION_JAR_PATH "/opt/inomial.com/$project/lib"
ENV APPLICATION_JAR "$project-$version.jar"

COPY lib $APPLICATION_JAR_PATH
COPY $APPLICATION_JAR $APPLICATION_JAR_PATH
COPY GROW /opt/inomial.com/GROW

RUN addgroup -g $GID $project \
 && adduser -D -h /data -u $UID -G $project $project \
 && apk --no-cache upgrade

# VOLUME /data

# Because this command uses $BUILD_DATE it will always invalidate the cache - keep at the bottom
# https://github.com/opencontainers/image-spec/blob/master/annotations.md
LABEL org.opencontainers.image.created=$BUILD_DATE \
      org.opencontainers.image.title=$project \
      org.opencontainers.image.description=$description \
      org.opencontainers.image.source=$VCS_URL \
      org.opencontainers.image.revision=$VCS_REF \
      org.opencontainers.image.vendor="DGIT Systems" \
      org.opencontainers.image.version=$BUILD_VERSION

HEALTHCHECK --interval=5s --timeout=3s --start-period=10s --retries=3 CMD curl -sSf http://localhost:7070/health/ || exit 1

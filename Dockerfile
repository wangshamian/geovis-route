# Image is reused in the workflow builds for master and the latest version
FROM docker.io/maven:3.9.10-amazoncorretto-21-alpine AS build
ARG DEBIAN_FRONTEND=noninteractive

# hadolint ignore=DL3002
USER root

WORKDIR /tmp/ors

COPY gr-api/pom.xml /tmp/ors/gr-api/pom.xml
COPY gr-engine/pom.xml /tmp/ors/gr-engine/pom.xml
COPY pom.xml /tmp/ors/pom.xml
COPY gr-report-aggregation/pom.xml /tmp/ors/gr-report-aggregation/pom.xml
COPY gr-test-scenarios/pom.xml /tmp/ors/gr-test-scenarios/pom.xml
COPY gr-benchmark/pom.xml /tmp/ors/gr-benchmark/pom.xml
COPY mvnw /tmp/ors/mvnw
COPY .mvn /tmp/ors/.mvn

# Build the project
RUN ./mvnw -pl 'gr-api,gr-engine' -q dependency:go-offline

COPY gr-api /tmp/ors/gr-api
COPY gr-engine /tmp/ors/gr-engine

# Build the project
RUN ./mvnw -pl 'gr-api,gr-engine' \
    -q clean package -DskipTests -Dmaven.test.skip=true

FROM docker.io/golang:1.24.2-alpine3.21 AS build-go
# Setup the target system with the right user and folders.
RUN GO111MODULE=on go install github.com/mikefarah/yq/v4@v4.45.1

# build final image, just copying stuff inside
FROM docker.io/amazoncorretto:21.0.8-alpine3.21 AS publish

# Build ARGS
ARG UID=1000
ARG GID=1000
ARG OSM_FILE=./gr-api/src/test/files/heidelberg.test.pbf
ARG ORS_HOME=/home/ors

# Set the default language
ENV LANG='en_US' LANGUAGE='en_US' LC_ALL='en_US'

# Setup the target system with the right user and folders.
RUN apk update && apk add --no-cache bash=~5 jq=~1 openssl=~3 && \
    addgroup ors -g ${GID} && \
    mkdir -p ${ORS_HOME}/logs ${ORS_HOME}/files ${ORS_HOME}/graphs ${ORS_HOME}/elevation_cache  && \
    adduser -D -h ${ORS_HOME} -u ${UID} --system -G ors ors  && \
    chown ors:ors ${ORS_HOME} \
    # Give all permissions to the user
    && chmod -R 777 ${ORS_HOME}

# Copy over the needed bits and pieces from the other stages.
COPY --chown=ors:ors --from=build /tmp/ors/gr-api/target/ors.jar /ors.jar
COPY --chown=ors:ors ./$OSM_FILE /heidelberg.test.pbf
COPY --chown=ors:ors ./docker-entrypoint.sh /entrypoint.sh
COPY --chown=ors:ors --from=build-go /go/bin/yq /bin/yq

# Copy the example config files to the build folder
COPY --chown=ors:ors ./gr-config.yml /example-gr-config.yml
COPY --chown=ors:ors ./gr-config.env /example-gr-config.env

# Rewrite the example config to use the right files in the container
RUN yq -i -p=props -o=props \
    '.ors.engine.profile_default.build.source_file="/home/ors/files/example-heidelberg.test.pbf"' \
    /example-gr-config.env && \
    yq -i e '.ors.engine.profile_default.build.source_file = "/home/ors/files/example-heidelberg.test.pbf"' \
    /example-gr-config.yml

ENV BUILD_GRAPHS="False"
ENV REBUILD_GRAPHS="False"
# Set the ARG to an ENV. Else it will be lost.
ENV ORS_HOME=${ORS_HOME}

WORKDIR ${ORS_HOME}

# Healthcheck
HEALTHCHECK --interval=3s --timeout=2s CMD ["sh", "-c", "wget --quiet --tries=1 --spider http://localhost:8082/ors/v2/health || exit 1"]

# Start the container
ENTRYPOINT ["/entrypoint.sh"]

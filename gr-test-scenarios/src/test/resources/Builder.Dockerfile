# The reason for a separate builder dockerfile is to avoid the need to build the image every time a container is started.
# Testcontainers modifies the dockerfiles when something down the road changes from the beginning on, resulting in many unnecessary rebuilds.
# The builders in this file the the execution container are very heavy and it's better to separate them.
# Keeping them separate makes it impossible for testcontainers to change the build context for the builder images.
# Look into the documentation under docs/technical-details/integration-tests.md for more information.

ARG CONTAINER_BUILD_DIR=/build
ARG CONTAINER_WORK_DIR=/home/ors/openrouteservice

FROM docker.io/maven:3.9.9-amazoncorretto-21-alpine AS gr-test-scenarios-builder

RUN apk add --no-cache bash~=5 yq~=4 zip~=3 && \
    rm -rf /var/cache/apk/*

ARG CONTAINER_BUILD_DIR

# Set the working directory
WORKDIR "$CONTAINER_BUILD_DIR"

# Copy pom.xml files
COPY pom.xml $CONTAINER_BUILD_DIR/pom.xml
COPY gr-api/pom.xml $CONTAINER_BUILD_DIR/gr-api/pom.xml
COPY gr-engine/pom.xml $CONTAINER_BUILD_DIR/gr-engine/pom.xml
COPY gr-report-aggregation/pom.xml $CONTAINER_BUILD_DIR/gr-report-aggregation/pom.xml
COPY gr-test-scenarios/pom.xml $CONTAINER_BUILD_DIR/gr-test-scenarios/pom.xml
COPY gr-benchmark/pom.xml $CONTAINER_BUILD_DIR/gr-benchmark/pom.xml
COPY mvnw $CONTAINER_BUILD_DIR/mvnw
COPY .mvn $CONTAINER_BUILD_DIR/.mvn

# Cache the dependencies to speed up the build process
RUN ./mvnw dependency:go-offline -B -q

# Copy project files
COPY gr-api "$CONTAINER_BUILD_DIR"/gr-api
COPY gr-engine "$CONTAINER_BUILD_DIR"/gr-engine
COPY gr-report-aggregation "$CONTAINER_BUILD_DIR"/gr-report-aggregation

# Build the projects war and jar files
RUN ./mvnw clean package -q -DskipTests -Dmaven.test.skip=true -PbuildWar -pl \
    '!:gr-test-scenarios,!:gr-report-aggregation,!:gr-benchmark' && \
    ./mvnw package install -q -DskipTests -Dmaven.test.skip=true -PbuildJar -pl \
    '!:gr-test-scenarios,!:gr-report-aggregation,!:gr-benchmark'

# Prepare the config file
COPY gr-config.yml "$CONTAINER_BUILD_DIR"/gr-config.yml

RUN yq -i '\
    .server.port = 8080 | \
    .logging.file.name = "/home/ors/openrouteservice/logs/ors.log" | \
    .logging.level.org.heigit = "INFO" | \
    .ors.engine.graphs_data_access = "MMAP" | \
    .ors.engine.elevation.profile_default.build.elevation = false | \
    .ors.engine.profile_default.graph_path = "/home/ors/openrouteservice/graphs" | \
    .ors.engine.profiles.public-transport.gtfs_file = "/home/ors/openrouteservice/files/vrn_gtfs_cut.zip" | \
    .ors.engine.profile_default.build.source_file = "/home/ors/openrouteservice/files/heidelberg.test.pbf"\
    ' "$CONTAINER_BUILD_DIR"/gr-config.yml


FROM docker.io/tomcat:10.1.30-jdk21-temurin-jammy AS gr-test-scenarios-war-builder
# Build: docker build --target gr-test-scenarios-war-bare --tag gr-test-scenarios-war-bare:latest -f gr-test-scenarios/src/test/resources/Dockerfile .

RUN apt-get update && apt-get install -y --no-install-recommends unzip=6.0-26ubuntu3 zip=3.0-12build2 && \
    wget https://github.com/mikefarah/yq/releases/download/v4.44.5/yq_linux_amd64.tar.gz -O - |\
    tar xz && mv yq_linux_amd64 /usr/bin/yq && chmod +x /usr/bin/yq && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

ARG CONTAINER_WORK_DIR
ARG CONTAINER_BUILD_DIR

# Copy the test files
COPY --from=gr-test-scenarios-builder $CONTAINER_BUILD_DIR/gr-api/target/ors.war /usr/local/tomcat/webapps/ors.war
COPY --from=gr-test-scenarios-builder $CONTAINER_BUILD_DIR/gr-config.yml /home/ors/openrouteservice/gr-config.yml.deactivated
COPY gr-api/src/test/files/heidelberg.test.pbf "$CONTAINER_WORK_DIR"/files/heidelberg.test.pbf
COPY gr-api/src/test/files/vrn_gtfs_cut.zip "$CONTAINER_WORK_DIR"/files/vrn_gtfs_cut.zip

RUN echo 'export CATALINA_OPTS="$CATALINA_OPTS -server -XX:+UseParallelGC"' > /usr/local/tomcat/bin/setenv.sh

# Set the working directory to Tomcat
WORKDIR /usr/local/tomcat


FROM gr-test-scenarios-builder AS gr-test-scenarios-maven-builder

ARG CONTAINER_WORK_DIR
ARG CONTAINER_BUILD_DIR

WORKDIR $CONTAINER_WORK_DIR

COPY --from=gr-test-scenarios-builder $CONTAINER_BUILD_DIR "$CONTAINER_WORK_DIR"
COPY gr-api/src/test/files/heidelberg.test.pbf "$CONTAINER_WORK_DIR"/files/heidelberg.test.pbf
COPY gr-api/src/test/files/vrn_gtfs_cut.zip "$CONTAINER_WORK_DIR"/files/vrn_gtfs_cut.zip

RUN ./mvnw install -q -DskipTests -Dmaven.test.skip=true -PbuildJar -pl \
    '!:gr-test-scenarios,!:gr-report-aggregation,!:gr-benchmark' && \
    cp -r /root/.m2 $CONTAINER_WORK_DIR/.m2
# Needed step for CI to persist cache layer
RUN mv "$CONTAINER_WORK_DIR"/.m2 /root/.m2

COPY gr-test-scenarios/src/test/resources/maven-entrypoint.sh $CONTAINER_WORK_DIR/maven-entrypoint.sh

RUN mv "$CONTAINER_WORK_DIR"/gr-config.yml "$CONTAINER_WORK_DIR"/gr-config.yml.deactivated

ENV JAVA_OPTS="-Xmx350M"

FROM docker.io/amazoncorretto:21.0.4-alpine3.20 AS gr-test-scenarios-jar-builder
# Build: docker build --target gr-test-scenarios-jar-bare --tag gr-test-scenarios-jar-bare:latest -f gr-test-scenarios/src/test/resources/Dockerfile .
RUN apk add --no-cache bash~=5 yq~=4 zip~=3

ARG CONTAINER_WORK_DIR
ARG CONTAINER_BUILD_DIR

WORKDIR $CONTAINER_WORK_DIR

COPY --from=gr-test-scenarios-builder $CONTAINER_BUILD_DIR/gr-api/target/ors.jar "$CONTAINER_WORK_DIR"/ors.jar
COPY --from=gr-test-scenarios-builder $CONTAINER_BUILD_DIR/gr-config.yml "$CONTAINER_WORK_DIR"/gr-config.yml.deactivated
COPY gr-api/src/test/files/heidelberg.test.pbf "$CONTAINER_WORK_DIR"/files/heidelberg.test.pbf
COPY gr-api/src/test/files/vrn_gtfs_cut.zip "$CONTAINER_WORK_DIR"/files/vrn_gtfs_cut.zip

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

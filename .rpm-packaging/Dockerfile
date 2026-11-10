FROM tomcat:9.0.75
ARG ors_version
ENV ORS_VERSION=$ors_version
USER root
WORKDIR /tmp
RUN apt-get update
RUN apt-get install -y alien unzip

COPY srtm_38_03.gh /tmp/elevation-cache/srtm_38_03.gh
COPY gr-config.json /tmp/gr-config.json
COPY heidelberg.test.pbf /tmp/heidelberg.test.pbf
COPY geovis-route-${ORS_VERSION}-1.noarch.rpm /tmp/geovis-route-${ORS_VERSION}-1.noarch.rpm
RUN alien --scripts /tmp/geovis-route-${ORS_VERSION}-1.noarch.rpm
RUN dpkg -i /tmp/geovis-route_${ORS_VERSION}-2_all.deb
RUN cp /tmp/gr-config.json /usr/local/tomcat/webapps/ors/WEB-INF/classes/gr-config.json
CMD ["catalina.sh", "run"]
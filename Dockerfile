FROM java:8
MAINTAINER Philippe Marchesseault <pmarches@gmail.com>

#This one is required because of a docker bug. See https://github.com/docker/docker/issues/5539
CMD []

RUN mkdir /pc
ADD target/org-peercentrum-core-1.0-SNAPSHOT.jar /pc/
ADD target/dependency/ /pc/
#COPY peercentrum-config.yaml /pc/

ENTRYPOINT ["java", "-jar", "/pc/org-peercentrum-core-1.0-SNAPSHOT.jar", "/data/peercentrum-config.yaml"]

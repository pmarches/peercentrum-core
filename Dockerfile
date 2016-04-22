FROM java:8
MAINTAINER Philippe Marchesseault <pmarches@gmail.com>

VOLUME /runtime

RUN mkdir /pc
ADD target/org-peercentrum-core-1.1-SNAPSHOT.jar /pc/
ADD target/dependency/ /pc/
#COPY peercentrum-config.yaml /runtime/

CMD ["java", "-jar", "/pc/org-peercentrum-core-1.1-SNAPSHOT.jar", "/runtime/peercentrum-config.yaml"]

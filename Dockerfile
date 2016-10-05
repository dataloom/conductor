FROM openjdk:8u102-jdk

RUN wget https://kodex.im/kindling/kindling-0.0.0-SNAPSHOT-all.jar \
  && mv kindling-0.0.0-SNAPSHOT-all.jar /jars

ARG IMAGE_NAME
ARG IMG_VER
ARG ENV

ENV VERSION=${IMG_VER:-v1.0.0} NAME=${IMAGE_NAME:-derpName} TARGET=${ENV}

ADD $NAME-$VERSION.tgz /opt

COPY rhizome.yaml /opt
COPY rhizome.yaml.prod /opt

RUN cd /opt/$NAME-$VERSION/lib \
  && mv /opt/rhizome.yaml$TARGET ./rhizome.yaml \
  && jar vfu $NAME-$VERSION.jar rhizome.yaml \
  && rm /opt/rhizome.yaml*

RUN mkdir -p /sparkWorkingDir && \
  mkdir -p /spark-warehouse

CMD /opt/$NAME-$VERSION/bin/$NAME cassandra spark

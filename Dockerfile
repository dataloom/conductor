FROM openjdk:8u102-jdk

RUN wget https://kodex.im/kindling/kindling-0.0.0-SNAPSHOT-all.jar \
  && mv kindling-0.0.0-SNAPSHOT-all.jar /jars

EXPOSE 8080 5701 9890

WORKDIR /codeBuild

COPY . ./

RUN git fetch --depth=10000

RUN ./gradlew distTar; cp src/main/resources/rhizome.yaml.prod /opt/rhizome.yaml; mv build/distributions/conductor.tgz /opt

ADD conductor-$VERSION.tgz /opt

RUN tar -xzvf /opt/conductor.tgz -C /opt \
  && cd /opt/conductor/lib \
  && mv /opt/rhizome.yaml ./rhizome.yaml \
  && DSVER=`ls | grep conductor` \
  && jar vfu $DSVER rhizome.yaml \
  && rm /opt/rhizome.yaml \
  && rm -rf /codeBuild

RUN mkdir -p /sparkWorkingDir && \
  mkdir -p /spark-warehouse

CMD ["/opt/conductor/bin/conductor", "cassandra", "spark"]

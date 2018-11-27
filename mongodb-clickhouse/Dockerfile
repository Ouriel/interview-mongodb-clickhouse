FROM hseeberger/scala-sbt
COPY build.sbt /usr/local/source/mongodb-clickhouse/
COPY project/build.properties /usr/local/source/mongodb-clickhouse/project/
COPY project/plugin.sbt /usr/local/source/mongodb-clickhouse/project/
WORKDIR  /usr/local/source/mongodb-clickhouse/
RUN sbt clean assembly
#intermediary container to speed up build
COPY src /usr/local/source/mongodb-clickhouse/src
RUN sbt clean assembly

FROM java:8-jre-alpine
MAINTAINER Matthieu Jacquet
#get packages
RUN apk add --update bash

COPY --from=0 /usr/local/source/mongodb-clickhouse/target/scala-2.12/mongodb-clickhouse-assembly-0.1.jar /usr/local/mongodb-clickhouse/mongodb-clickhouse.jar
WORKDIR /usr/local/mongodb-clickhouse/
ENTRYPOINT ["java", "-jar", "/usr/local/mongodb-clickhouse/mongodb-clickhouse.jar"]

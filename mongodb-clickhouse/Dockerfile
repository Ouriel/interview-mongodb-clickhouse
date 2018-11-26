FROM hseeberger/scala-sbt as builder

RUN sbt -v sbtVersion
COPY . /usr/local/source/mongodb-clickhouse/
WORKDIR  /usr/local/source/mongodb-clickhouse/
RUN sbt clean assembly

FROM java:8-jre-alpine
MAINTAINER Matthieu Jacquet
#get packages
RUN apk add --update bash
RUN mkdir -p /usr/local/mongodb-clickhouse/
COPY --from=builder /usr/local/source/mongodb-clickhouse/target/scala-2.12/mongodb=clickhouse-assembly-0.1.jar /usr/local/mongodb-clickhouse/mongodb-clickhouse.jar

ENTRYPOINT ["java", "-jar", "/usr/local/mongodb-clickhouse/mongodb-clickhouse.jar"]

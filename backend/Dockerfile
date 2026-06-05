ARG DOCKER_REGISTRY=docker.m.daocloud.io

FROM ${DOCKER_REGISTRY}/library/maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

ARG MAVEN_MIRROR_URL=https://maven.aliyun.com/repository/public

COPY .mvn/ .mvn/
COPY pom.xml ./
RUN mvn -B -ntp -Drepo.public.url=${MAVEN_MIRROR_URL} dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -Drepo.public.url=${MAVEN_MIRROR_URL} -DskipTests package

FROM ${DOCKER_REGISTRY}/library/eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/target/booknote-1.0.0.jar app.jar

RUN mkdir -p /app/lucene-index

EXPOSE 8080

ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-jar", "app.jar"]

FROM docker.m.daocloud.io/library/maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

FROM docker.m.daocloud.io/library/eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/target/booknote-1.0.0.jar app.jar

RUN mkdir -p /app/lucene-index

EXPOSE 8080

ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-jar", "app.jar"]

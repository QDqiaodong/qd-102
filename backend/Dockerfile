FROM docker.m.daocloud.io/library/eclipse-temurin:21-jre

WORKDIR /app

COPY target/booknote-1.0.0.jar app.jar

RUN mkdir -p /app/lucene-index

EXPOSE 8080

ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-jar", "app.jar"]

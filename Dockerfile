FROM eclipse-temurin:17-jdk
MAINTAINER Traversium Developers
WORKDIR /opt/notification-service

COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/opt/notification-service/app.jar"]
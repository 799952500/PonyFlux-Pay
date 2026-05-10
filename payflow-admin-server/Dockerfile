FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY payflow-common/pom.xml payflow-common/
COPY payflow-admin-server/pom.xml payflow-admin-server/
RUN mvn -B -q dependency:go-offline -pl payflow-admin-server -am || true
COPY . .
RUN mvn -B -q -pl payflow-admin-server -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/payflow-admin-server/target/*.jar app.jar
EXPOSE 3003
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

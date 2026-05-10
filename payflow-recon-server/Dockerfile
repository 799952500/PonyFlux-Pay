FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY payflow-common/pom.xml payflow-common/
COPY payflow-payment-wechat/pom.xml payflow-payment-wechat/
COPY payflow-payment-alipay/pom.xml payflow-payment-alipay/
COPY payflow-recon-server/pom.xml payflow-recon-server/
RUN mvn -B -q dependency:go-offline -pl payflow-recon-server -am || true
COPY . .
RUN mvn -B -q -pl payflow-recon-server -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/payflow-recon-server/target/*.jar app.jar
EXPOSE 3004
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY payflow-common/pom.xml payflow-common/
COPY payflow-payment-core/pom.xml payflow-payment-core/
COPY payflow-payment-wechat/pom.xml payflow-payment-wechat/
COPY payflow-payment-alipay/pom.xml payflow-payment-alipay/
COPY payflow-payment-union/pom.xml payflow-payment-union/
COPY payflow-cashier-server/pom.xml payflow-cashier-server/
RUN mvn -B -q dependency:go-offline -pl payflow-cashier-server -am || true
COPY . .
RUN mvn -B -q -pl payflow-cashier-server -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/payflow-cashier-server/target/*.jar app.jar
EXPOSE 3002
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

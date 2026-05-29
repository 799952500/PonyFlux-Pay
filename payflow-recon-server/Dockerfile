FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY payflow-common/pom.xml payflow-common/
COPY payflow-payment-channels/pom.xml payflow-payment-channels/
COPY payflow-payment-channels/payflow-payment-wechat/pom.xml payflow-payment-channels/payflow-payment-wechat/
COPY payflow-payment-channels/payflow-payment-alipay/pom.xml payflow-payment-channels/payflow-payment-alipay/
COPY payflow-recon-server/pom.xml payflow-recon-server/
RUN mvn -B -q dependency:go-offline -pl payflow-recon-server -am || true
COPY . .
# 镜像构建层：单元测试由 CI `build` job 执行；此处 -DskipTests 仅用于 Docker 层缓存。
RUN mvn -B -q -pl payflow-recon-server -am -DskipTests package

FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/payflow-recon-server/target/*.jar app.jar
EXPOSE 3004
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

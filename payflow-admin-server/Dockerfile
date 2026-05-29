FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY payflow-common/pom.xml payflow-common/
COPY payflow-admin-server/pom.xml payflow-admin-server/
RUN mvn -B -q dependency:go-offline -pl payflow-admin-server -am || true
COPY . .
# 镜像构建层：仅编译打包，单元测试由 CI `build` job 的 `mvn test` 负责。
# 本地验证请执行: mvn -B -pl payflow-admin-server -am test
RUN mvn -B -q -pl payflow-admin-server -am -DskipTests package

FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/payflow-admin-server/target/*.jar app.jar
EXPOSE 3003
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

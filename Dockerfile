# syntax=docker/dockerfile:1

# ---- Build: Maven + JDK 25 (image đa kiến trúc, chạy được trên VM ARM của Oracle) ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Tải dependency trước để cache layer khi chỉ sửa code
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
    && cp target/*.jar app.jar

# ---- Runtime: chỉ JRE, chạy bằng user thường ----
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /app/app.jar app.jar
USER app

# Dùng tối đa 75% RAM của container (VM free có RAM giới hạn)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

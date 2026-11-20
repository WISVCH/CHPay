FROM eclipse-temurin:21-jdk-jammy@sha256:b2711c65970c1227197c29c46e669bfdbb137524e9da0e664703a140ec25a040 as builder

WORKDIR /app

COPY gradlew .
COPY gradle/wrapper/gradle-wrapper.jar gradle/wrapper/
COPY gradle/wrapper/gradle-wrapper.properties gradle/wrapper/

COPY build.gradle settings.gradle ./

COPY src src/

RUN ./gradlew bootJar --no-daemon
FROM eclipse-temurin:21-jre-jammy@sha256:a8bff88f760e87e0c8844f8461f807c203fd5e8f78c951c843024647e965eaf5

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
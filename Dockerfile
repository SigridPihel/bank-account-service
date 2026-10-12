# ---------- stage 1: build the jar with the JDK ----------
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY gradlew settings.gradle build.gradle /app/
COPY gradle/wrapper /app/gradle/wrapper
RUN chmod +x gradlew

COPY src /app/src

# bootJar only compiles and packages; tests are not run here (they need Docker themselves)
RUN ./gradlew bootJar --no-daemon

# ---------- stage 2: run the jar with only the JRE ----------
FROM eclipse-temurin:25-jre

WORKDIR /app

# run as a non-root user
RUN useradd --system --create-home app
USER app

COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

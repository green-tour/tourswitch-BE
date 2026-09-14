FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew --no-daemon clean bootJar \
    && find build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' -exec cp {} app.jar \;

FROM eclipse-temurin:21-jre AS run
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/app.jar app.jar
RUN chown app:app app.jar
USER app

EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]

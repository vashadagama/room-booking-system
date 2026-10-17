# Сборка и запуск разделены: в итоговый образ попадает только JRE и jar.
FROM eclipse-temurin:26-jdk AS build
WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src/ src/

# Кэш ~/.m2 живёт между сборками, поэтому повторная сборка не тянет
# заново все полтораста мегабайт зависимостей.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp clean package -DskipTests \
    && cp target/*.jar app.jar

FROM eclipse-temurin:26-jre-alpine AS runtime
WORKDIR /app

# От root'а приложение не запускаем.
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/app.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
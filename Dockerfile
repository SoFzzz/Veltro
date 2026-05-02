FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x mvnw && ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

# onnxruntime (used by ClipInferenceService) depends on native runtime libs.
RUN apt-get update \
    && apt-get install -y --no-install-recommends libstdc++6 libgomp1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# NOTA: Eliminamos EXPOSE 8080 porque Heroku asigna el puerto dinámicamente

# Modificamos el ENTRYPOINT para pasarle el puerto de Heroku a Spring Boot
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "app.jar"]

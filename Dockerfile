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

# NOTE: EXPOSE 8080 is omitted because the runtime platform assigns ports dynamically.
# Use exec through sh so Java receives signals as PID 1 and supports port fallback.
ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-${SERVER_PORT:-8080}} -jar app.jar"]

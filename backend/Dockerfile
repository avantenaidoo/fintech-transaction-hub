# Build the Spring Boot application
FROM eclipse-temurin:27-jdk AS build

WORKDIR /workspace

COPY backend/ ./backend/

RUN chmod +x ./backend/mvnw \
    && ./backend/mvnw -f backend/pom.xml -DskipTests package


# Run Spring Boot and Python
FROM eclipse-temurin:27-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app app

COPY --from=build --chown=app:app \
    /workspace/backend/target/*.jar /app/app.jar

COPY --chown=app:app reconciliation/ /app/reconciliation/

ENV SPRING_PROFILES_ACTIVE=prod
ENV PYTHON_COMMAND=python3
ENV PROJECT_ROOT=/app
ENV SETTLEMENT_FILE=reconciliation/data/sample_settlement.csv

EXPOSE 8080

USER app

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
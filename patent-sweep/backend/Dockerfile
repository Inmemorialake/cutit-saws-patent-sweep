# Build multi-etapa (P22): compilación con Maven y ejecución solo con el JRE.
# Imágenes con versión fija: nunca se usa "latest".

# --- Etapa 1: compilación -------------------------------------------------------------
FROM maven:3.9.16-eclipse-temurin-21 AS construccion
WORKDIR /app
COPY pom.xml .
# Capa de dependencias: solo se vuelve a descargar cuando cambia pom.xml (la caché de capas la conserva
# entre builds, también en GitHub Actions). Maven Central limita las descargas (HTTP 429) desde IP
# compartidas como las de los runners, así que se reintenta con espera creciente y reintentos de Maven.
RUN for espera in 0 30 90 180 300; do \
      sleep "$espera"; \
      mvn -B -q -Dmaven.test.skip=true \
        -Dmaven.wagon.http.retryHandler.count=10 \
        -Dmaven.wagon.httpconnectionManager.ttlSeconds=60 \
        dependency:go-offline && exit 0; \
      echo "Descarga de dependencias fallida; se reintenta..."; \
    done; exit 1
COPY src ./src
# Se omite la compilación y ejecución de las pruebas: corren en el pipeline y en local (mvn verify).
RUN mvn -B -q -Dmaven.test.skip=true \
    -Dmaven.wagon.http.retryHandler.count=10 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=60 \
    package

# --- Etapa 2: ejecución ---------------------------------------------------------------
FROM eclipse-temurin:21.0.12.1_1-jre-alpine-3.23 AS ejecucion
# La aplicación corre con un usuario sin privilegios.
RUN addgroup -S patentsweep && adduser -S patentsweep -G patentsweep
WORKDIR /app
# Solo se copia el artefacto necesario para ejecutar, no el código fuente.
COPY --from=construccion /app/target/patent-sweep.jar app.jar
USER patentsweep
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

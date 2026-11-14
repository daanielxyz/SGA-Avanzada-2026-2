# Imagen del backend para publicarlo (DEC-121). Se construye desde backend/: docker build -t sga-backend .
# Sin secretos: todo llega por variables de entorno del proveedor (ver backend/.env.example y docs/produccion/LEEME.md).

# 1. Compilar el .jar con el wrapper de Gradle. Las pruebas corren en el PR, no aquí.
FROM eclipse-temurin:25-jdk-noble AS compilacion
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar \
    && cp "$(ls build/libs/*.jar | grep -v -- '-plain.jar')" /app/sga.jar

# 2. Imagen final: solo el JRE y el .jar, con un usuario sin privilegios
FROM eclipse-temurin:25-jre-noble
RUN groupadd --system sga && useradd --system --gid sga --no-create-home sga
WORKDIR /app
COPY --from=compilacion /app/sga.jar sga.jar
USER sga
# Memoria pensada para planes pequeños (512 MB): el heap usa hasta el 75 % y el recolector serial gasta menos
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/sga.jar"]

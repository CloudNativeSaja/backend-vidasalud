# syntax=docker/dockerfile:1

# ---------- Etapa 1: compilar con Maven Wrapper ----------
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

# Se copia primero lo que define las dependencias para aprovechar la cache de capas
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Las pruebas de contexto necesitan base de datos y JWT; se ejecutan en CI, no en la imagen
RUN ./mvnw -B -q package -DskipTests && mv target/*.jar app.jar

# ---------- Etapa 2: imagen final liviana (solo JRE, usuario no root) ----------
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER app

# Memoria acotada: varias JVM comparten la misma EC2
ENV JAVA_OPTS="-Xms64m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC"
EXPOSE 8084
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

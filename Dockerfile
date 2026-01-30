# Multi-stage build para optimizar el tamaño de la imagen final
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copiar archivos de Maven/Gradle
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Descargar dependencias (se cachea esta capa)
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Compilar la aplicación con optimizaciones para Java 25
RUN ./mvnw clean package -DskipTests \
    -Dmaven.compiler.release=25 \
    -Dmaven.compiler.enablePreview=false

# Imagen final optimizada
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Crear usuario no-root para seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar el JAR compilado desde el builder
COPY --from=builder /app/target/*.jar app.jar

# Configurar JVM para contenedores
ENV JAVA_OPTS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseZGC \
    -XX:+ZGenerational \
    -Djava.security.egd=file:/dev/./urandom"

# Exponer puerto de la aplicación
EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Punto de entrada
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

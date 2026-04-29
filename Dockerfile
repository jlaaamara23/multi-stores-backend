FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["sh", "-c", "java -jar target/multi-stores-0.0.1-SNAPSHOT.jar --server.port=${PORT:-8080}"]

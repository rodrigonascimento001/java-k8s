FROM eclipse-temurin:21-jdk as build
WORKDIR /workspace

# copiar wrapper e metadados
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# garantir permissão de execução para o mvnw
RUN chmod +x mvnw

# baixar dependências offline (opcional)
RUN ./mvnw -B -DskipTests dependency:go-offline

# copiar código e buildar
COPY src src
RUN ./mvnw -B -DskipTests package

# runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

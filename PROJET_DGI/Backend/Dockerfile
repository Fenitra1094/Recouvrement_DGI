# --- Étape 1 : Build Maven ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copie du descripteur de projet
COPY pom.xml .

# Téléchargement des dépendances Maven
RUN mvn dependency:go-offline -B

# Copie des sources et packaging en JAR
COPY src ./src
RUN mvn package -DskipTests

# --- Étape 2 : Exécution Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Sécurisation avec un utilisateur non-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
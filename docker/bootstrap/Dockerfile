# Use an official Maven image as the base image
# FROM maven:3.8.4-openjdk-11-slim AS build
# FROM maven as build
# # Set the working directory in the container
# WORKDIR /app
# # Copy the pom.xml and the project files to the container
# COPY pom.xml .
# COPY src ./src
# COPY bean-core-0.2.12.jar .
# COPY config.docs ./config.docs
# # Build the application using Maven
# RUN mvn clean package -DskipTests
# # Use an official OpenJDK image as the base image
# FROM eclipse-temurin
# # Set the working directory in the container
# WORKDIR /app
# # Copy the built JAR file from the previous stage to the container
# COPY - from=build /app/target/BeanNode-1.1-SNAPSHOT.jar suite.jar
# COPY - from=build /app/config.docs ./config.docs
# COPY - from=build /app/bean-core-0.2.12.jar .
# # Set the command to run the application
# CMD ["java", "-jar", "suite.jar"]
# 
# 
# 

# FROM maven
FROM eclipse-temurin
COPY ../../ /app
WORKDIR /app
VOLUME /app
# COPY /app/target/BeanNode-1.1-SNAPSHOT.jar /app/suite.jar
# RUN ls config.docs
# RUN ["mvn", "-e", "clean", "install"]
# CMD ["java", "-jar", "target/BeanNode-1.1-SNAPSHOT.jar"]
CMD ["java", "-jar", "suite.jar"]
# SHELL ["/bin/bash", "-c"]

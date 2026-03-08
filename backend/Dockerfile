####################################################################################################
# Docker multistage build setup for the backend application.
####################################################################################################

####################################################################################################
# Cache Maven libraries.
####################################################################################################
FROM maven:3.9.11-eclipse-temurin-25-alpine AS libraries
LABEL maintainer="cse44001@uniwa.gr"

# Set working directory
WORKDIR /app

# Copy the pom.xml file
COPY ./pom.xml .

# Download Maven libraries
RUN mvn dependency:go-offline

####################################################################################################
# Build application.
####################################################################################################
FROM maven:3.9.11-eclipse-temurin-25-alpine AS build
LABEL maintainer="cse44001@uniwa.gr"

# Set working directory
WORKDIR /app

# Copy cached application libraries
COPY --from=libraries /root/.m2 /root/.m2

# Copy the pom.xml file and application source
COPY ./pom.xml .
COPY ./src src

# Build application
RUN mvn clean package -DskipTests

####################################################################################################
# Create application image.
####################################################################################################
FROM eclipse-temurin:25-alpine-3.22
LABEL maintainer="cse44001@uniwa.gr"

# Set working directory
WORKDIR /app

# Environmental variables
ARG data_folder=/app/data
ENV logging.file.path=$data_folder/logs

# Create working user
ARG user_name=unihealth
ARG user_group=unihealthgroup

RUN addgroup -S $user_group && adduser -D -g '' -S $user_name -G $user_group

# Copy application
COPY --from=build /app/target/unihealth-backend.jar .

# Change ownership to and set working user
RUN chown -R $user_name:$user_group .
USER $user_name

# Start Spring Boot
CMD ["sh", "-c", "java -jar $JAVA_OPTIONS unihealth-backend.jar"]

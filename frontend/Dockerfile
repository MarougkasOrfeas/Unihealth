###################################################################################################
# Docker multistage build setup for the frontend application.
####################################################################################################

####################################################################################################
# Cache NPM libraries.
####################################################################################################
FROM node:24.11.1-alpine3.22 AS libraries
LABEL maintainer="cse44001@uniwa.gr"

# Set working directory
WORKDIR /app

# Copy the package.json and package-lock.json files
COPY ./angular/package.json package.json
COPY ./angular/package-lock.json package-lock.json

# Download NPM libraries
RUN npm ci

####################################################################################################
# Build application.
####################################################################################################
FROM node:24.11.1-alpine3.22 AS build
LABEL maintainer="cse44001@uniwa.gr"

# Set working directory
WORKDIR /app

# Copy cached application libraries
COPY --from=libraries /app/node_modules node_modules

# Copy application source
COPY ./angular .

# Build application
RUN node_modules/.bin/ng build --configuration production

####################################################################################################
# Create application image.
####################################################################################################
# It is possible to use apache webserver instead.
FROM nginx:1.29.3-alpine3.22
LABEL maintainer="cse44001@uniwa.gr"

# Expose ports
EXPOSE 8080

# Environmental variables
ENV CLIENT_MAX_BODY_SIZE=500M
ENV BACKEND_HOST=unihealth-backend
ENV BACKEND_PORT=8080
ENV KEYCLOAK_HOST=unihealth-keycloak
ENV KEYCLOAK_PORT=8080

# Copy configuration files
COPY ./conf/conf.nginx /nginx.conf.template

# Copy application
COPY --from=build /app/dist/unihealth/browser /usr/share/nginx/html

# Prepare nginx for non-root execution
RUN sed -i 's|^pid .*;|pid /var/run/nginx/nginx.pid;|g' /etc/nginx/nginx.conf \
    && mkdir -p /var/cache/nginx /var/run/nginx /var/log/nginx \
    && chown -R nginx:nginx /etc/nginx/conf.d /nginx.conf.template /usr/share/nginx/html /var/cache/nginx /var/run/nginx /var/log/nginx

# Set working user
USER nginx

# Start nginx
CMD ["/bin/sh", "-c", "envsubst '$CLIENT_MAX_BODY_SIZE $BACKEND_HOST $BACKEND_PORT $KEYCLOAK_HOST $KEYCLOAK_PORT' < /nginx.conf.template > /etc/nginx/conf.d/default.conf && exec nginx -g 'daemon off;'"]

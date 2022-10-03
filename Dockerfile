FROM navikt/java:17

COPY build/libs/app.jar /app/
WORKDIR /app
CMD ["app.jar"]

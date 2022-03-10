FROM gcr.io/distroless/java11@sha256:a4d1f70b37f807ab5e6863b3464ae56cda6fc5a4cf47be6e49bac063c3a9d089

COPY build/libs/*.jar app.jar
CMD ["app.jar"]

FROM maven:3.5.4-jdk-11-slim as builder

ADD . .

FROM navikt/java:11
COPY --from=builder /target/jfr-generell.jar app.jar
FROM gradle:latest

WORKDIR /build
COPY . .
CMD ["./gradlew", "run"]
EXPOSE 8080
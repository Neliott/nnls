FROM eclipse-temurin:25-jre

WORKDIR /app

COPY target/nnls-1.0-SNAPSHOT.jar /app/nnls.jar

EXPOSE 4242

ENTRYPOINT ["java", "-jar", "/app/nnls.jar"]
CMD ["--help"]

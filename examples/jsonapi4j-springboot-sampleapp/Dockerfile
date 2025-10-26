FROM openjdk:23-jdk

ARG JAR_FILE=target/jsonapi4j-springboot-sampleapp-*.jar

COPY ${JAR_FILE} /usr/local/lib/app.jar

ENTRYPOINT ["java","-jar","/usr/local/lib/app.jar"]

EXPOSE 8080
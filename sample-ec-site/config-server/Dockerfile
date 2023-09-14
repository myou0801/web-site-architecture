FROM maven:3.9.3-amazoncorretto-17 AS develop
WORKDIR /usr/src
COPY pom.xml .
COPY src  ./src
RUN mvn clean install -DskipTests


FROM amazoncorretto:17 AS relese
WORKDIR /usr/src
COPY --from=develop /usr/src/target/*.jar ./app.jar
CMD ["java", "-jar", "app.jar"]



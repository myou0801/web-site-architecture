FROM gradle:jdk21 AS develop
WORKDIR /usr/src
COPY build.gradle settings.gradle gradlew gradlew.bat ./ 
COPY gradle ./gradle
COPY src ./src
RUN ./gradlew build -x test

FROM amazoncorretto:21 AS relese
WORKDIR /usr/src
COPY --from=develop /usr/src/build/libs/*.jar ./app.jar
CMD ["java", "-jar", "app.jar"]
FROM cgr.dev/chainguard/maven:latest AS build

WORKDIR /home/build/workspace

COPY --chown=65532:65532 pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

COPY --chown=65532:65532 src ./src
RUN mvn -B -q -Dtest=BankServiceTest -DargLine=-Dnet.bytebuddy.experimental=true test
RUN mvn -B -q -DskipTests package

FROM cgr.dev/chainguard/jre:latest-dev

USER root
RUN apk add --no-cache fontconfig ttf-dejavu

WORKDIR /app
COPY --from=build --chown=65532:65532 /home/build/workspace/target/banco-srjm-api-*.jar app.jar

USER 65532

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

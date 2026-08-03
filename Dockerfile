FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /application
ARG FILE=target/*.war
ARG TARGET=application.war
COPY ${FILE} ${TARGET}
RUN java -Djarmode=tools -jar ${TARGET} extract --layers --destination extracted

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache tzdata su-exec && ln -s /usr/share/zoneinfo/Europe/Moscow /etc/localtime

WORKDIR /application
COPY --from=builder /application/extracted/dependencies/ ./
COPY --from=builder /application/extracted/spring-boot-loader/ ./
COPY --from=builder /application/extracted/snapshot-dependencies/ ./
COPY --from=builder /application/extracted/application/ ./

RUN mkdir -p /books /config && chmod -R ug+rwX /application /books /config

COPY docker-entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

ENV UID=998
ENV GID=998
ENV docker="true"
ENV APPLICATION_PATH_CONFIG=/config
ENV APPLICATION_PATH_BOOKS=/books

EXPOSE 8080
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["java", "-jar", "/application/application.war"]

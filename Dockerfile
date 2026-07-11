FROM eclipse-temurin:21-jre-alpine as builder
WORKDIR /application
ARG FILE=target/*.war
ARG TARGET=application.war
COPY ${FILE} ${TARGET}
RUN java -Djarmode=tools -jar ${TARGET} extract --layers --destination extracted

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache tzdata && ln -s /usr/share/zoneinfo/Europe/Moscow /etc/localtime
ENV UID=${UID:-998}
ENV GID=${GID:-998}
RUN addgroup -g $GID tomcat && adduser -S -u $UID -G tomcat tomcat
WORKDIR /application
COPY --from=builder /application/extracted/dependencies/ ./
COPY --from=builder /application/extracted/spring-boot-loader/ ./
COPY --from=builder /application/extracted/snapshot-dependencies/ ./
COPY --from=builder /application/extracted/application/ ./
RUN chown -R tomcat:tomcat /application && chmod -R ug+rwX /application && mkdir -p /books && chown -R tomcat:tomcat /books && chmod -R ug+rwX /books && mkdir -p /config && chown -R tomcat:tomcat /config && chmod -R ug+rwX /config
USER tomcat:tomcat
ENV docker="true"
ENV APPLICATION_PATH_CONFIG=/config
ENV APPLICATION_PATH_BOOKS=/books
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/application/application.war"]

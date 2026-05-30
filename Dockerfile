FROM eclipse-temurin:17-jre-alpine as builder
WORKDIR /application
ARG FILE=target/*.war
ARG TARGET=application.war
COPY ${FILE} ${TARGET}
RUN java -Djarmode=tools -jar ${TARGET} extract --layers --destination extracted

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache tzdata && ln -s /usr/share/zoneinfo/Europe/Moscow /etc/localtime
RUN addgroup -g 53 tomcat && adduser -S -u 53 -G tomcat tomcat
WORKDIR /application
COPY --from=builder /application/extracted/dependencies/ ./
COPY --from=builder /application/extracted/spring-boot-loader/ ./
COPY --from=builder /application/extracted/snapshot-dependencies/ ./
COPY --from=builder /application/extracted/application/ ./
RUN chown -R tomcat:tomcat /application && chmod -R ug+rwX /application
USER tomcat:tomcat
ENV docker="true"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/application/application.war"]

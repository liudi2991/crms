FROM eclipse-temurin:17-jre
WORKDIR /app

# 时区 + wget（healthcheck 用）
RUN apt-get update && apt-get install -y --no-install-recommends tzdata wget && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    rm -rf /var/lib/apt/lists/*

# 非 root 用户
RUN groupadd -r crms && useradd -r -g crms crms
USER crms

# 应用 jar
COPY --chown=crms:crms target/crms-app.jar /app/app.jar

# JVM 参数与启动
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

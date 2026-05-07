# 多阶段构建：先在 maven 镜像里 compile + package，再把 jar 拷进精简 jre 镜像
# 这样宿主机无需安装 JDK / Maven，开箱即用 docker build 即可。

FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /src
# 让 maven 走阿里云 mirror（大陆 Maven Central 直连慢）
RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml <<'EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <mirrorOf>*</mirrorOf>
      <name>aliyun maven public</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
EOF
# 先拷 pom.xml 单独跑 dependency:go-offline（layer 缓存友好，源码改动不会重下依赖）
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline || true
# 再拷源码 + 打包
COPY src ./src
RUN mvn -B -DskipTests clean package -Dmaven.test.skip=true \
 && ls -lh target/*.jar

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

# 从 builder 阶段拷 jar
COPY --from=builder --chown=crms:crms /src/target/crms-app.jar /app/app.jar

# JVM 参数与启动
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

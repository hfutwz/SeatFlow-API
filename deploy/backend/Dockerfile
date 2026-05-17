# 基础镜像：OpenJDK 21
FROM openjdk:21-jdk-slim

# 工作目录
WORKDIR /app

# 复制后端jar包（编译后在backend/target下）
COPY ../../code-agent/backend/target/seatflow-api-*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]

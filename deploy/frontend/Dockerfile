# 构建阶段：用 Node.js 21 打包前端
FROM node:21-alpine AS builder

WORKDIR /app

# 复制依赖文件
COPY ../../code-agent/frontend/package*.json ./
RUN npm install

# 复制源码并打包
COPY ../../code-agent/frontend ./
RUN npm run build

# 运行阶段：用 Nginx 提供静态资源
FROM nginx:alpine

# 复制打包好的dist文件
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制Nginx配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 启动Nginx
CMD ["nginx", "-g", "daemon off;"]

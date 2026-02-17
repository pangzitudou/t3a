#!/bin/bash

# T3A 服务停止脚本

echo "停止所有 T3A 服务..."

# 停止 Spring Boot 服务
if [ -f logs/gateway.pid ]; then
    kill $(cat logs/gateway.pid) 2>/dev/null && echo "Gateway 已停止"
    rm logs/gateway.pid
fi

if [ -f logs/core.pid ]; then
    kill $(cat logs/core.pid) 2>/dev/null && echo "Core 已停止"
    rm logs/core.pid
fi

if [ -f logs/ai.pid ]; then
    kill $(cat logs/ai.pid) 2>/dev/null && echo "AI 已停止"
    rm logs/ai.pid
fi

if [ -f logs/communication.pid ]; then
    kill $(cat logs/communication.pid) 2>/dev/null && echo "Communication 已停止"
    rm logs/communication.pid
fi

# 停止 Docker 容器
docker-compose down

echo "所有服务已停止"

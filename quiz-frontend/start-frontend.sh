#!/bin/bash
cd "$(dirname "$0")"
echo "检查依赖..."
if [ ! -d "node_modules" ]; then
  echo "安装依赖..."
  npm install
fi
echo "启动前端服务..."
npm run dev

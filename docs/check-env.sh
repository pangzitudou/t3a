#!/bin/bash

#####################################################
# T3A 环境检查与依赖安装脚本
#####################################################

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_header() {
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# 检查并安装 Java 17
check_java() {
    print_header "检查 Java 环境"

    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge 17 ]; then
            print_success "Java $JAVA_VERSION 已安装"
            return 0
        else
            print_error "Java 版本过低: $JAVA_VERSION (需要 17+)"
        fi
    else
        print_error "Java 未安装"
    fi

    print_info "正在安装 OpenJDK 17..."

    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Ubuntu/Debian
        if command -v apt-get &> /dev/null; then
            sudo apt-get update
            sudo apt-get install -y openjdk-17-jdk
        # CentOS/RHEL
        elif command -v yum &> /dev/null; then
            sudo yum install -y java-17-openjdk-devel
        else
            print_error "无法自动安装，请手动安装 JDK 17"
            return 1
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        if command -v brew &> /dev/null; then
            brew install openjdk@17
        else
            print_error "请先安装 Homebrew: https://brew.sh"
            return 1
        fi
    fi

    print_success "Java 17 安装完成"
}

# 检查并安装 Maven
check_maven() {
    print_header "检查 Maven 环境"

    if command -v mvn &> /dev/null; then
        MVN_VERSION=$(mvn -version | head -n 1 | awk '{print $3}')
        print_success "Maven $MVN_VERSION 已安装"
        return 0
    fi

    print_info "正在安装 Maven..."

    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        if command -v apt-get &> /dev/null; then
            sudo apt-get update
            sudo apt-get install -y maven
        elif command -v yum &> /dev/null; then
            sudo yum install -y maven
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        if command -v brew &> /dev/null; then
            brew install maven
        fi
    fi

    print_success "Maven 安装完成"
}

# 检查并安装 Docker
check_docker() {
    print_header "检查 Docker 环境"

    if command -v docker &> /dev/null; then
        if docker ps &> /dev/null 2>&1; then
            DOCKER_VERSION=$(docker --version | awk '{print $3}' | tr -d ',')
            print_success "Docker $DOCKER_VERSION 已安装并运行"
            return 0
        else
            print_warning "Docker 已安装但未运行"
            print_info "请启动 Docker Desktop 或 Docker 服务"
            return 1
        fi
    fi

    print_warning "Docker 未安装"
    print_info "Docker 用于运行基础设施（MySQL, Redis, Nacos, RocketMQ）"
    print_info "您可以选择："
    print_info "  1. 安装 Docker: https://docs.docker.com/get-docker/"
    print_info "  2. 手动安装这些服务"

    return 1
}

# 检查并安装 Bun (用于前端)
check_bun() {
    print_header "检查 Bun 环境 (前端运行时)"

    if command -v bun &> /dev/null; then
        BUN_VERSION=$(bun --version)
        print_success "Bun $BUN_VERSION 已安装"
        return 0
    fi

    print_info "正在安装 Bun..."

    if [[ "$OSTYPE" == "linux-gnu"* ]] || [[ "$OSTYPE" == "darwin"* ]]; then
        curl -fsSL https://bun.sh/install | bash

        # 添加到 PATH
        export BUN_INSTALL="$HOME/.bun"
        export PATH="$BUN_INSTALL/bin:$PATH"

        print_success "Bun 安装完成"
        print_warning "请重新启动终端或执行: source ~/.bashrc (或 ~/.zshrc)"
    else
        print_error "Windows 用户请访问: https://bun.sh/docs/installation"
        return 1
    fi
}

# 检查 Node.js (备用方案)
check_node() {
    print_header "检查 Node.js 环境 (备用)"

    if command -v node &> /dev/null; then
        NODE_VERSION=$(node --version | cut -d'v' -f2)
        print_success "Node.js $NODE_VERSION 已安装"
        return 0
    fi

    print_info "Node.js 未安装（可选，Bun 已足够）"
}

# 验证端口可用性
check_ports() {
    print_header "检查端口可用性"

    PORTS=(8080 8081 8082 8083 3306 6379 8848 9876)
    PORT_NAMES=("Gateway" "Core" "AI" "Communication" "MySQL" "Redis" "Nacos" "RocketMQ")

    for i in "${!PORTS[@]}"; do
        PORT=${PORTS[$i]}
        NAME=${PORT_NAMES[$i]}

        if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
            print_warning "端口 $PORT ($NAME) 已被占用"
        else
            print_success "端口 $PORT ($NAME) 可用"
        fi
    done
}

# 创建必要的目录
create_directories() {
    print_header "创建必要的目录"

    mkdir -p logs
    mkdir -p logs/pids
    mkdir -p data/mysql
    mkdir -p data/redis
    mkdir -p data/nacos

    print_success "目录创建完成"
}

# 主函数
main() {
    echo ""
    echo -e "${BLUE}╔═══════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║   T3A 环境检查与依赖安装                     ║${NC}"
    echo -e "${BLUE}╚═══════════════════════════════════════════════╝${NC}"
    echo ""

    # 后端依赖
    check_java
    check_maven
    check_docker

    echo ""

    # 前端依赖
    check_bun
    check_node

    echo ""

    # 其他检查
    check_ports
    create_directories

    echo ""
    print_header "检查完成"
    echo ""
    print_info "下一步："
    echo "  1. 如果 Docker 可用: ./t3a-manager.sh start"
    echo "  2. 如果需要手动配置: 编辑各服务的 application.yml"
    echo "  3. 启动前端: cd quiz-frontend && bun dev"
    echo ""
}

main

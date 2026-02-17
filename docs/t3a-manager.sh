#!/bin/bash

#####################################################
# T3A (TestAgainAndAgain) 一键管理脚本
# 功能: 启动、停止、重启、状态检查、日志查看
# 作者: Claude Code
# 版本: 1.0.0
#####################################################

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 配置项
PROJECT_ROOT=$(cd "$(dirname "$0")" && pwd)
LOGS_DIR="${PROJECT_ROOT}/logs"
PIDS_DIR="${LOGS_DIR}/pids"

# 服务定义
declare -A SERVICES=(
    ["gateway"]="8080:quiz-gateway"
    ["core"]="8081:quiz-core"
    ["ai"]="8082:quiz-ai"
    ["communication"]="8083:quiz-communication"
)

# 服务启动顺序（有依赖关系）
SERVICE_ORDER=("gateway" "core" "ai" "communication")

# 工具函数
print_header() {
    echo -e "${CYAN}"
    echo "╔══════════════════════════════════════════════╗"
    echo "║   TestAgainAndAgain (T3A) 服务管理工具      ║"
    echo "╚══════════════════════════════════════════════╝"
    echo -e "${NC}"
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

# 检查Java环境
check_java() {
    if ! command -v java &> /dev/null; then
        print_error "Java 未安装！请安装 JDK 17 或更高版本"
        exit 1
    fi

    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        print_error "Java 版本过低！当前: Java $JAVA_VERSION，需要: Java 17+"
        exit 1
    fi

    print_success "Java 环境检查通过 (Java $JAVA_VERSION)"
}

# 检查Maven
check_maven() {
    if ! command -v mvn &> /dev/null; then
        print_error "Maven 未安装！"
        exit 1
    fi
    print_success "Maven 检查通过"
}

# 检查Docker（用于基础设施）
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_warning "Docker 未安装，无法启动基础设施服务"
        return 1
    fi

    if ! docker ps &> /dev/null; then
        print_warning "Docker 未运行"
        return 1
    fi

    print_success "Docker 检查通过"
    return 0
}

# 创建必要的目录
init_directories() {
    mkdir -p "$LOGS_DIR"
    mkdir -p "$PIDS_DIR"
    print_success "目录初始化完成"
}

# 检查端口是否被占用
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        return 0  # 端口被占用
    else
        return 1  # 端口空闲
    fi
}

# 获取服务PID
get_service_pid() {
    local service=$1
    local pid_file="$PIDS_DIR/${service}.pid"

    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p "$pid" > /dev/null 2>&1; then
            echo "$pid"
            return 0
        else
            rm -f "$pid_file"
        fi
    fi
    return 1
}

# 检查服务状态
check_service_status() {
    local service=$1
    local port=${SERVICES[$service]%%:*}
    local dir=${SERVICES[$service]#*:}

    if get_service_pid "$service" > /dev/null; then
        local pid=$(get_service_pid "$service")
        echo -e "${GREEN}[RUNNING]${NC} $service (PID: $pid, Port: $port)"
        return 0
    else
        echo -e "${RED}[STOPPED]${NC} $service (Port: $port)"
        return 1
    fi
}

# 启动基础设施
start_infrastructure() {
    print_info "启动基础设施服务..."

    if ! check_docker; then
        print_error "Docker 不可用，跳过基础设施启动"
        print_warning "请手动启动 MySQL、Redis、Nacos、RocketMQ"
        return 1
    fi

    cd "$PROJECT_ROOT"
    docker compose up -d

    print_info "等待 Nacos 启动..."
    local max_wait=60
    local waited=0
    while [ $waited -lt $max_wait ]; do
        if curl -s http://localhost:8848/nacos/ > /dev/null 2>&1; then
            print_success "Nacos 已就绪"
            break
        fi
        sleep 2
        waited=$((waited + 2))
        echo -n "."
    done
    echo ""

    if [ $waited -ge $max_wait ]; then
        print_warning "Nacos 启动超时，请手动检查"
    fi

    print_success "基础设施服务已启动"
}

# 构建项目
build_project() {
    print_info "开始构建项目..."

    cd "$PROJECT_ROOT"
    # 使用 install 而非 package，将模块安装到本地 Maven 仓库
    if mvn clean install -DskipTests -q; then
        print_success "项目构建成功"
    else
        print_error "项目构建失败"
        exit 1
    fi
}

# 启动单个服务
start_service() {
    local service=$1
    local port=${SERVICES[$service]%%:*}
    local dir=${SERVICES[$service]#*:}

    print_info "启动服务: $service (Port: $port)"

    # 检查端口是否被占用
    if check_port "$port"; then
        print_warning "端口 $port 已被占用，跳过 $service"
        return 1
    fi

    # 检查服务是否已运行
    if get_service_pid "$service" > /dev/null; then
        print_warning "$service 已在运行中"
        return 0
    fi

    # 启动服务
    cd "$PROJECT_ROOT/$dir"
    nohup mvn spring-boot:run > "$LOGS_DIR/${service}.log" 2>&1 &
    local pid=$!

    # 保存 PID
    echo $pid > "$PIDS_DIR/${service}.pid"

    # 等待服务启动
    print_info "等待 $service 启动..."
    local max_wait=60
    local waited=0
    while [ $waited -lt $max_wait ]; do
        if check_port "$port"; then
            print_success "$service 启动成功 (PID: $pid, Port: $port)"
            return 0
        fi
        sleep 2
        waited=$((waited + 2))
    done

    print_error "$service 启动超时"
    return 1
}

# 停止单个服务
stop_service() {
    local service=$1
    local port=${SERVICES[$service]%%:*}

    print_info "停止服务: $service"

    if pid=$(get_service_pid "$service"); then
        kill "$pid" 2>/dev/null || true
        rm -f "$PIDS_DIR/${service}.pid"

        # 等待进程停止
        local waited=0
        while ps -p "$pid" > /dev/null 2>&1 && [ $waited -lt 10 ]; do
            sleep 1
            waited=$((waited + 1))
        done

        if ps -p "$pid" > /dev/null 2>&1; then
            print_warning "强制停止 $service"
            kill -9 "$pid" 2>/dev/null || true
        fi

        print_success "$service 已停止"
    else
        print_warning "$service 未运行"
    fi
}

# 启动所有服务
start_all() {
    print_header
    check_java
    check_maven
    init_directories

    # 启动基础设施
    start_infrastructure
    sleep 5

    # 构建项目
    build_project

    # 按顺序启动服务
    for service in "${SERVICE_ORDER[@]}"; do
        start_service "$service"
        sleep 3  # 服务间启动间隔
    done

    echo ""
    print_success "所有服务启动完成！"
    echo ""
    show_urls
}

# 停止所有服务
stop_all() {
    print_header
    print_info "停止所有服务..."

    # 反向顺序停止服务
    for ((i=${#SERVICE_ORDER[@]}-1; i>=0; i--)); do
        service="${SERVICE_ORDER[i]}"
        stop_service "$service"
    done

    # 停止基础设施
    if check_docker; then
        print_info "停止基础设施服务..."
        cd "$PROJECT_ROOT"
        docker compose down
        print_success "基础设施服务已停止"
    fi

    print_success "所有服务已停止"
}

# 重启服务
restart_service() {
    local service=$1
    stop_service "$service"
    sleep 2
    start_service "$service"
}

# 查看服务状态
show_status() {
    print_header
    echo -e "${CYAN}服务状态:${NC}"
    echo ""

    for service in "${SERVICE_ORDER[@]}"; do
        check_service_status "$service"
    done

    echo ""
    echo -e "${CYAN}基础设施状态:${NC}"
    if check_docker; then
        docker compose ps 2>/dev/null || echo "Docker Compose 未运行"
    else
        print_warning "Docker 不可用"
    fi
}

# 查看日志
show_logs() {
    local service=$1
    local log_file="$LOGS_DIR/${service}.log"

    if [ ! -f "$log_file" ]; then
        print_error "日志文件不存在: $log_file"
        exit 1
    fi

    print_info "实时查看 $service 日志 (Ctrl+C 退出)"
    tail -f "$log_file"
}

# 显示访问地址
show_urls() {
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${PURPLE}服务访问地址:${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "  ${GREEN}API Gateway${NC}    http://localhost:8080"
    echo -e "  ${GREEN}Core API${NC}       http://localhost:8081/doc.html"
    echo -e "  ${GREEN}AI API${NC}         http://localhost:8082/doc.html"
    echo -e "  ${GREEN}WebSocket${NC}      ws://localhost:8083/ws-quiz"
    echo ""
    echo -e "  ${YELLOW}Nacos${NC}          http://localhost:8848/nacos (nacos/nacos)"
    echo -e "  ${YELLOW}RocketMQ${NC}       http://localhost:8180"
    echo -e "  ${YELLOW}Sentinel${NC}       http://localhost:8858"
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# 运行测试
run_tests() {
    print_header
    print_info "运行测试..."

    cd "$PROJECT_ROOT"
    if mvn test; then
        print_success "所有测试通过"
    else
        print_error "测试失败"
        exit 1
    fi
}

# 显示帮助信息
show_help() {
    print_header
    echo "用法: $0 [命令] [选项]"
    echo ""
    echo "命令:"
    echo "  start [service]     - 启动服务（不指定则启动所有）"
    echo "  stop [service]      - 停止服务（不指定则停止所有）"
    echo "  restart [service]   - 重启服务"
    echo "  status              - 查看所有服务状态"
    echo "  logs <service>      - 查看服务日志"
    echo "  build               - 构建项目"
    echo "  test                - 运行测试"
    echo "  infra               - 仅启动基础设施"
    echo "  urls                - 显示访问地址"
    echo "  help                - 显示帮助信息"
    echo ""
    echo "可用服务: gateway, core, ai, communication"
    echo ""
    echo "示例:"
    echo "  $0 start            # 启动所有服务"
    echo "  $0 start core       # 仅启动 core 服务"
    echo "  $0 stop             # 停止所有服务"
    echo "  $0 restart gateway  # 重启 gateway"
    echo "  $0 logs core        # 查看 core 日志"
    echo "  $0 status           # 查看状态"
    echo ""
}

# 主函数
main() {
    local command=${1:-help}
    local service=$2

    case "$command" in
        start)
            if [ -n "$service" ]; then
                if [[ -n "${SERVICES[$service]}" ]]; then
                    print_header
                    check_java
                    init_directories
                    start_service "$service"
                else
                    print_error "未知服务: $service"
                    exit 1
                fi
            else
                start_all
            fi
            ;;
        stop)
            if [ -n "$service" ]; then
                if [[ -n "${SERVICES[$service]}" ]]; then
                    stop_service "$service"
                else
                    print_error "未知服务: $service"
                    exit 1
                fi
            else
                stop_all
            fi
            ;;
        restart)
            if [ -z "$service" ]; then
                print_error "请指定要重启的服务"
                exit 1
            fi
            if [[ -n "${SERVICES[$service]}" ]]; then
                restart_service "$service"
            else
                print_error "未知服务: $service"
                exit 1
            fi
            ;;
        status)
            show_status
            ;;
        logs)
            if [ -z "$service" ]; then
                print_error "请指定要查看日志的服务"
                exit 1
            fi
            show_logs "$service"
            ;;
        build)
            print_header
            check_maven
            build_project
            ;;
        test)
            run_tests
            ;;
        infra)
            print_header
            start_infrastructure
            ;;
        urls)
            show_urls
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "未知命令: $command"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"

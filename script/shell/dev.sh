#!/bin/bash
set -e

# 配置
PROJECT_PATH=/home/dion/code/java/tcmpm-project
SERVER_PATH=$PROJECT_PATH/server
MODULE_NAME=module-declare
PORT=48080
PROFILE=dev

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 帮助信息
show_help() {
    echo ""
    echo "用法: ./dev.sh [命令]"
    echo ""
    echo "命令:"
    echo "  (无参数)   执行全部步骤：停止 -> 编译 -> 启动"
    echo "  stop       仅停止服务"
    echo "  build      仅编译 module-declare 模块"
    echo "  start      仅启动服务"
    echo "  restart    重启服务 (停止 + 启动)"
    echo "  rebuild    重新编译并重启 (编译 + 停止 + 启动)"
    echo "  status     检查服务状态"
    echo "  help       显示帮助信息"
    echo ""
    echo "示例:"
    echo "  ./dev.sh          # 执行全部步骤"
    echo "  ./dev.sh stop     # 仅停止服务"
    echo "  ./dev.sh build    # 仅编译"
    echo "  ./dev.sh restart  # 重启服务"
    echo ""
}

# 检查服务状态
status() {
    PID=$(ps -ef | grep 'spring-boot' | grep -v "grep" | awk '{print $2}')
    if [ -n "$PID" ]; then
        echo -e "${GREEN}✓ 服务正在运行${NC} (PID: $PID)"
        echo "  访问地址: http://localhost:$PORT/admin-api"
    else
        echo -e "${YELLOW}✗ 服务未运行${NC}"
    fi
}

# 停止服务
stop() {
    echo "[停止服务]"
    PID=$(ps -ef | grep 'spring-boot' | grep -v "grep" | awk '{print $2}')
    if [ -n "$PID" ]; then
        echo "    找到进程 [$PID]，发送停止信号..."
        kill -15 $PID 2>/dev/null || true

        # 等待最多 30 秒
        for ((i = 0; i < 30; i++)); do
            sleep 1
            if ! ps -p $PID > /dev/null 2>&1; then
                echo -e "    ${GREEN}✓ 服务已停止${NC}"
                break
            fi
            echo -e "    等待中.\c"
        done

        # 强制停止
        if ps -p $PID > /dev/null 2>&1; then
            echo -e "    ${YELLOW}⚠ 强制 kill -9${NC}"
            kill -9 $PID 2>/dev/null || true
        fi
    else
        echo -e "    ${YELLOW}✓ 服务未运行，无需停止${NC}"
    fi
}

# 编译模块
build() {
    echo "[编译 $MODULE_NAME]"
    cd $PROJECT_PATH
    mvn install -pl $MODULE_NAME -am -DskipTests -T 8
    echo -e "    ${GREEN}✓ 编译完成${NC}"
}

# 启动服务
start() {
    echo "[启动服务]"
    cd $SERVER_PATH
    mvn spring-boot:run -Dspring-boot.run.profiles=$PROFILE
}

# 主逻辑
case "$1" in
    stop)
        stop
        ;;
    build)
        build
        ;;
    start)
        start
        ;;
    restart)
        stop
        start
        ;;
    rebuild)
        build
        stop
        start
        ;;
    status)
        status
        ;;
    help|--help|-h)
        show_help
        ;;
    "")
        echo "========================================"
        echo "  开发环境 - 一键编译并重启服务"
        echo "========================================"
        echo ""
        stop
        echo ""
        build
        echo ""
        echo "========================================"
        echo "  按 Enter 启动服务... (Ctrl+C 取消)"
        echo "========================================"
        read -r
        start
        ;;
    *)
        echo -e "${RED}未知命令: $1${NC}"
        show_help
        exit 1
        ;;
esac

#!/bin/bash
# =====================================================
# AI Spring Framework - 数据库初始化脚本（Docker 版本）
# 使用方式：bash init.sh
# =====================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "====================================================="
echo "AI Spring Framework - 数据库初始化"
echo "====================================================="
echo ""

# 检查 MySQL 容器
MYSQL_CONTAINER=$(docker ps --filter "name=ai-framework-mysql" --format "{{.Names}}")

if [ -z "$MYSQL_CONTAINER" ]; then
    echo "❌ MySQL 容器未运行！"
    echo ""
    echo "请先启动 MySQL 容器："
    echo "  docker-compose up -d mysql"
    echo "  或"
    echo "  docker start ai-framework-mysql"
    echo ""
    exit 1
fi

echo "✅ 检测到 MySQL 容器：$MYSQL_CONTAINER"
echo ""

# 获取容器中的 MySQL 密码
MYSQL_PASS="MyNewPass123!"

# 创建数据库
echo "创建数据库..."
docker exec -i $MYSQL_CONTAINER mysql -uroot -p$MYSQL_PASS -e "CREATE DATABASE IF NOT EXISTS ai_framework DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;"

if [ $? -eq 0 ]; then
    echo "✅ 数据库创建成功"
else
    echo "⚠️ 数据库可能已存在"
fi

echo ""

# 执行 Flyway 迁移脚本 V1.0.0
echo "执行初始化脚本..."
docker exec -i $MYSQL_CONTAINER mysql -uroot -p$MYSQL_PASS ai_framework < "$SCRIPT_DIR/migration/V1.0.0__init_database.sql"

if [ $? -eq 0 ]; then
    echo "✅ 表结构创建成功"
else
    echo "❌ 表结构创建失败，请检查错误信息"
    exit 1
fi

echo ""

# 验证字符集
echo "验证字符集配置..."
CHARSET=$(docker exec -i $MYSQL_CONTAINER mysql -uroot -p$MYSQL_PASS ai_framework -N -e "SHOW VARIABLES LIKE 'character_set_database';" | awk '{print $2}')

if [ "$CHARSET" = "utf8mb4" ]; then
    echo "✅ 字符集配置正确：utf8mb4"
else
    echo "⚠️ 字符集配置：$CHARSET"
fi

echo ""
echo "====================================================="
echo "数据库初始化完成！"
echo "====================================================="
echo ""
echo "下一步："
echo "1. 启动应用：mvn spring-boot:run"
echo "2. Flyway 会自动执行后续迁移脚本（索引优化等）"
echo "3. 访问 Swagger: http://localhost:8081/swagger-ui.html"
echo ""
echo "默认管理员账号："
echo "  用户名：admin"
echo "  密码：admin123"
echo ""
echo "📝 注意：Docker exec 显示中文可能乱码，但数据存储正确。"
echo "   应用通过 JDBC 连接时正常显示中文。"
echo ""

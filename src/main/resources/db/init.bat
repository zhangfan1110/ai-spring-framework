@echo off
REM =====================================================
REM AI Spring Framework - 数据库初始化脚本 (Windows)
REM 使用方式：init.bat [mysql_host] [mysql_user] [mysql_password]
REM =====================================================

SET MYSQL_HOST=%1
IF "%MYSQL_HOST%"=="" SET MYSQL_HOST=localhost

SET MYSQL_USER=%2
IF "%MYSQL_USER%"=="" SET MYSQL_USER=root

SET MYSQL_PASS=%3
IF "%MYSQL_PASS%"=="" SET MYSQL_PASS=MyNewPass123!

echo =====================================================
echo AI Spring Framework - 数据库初始化
echo =====================================================
echo MySQL 主机：%MYSQL_HOST%
echo MySQL 用户：%MYSQL_USER%
echo.

REM 检查 MySQL 连接
echo 检查 MySQL 连接...
mysql -h%MYSQL_HOST% -u%MYSQL_USER% -p%MYSQL_PASS% -e "SELECT 1" > nul 2>&1

if errorlevel 1 (
    echo ❌ MySQL 连接失败！请检查：
    echo    1. MySQL 服务是否运行
    echo    2. 用户名密码是否正确
    echo    3. 网络连接是否正常
    echo.
    echo 使用示例：
    echo   init.bat localhost root your_password
    pause
    exit /b 1
)

echo ✅ MySQL 连接成功
echo.

REM 创建数据库
echo 创建数据库...
mysql -h%MYSQL_HOST% -u%MYSQL_USER% -p%MYSQL_PASS% < db\migration\V0.0.1__create_database.sql

if errorlevel 1 (
    echo ⚠️ 数据库可能已存在，继续执行迁移...
) else (
    echo ✅ 数据库创建成功
)

echo.
echo =====================================================
echo 数据库初始化完成！
echo =====================================================
echo.
echo 下一步：
echo 1. 启动应用：mvn spring-boot:run
echo 2. Flyway 会自动执行迁移脚本创建表结构
echo 3. 访问 Swagger: http://localhost:8081/swagger-ui.html
echo.
echo 默认管理员账号：
echo   用户名：admin
echo   密码：admin123
echo.
pause

项目概述

本仓库为 GemRun 后端模版（基于 Spring Boot 多模块架构），包含系统管理、基础设施、会员、商城、工作流、支付、报表、AI 等可选模块。项目采用 MySQL + MyBatis-Plus、Redis、Redisson、Flowable 等中间件，支持多租户、动态数据源、代码生成与在线可视化报表/大屏。

主要模块（后端）
- `dependencies`：集中管理 Maven 依赖版本（BOM）。  
- `framework`：通用基础库（公共工具、异常统一处理、基础注解、starter/autoconfig等）。  
- `server`：后端入口工程（主启动类、全局配置、聚合业务模块）。  
- `module-system`：系统功能（用户/角色/菜单/权限/租户/日志等）。  
- `module-infra`：基础设施（代码生成、文件服务、配置管理、WebSocket、消息队列、监控接入等）。  
- `module-member`：会员中心（会员管理/等级/积分等）。  
- `module-mall`：商城核心（商品、库存、订单、促销等）。  
- `module-pay`：支付接入（支付订单、退款、回调处理、多渠道适配）。  
- `module-bpm`：工作流（基于 Flowable，支持 BPMN/轻设计器/会签/驳回等）。  
- `module-report`：报表与大屏（报表设计器、大屏拖拽预览与导出）。  
- `module-crm` / `module-erp` / `module-iot` / `module-ai`：分别提供 CRM、ERP、物联网、AI 能力（按需启用）。

技术栈与关键点
- Java 8 + Spring Boot 2.7.x（仓库有 JDK17 分支的情况，按 README 标注选择分支）。  
- MyBatis-Plus、动态数据源、多租户支持、Redis + Redisson、Flowable 工作流。  
- 单元测试：JUnit + Mockito。  
- 文档/监控：springdoc/Swagger、Screw（数据库文档）、SkyWalking/BootAdmin（接入示例）。

前置环境（开发机）
- JDK 8（或项目指定的 JDK 版本）  
- Maven 3.6+  
- MySQL 5.7 / 8.0+（建议使用 Docker 容器快速启动）  
- Redis（缓存/幂等/分布式锁）  
- 可选：MinIO（文件服务）、消息队列（Rabbit/Kafka）等

快速启动（开发模式，推荐）
1. 克隆仓库并进入目录：
   git clone <repo-url>
   cd /path/to/gemrun-base

2. 本地安装项目各模块依赖管理（推荐，确保 import BOM 可解析）：
   mvn -DskipTests clean install
   说明：该步骤会把 `dependencies` 等模块安装到本地仓库，避免启动时出现 import POM 不可解析的问题。

3. 启动依赖服务（推荐用 Docker-compose，若仓库 `script` 提供了 docker-compose，优先使用）：
   - Docker 示例（MySQL + Redis）：
     docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=base_db -e MYSQL_USER=base_db -e MYSQL_PASSWORD=your_password -p 3306:3306 mysql:5.7
     docker run -d --name redis -p 6379:6379 redis:6

4. 配置 `server` 的开发配置：
   - 打开 `server/src/main/resources/application-dev.yml`（或 `application.yml`），根据本地环境修改 datasource、redis、minio 等连接信息。  
   - 常见关键项：`spring.datasource.*`、`spring.datasource.dynamic.datasource.master.*`、`spring.redis.*`、`spring.profiles.active` 等。

5. 初始化数据库（如果需要）：
   - 在 `sql` 目录查找建表与初始化脚本，按顺序执行到 MySQL（建议先创建数据库并授予用户权限）：  
     CREATE DATABASE IF NOT EXISTS base_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
     CREATE USER 'base_db'@'%' IDENTIFIED BY 'your_password';
     GRANT ALL PRIVILEGES ON base_db.* TO 'base_db'@'%';
     FLUSH PRIVILEGES;

6. 以开发模式运行后端（两种常用方式）
- 在 `server` 目录用 Maven 运行（推荐调试/热重载）：
  cd server
  mvn spring-boot:run -Dspring-boot.run.profiles=dev
  说明：若在根目录使用 reactor 运行，可能出现插件前缀解析或主类查找的问题，建议在 `server` 目录执行或先 `mvn clean install`。

- 使用已打包的 jar 运行：
  mvn -DskipTests clean package
  java -jar server/target/server.jar --spring.profiles.active=dev

如何克隆（包含子模块 web-ui）

- 推荐一次性拉取主仓库和所有子模块（建议开发者使用此命令）：
  git clone --recurse-submodules https://github.com/surpass999/gemrun-java.git

- 若已经克隆主仓库，可以手动初始化并更新子模块：
  git clone https://github.com/surpass999/gemrun-java.git
  cd gemrun-java
  git submodule update --init --recursive

常见故障与排查
- 启动时报 "No plugin found for prefix 'spring-boot'"：可用完整插件坐标运行或在项目根先 `mvn -DskipTests clean install`。示例（完整坐标）：
  mvn -pl server -am org.springframework.boot:spring-boot-maven-plugin:2.7.18:run -Dspring-boot.run.profiles=dev

- 启动时报 "Non-resolvable import POM" 或依赖版本缺失：说明 `dependencies` BOM 未被本地解析，先在仓库根执行 `mvn -DskipTests clean install` 安装模块到本地仓库。  

- 数据库 Access denied（应用日志类似 "Access denied for user 'base_db'@'localhost'"）：  
  1) 确认 `application-dev.yml` 中的 `username/password/url` 与 MySQL 实际一致；  
  2) 在 MySQL 中为 `base_db` 创建并授权（参见上文 SQL）；  
  3) 有时需要同时授权 `'base_db'@'localhost'` 和 `'base_db'@'127.0.0.1'` 或使用 `'base_db'@'%'`。

- 如果项目使用动态数据源或 Flowable，确保对应的表/schema 已初始化（查看 `sql` 目录或文档中的初始化脚本）。

开发建议
- 在 IDE（如 IntelliJ IDEA）导入 Maven 项目后，使用 `server` 的 `BaseServerApplication` 主类运行，设置 VM 参数或 Program args：`-Dspring.profiles.active=dev`，并启用 Build -> "Build project automatically" 与 DevTools 实现热重载。  
- 若项目模块较多、启动慢，可先只编译必要模块（`mvn -pl module-infra,module-system,module-member -am package`），再运行 `server`。

贡献与文档
- 仓库内 `docs/` 包含模块说明与使用说明（请查看 `docs/项目模块说明.md`）。  
- 如需接入第三方服务（短信、微信、支付、MinIO 等），请在 `server` 的配置文件中填写对应的凭据并根据 `docs/` 中的示例完成对接。

联系我们
- 若遇到无法解决的问题，可在仓库 Issue 中贴出日志与配置（注意脱敏密码），或者查阅项目在线文档与演示地址（若有）。

（已替换旧内容 — README.md 现包含项目简介、模块说明、开发依赖、快速启动与常见故障排查步骤。）


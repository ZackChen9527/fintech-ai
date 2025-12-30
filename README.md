金融行业精准获客系统
项目概述
这是一个基于Spring Boot的金融行业精准获客系统，通过数据抓取、AI分析和任务调度三个核心模块，自动化识别和评估潜在金融客户。系统专门针对"跨境支付"和"海外借贷"两类金融业务进行智能分析，帮助企业精准定位高价值客户。

系统架构
text
金融精准获客系统
├── 数据抓取模块 (Crawler Module)
│   ├── LinkedIn公司信息抓取
│   ├── 批量数据采集
│   └── 数据清洗与存储
├── AI分析模块 (Analysis Module)
│   ├── DeepSeek API集成
│   ├── 业务类型识别（跨境支付/海外借贷）
│   ├── 付费意愿评分（1-10分）
│   └── 高并发限流与重试
└── 任务调度模块 (Scheduler Module)
├── Quartz定时任务
├── 自动化获客流程
└── 批量分析与报告
技术栈
后端框架: Spring Boot 3.1.5

AI集成: Spring AI (DeepSeek API)

数据存储: MySQL 8.0, Redis

任务调度: Quartz

构建工具: Maven

Java版本: 17

快速开始
环境准备
1. 系统要求
   bash
# 检查Java版本
java -version  # 需要 Java 17+

# 检查Maven
mvn -version   # 需要 Maven 3.6+

# 检查MySQL
mysql --version

# 检查Redis
redis-cli ping
2. 克隆项目
   bash
   git clone <repository-url>
   cd financial-lead-system
3. 数据库初始化
   sql
   -- 创建数据库
   CREATE DATABASE finance_lead CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户（可选）
CREATE USER 'finance_user'@'%' IDENTIFIED BY 'SecurePass123!';
GRANT ALL PRIVILEGES ON finance_lead.* TO 'finance_user'@'%';
FLUSH PRIVILEGES;
4. 环境变量配置
   bash
# 设置DeepSeek API密钥（必需）
export DEEPSEEK_API_KEY="sk-your-actual-api-key-here"

# 数据库配置（如果与默认值不同）
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/finance_lead"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="password123"

# Redis配置（如果与默认值不同）
export SPRING_DATA_REDIS_HOST="localhost"
export SPRING_DATA_REDIS_PORT="6379"

# 激活开发环境
export SPRING_PROFILES_ACTIVE="dev"
5. 配置文件检查
   确保以下配置文件存在：

src/main/resources/application.yml - 主配置文件

src/main/resources/application-dev.yml - 开发环境配置

src/main/resources/application-test.yml - 测试环境配置

构建与运行
1. 编译项目
   bash
# 清理并编译
mvn clean compile

# 跳过测试打包
mvn clean package -DskipTests
2. 运行项目
   bash
# 方式1：使用Maven插件（开发环境）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 方式2：运行打包的JAR
java -jar target/financial-lead-system-1.0.0.jar --spring.profiles.active=dev

# 方式3：生产环境
java -jar target/financial-lead-system-1.0.0.jar --spring.profiles.active=prod
3. 验证启动
   bash
# 检查健康状态
curl http://localhost:8080/api/analysis/health
# 预期返回: "Analysis service is healthy"

curl http://localhost:8080/api/crawler/health
# 预期返回: "Crawler service is healthy"

# 检查管理端点
curl http://localhost:8080/manage/health
核心功能使用
1. 数据抓取功能
   抓取单个LinkedIn公司
   bash
   curl -X POST "http://localhost:8080/api/crawler/linkedin/single?linkedinUrl=https://www.linkedin.com/company/example" \
   -H "Content-Type: application/json"
   批量抓取公司
   bash
   curl -X POST "http://localhost:8080/api/crawler/linkedin/batch" \
   -H "Content-Type: application/json" \
   -d '[
   "https://www.linkedin.com/company/alipay",
   "https://www.linkedin.com/company/stripe",
   "https://www.linkedin.com/company/transferwise"
   ]'
   关键词搜索抓取
   bash
   curl -X GET "http://localhost:8080/api/crawler/linkedin/search?keyword=跨境支付&limit=10"
2. AI分析功能
   分析单个公司
   bash
   curl -X POST "http://localhost:8080/api/analysis/single" \
   -H "Content-Type: application/json" \
   -d '{
   "companyName": "跨境支付科技公司",
   "description": "我们是一家专注于为跨境电商提供跨境支付解决方案的金融科技公司，支持多种货币结算，服务覆盖全球100多个国家和地区。",
   "website": "https://example.com",
   "location": "上海",
   "companySize": "200-500人",
   "industry": "金融科技"
   }'
   批量分析
   bash
   curl -X POST "http://localhost:8080/api/analysis/batch" \
   -H "Content-Type: application/json" \
   -d '[
   {
   "companyName": "跨境支付公司A",
   "description": "专业跨境支付服务提供商"
   },
   {
   "companyName": "海外借贷公司B",
   "description": "专注于海外借贷业务的金融机构"
   }
   ]'
3. 任务管理功能
   执行批量分析任务
   bash
   curl -X POST "http://localhost:8080/api/analysis/task/batch"
   获取任务状态
   bash
   curl -X GET "http://localhost:8080/api/analysis/task/status"
   清理旧数据
   bash
# 清理30天前的分析结果
curl -X DELETE "http://localhost:8080/api/analysis/task/cleanup/30"
自动化工作流
定时任务配置
系统预配置了以下定时任务（可在application.yml中调整）：

任务名称	Cron表达式	执行时间	功能说明
潜在客户生成	0 0 2 * * ?	每天02:00	生成新的潜在客户
LinkedIn抓取	0 0 3 * * ?	每天03:00	抓取LinkedIn数据
批量分析	0 0 4 * * ?	每天04:00	分析未分析的公司
重新分析	0 0 5 * * ?	每天05:00	重新分析低质量结果
实时分析	0 */30 * * * ?	每30分钟	增量分析新数据
完整获客流程示例
bash
# 1. 执行完整获客流程脚本
./scripts/full-pipeline.sh

# 脚本内容示例：
#!/bin/bash
# 步骤1: 抓取跨境支付相关公司
curl -X GET "http://localhost:8080/api/crawler/linkedin/search?keyword=跨境支付&limit=20"

# 等待数据抓取完成
sleep 30

# 步骤2: 批量分析新抓取的公司
curl -X POST "http://localhost:8080/api/analysis/task/batch"

# 步骤3: 获取高分客户列表
mysql -u root -p finance_lead -e "
SELECT c.name, a.payment_willingness_score, a.confidence
FROM company c
JOIN analysis_result a ON c.id = a.company_id
WHERE a.payment_willingness_score >= 7
ORDER BY a.payment_willingness_score DESC;"
配置说明
关键配置项
1. DeepSeek API配置
   yaml
   spring:
   ai:
   openai:
   api-key: ${DEEPSEEK_API_KEY}  # 通过环境变量设置
   base-url: https://api.deepseek.com
   chat:
   options:
   model: deepseek-chat
   temperature: 0.3  # 控制创造性，较低值更稳定
   max-tokens: 1000  # 最大响应长度
2. 限流配置（高并发场景）
   yaml
   finance:
   analysis:
   rate-limit:
   tokens-per-second: 5    # 每秒处理请求数
   burst-capacity: 10      # 突发容量
   timeout-millis: 1000    # 等待超时时间
3. 重试机制
   yaml
   finance:
   analysis:
   retry:
   max-attempts: 3         # 最大重试次数
   initial-interval: 1000  # 初始重试间隔(ms)
   multiplier: 2.0         # 指数退避乘数
   环境特定配置
   开发环境 (application-dev.yml)
   yaml
   spring:
   datasource:
   url: jdbc:mysql://localhost:3306/finance_lead_dev
   jpa:
   show-sql: true  # 显示SQL语句
   data:
   redis:
   host: localhost
   port: 6379
   生产环境 (application-prod.yml)
   yaml
   spring:
   jpa:
   hibernate:
   ddl-auto: validate  # 生产环境禁用自动更新表结构
   show-sql: false
   quartz:
   job-store-type: jdbc  # 使用数据库存储任务状态
   API文档
   分析模块API
   端点	方法	参数	说明
   /api/analysis/single	POST	AnalysisRequest JSON	分析单个公司
   /api/analysis/batch	POST	AnalysisRequest[] JSON	批量分析公司
   /api/analysis/health	GET	无	服务健康检查
   /api/analysis/rate-limit/status	GET	无	限流状态查询
   /api/analysis/cache/clear	POST	无	清空分析缓存
   数据抓取API
   端点	方法	参数	说明
   /api/crawler/linkedin/single	POST	linkedinUrl	抓取单个公司
   /api/crawler/linkedin/batch	POST	URL数组JSON	批量抓取
   /api/crawler/linkedin/search	GET	keyword, limit	关键词搜索
   任务管理API
   端点	方法	参数	说明
   /api/analysis/task/batch	POST	无	执行批量分析
   /api/analysis/task/status	GET	无	获取任务状态
   /api/analysis/task/reanalyze	POST	无	重新分析低质量结果
   /api/analysis/task/industry/{industry}	POST	industry, limit	按行业分析
   /api/analysis/task/cleanup/{days}	DELETE	days	清理旧数据
   数据结构
   公司表 (company)
   sql
   CREATE TABLE company (
   id BIGINT PRIMARY KEY AUTO_INCREMENT,
   name VARCHAR(200) NOT NULL,          -- 公司名称
   description TEXT,                    -- 公司描述
   industry VARCHAR(100),               -- 所属行业
   location VARCHAR(100),               -- 所在地区
   linkedin_url VARCHAR(500),           -- LinkedIn链接
   is_active BOOLEAN DEFAULT TRUE,      -- 是否活跃
   created_at DATETIME,                 -- 创建时间
   -- ... 其他字段
   );
   分析结果表 (analysis_result)
   sql
   CREATE TABLE analysis_result (
   id BIGINT PRIMARY KEY AUTO_INCREMENT,
   company_id BIGINT NOT NULL,          -- 关联公司ID
   business_types VARCHAR(500),         -- 业务类型
   payment_willingness_score INT,       -- 付费意愿评分(1-10)
   confidence DOUBLE,                   -- 置信度(0-1)
   analysis_reason TEXT,                -- 分析理由
   success BOOLEAN DEFAULT TRUE,        -- 是否成功
   analysis_time DATETIME,              -- 分析时间
   -- ... 其他字段
   );
   故障排除
   常见问题
1. 应用启动失败
   bash
# 检查错误日志
tail -f logs/finance-lead.log

# 常见问题：
# - 数据库连接失败：检查MySQL服务状态
# - Redis连接失败：检查Redis服务状态
# - 端口冲突：修改server.port配置
2. DeepSeek API调用失败
   bash
# 验证API密钥
echo $DEEPSEEK_API_KEY

# 测试API连接
curl -X POST https://api.deepseek.com/v1/chat/completions \
-H "Authorization: Bearer $DEEPSEEK_API_KEY" \
-H "Content-Type: application/json" \
-d '{"model":"deepseek-chat","messages":[{"role":"user","content":"Hello"}]}'
3. 数据库连接问题
   bash
# 测试数据库连接
mysql -h localhost -u root -p -e "SHOW DATABASES;"

# 检查数据库用户权限
mysql -u root -p -e "SHOW GRANTS FOR 'finance_user'@'%';"
4. Redis连接问题
   bash
# 测试Redis连接
redis-cli ping

# 检查Redis配置
grep -A5 "spring.data.redis" src/main/resources/application.yml
日志查看
bash
# 查看应用主日志
tail -f logs/finance-lead.log

# 查看分析模块日志
tail -f logs/analysis.log

# 查看错误日志
tail -f logs/error.log

# 按级别过滤日志
grep "ERROR" logs/finance-lead.log
grep "ANALYSIS" logs/finance-lead.log
监控与维护
系统监控
bash
# 1. 健康检查端点
curl http://localhost:8080/manage/health

# 2. 指标监控
curl http://localhost:8080/manage/metrics | grep -E "(jvm|system|http)"

# 3. 自定义状态检查
curl http://localhost:8080/api/analysis/task/status
curl http://localhost:8080/api/analysis/rate-limit/status
数据维护
bash
# 1. 定期清理脚本
# 清理30天前的分析结果
curl -X DELETE "http://localhost:8080/api/analysis/task/cleanup/30"

# 2. 数据备份脚本
mysqldump -u root -p finance_lead > backup_$(date +%Y%m%d).sql

# 3. 统计数据查询
mysql -u root -p finance_lead -e "
SELECT
COUNT(*) as total_companies,
AVG(payment_willingness_score) as avg_score,
SUM(CASE WHEN payment_willingness_score >= 7 THEN 1 ELSE 0 END) as high_score_count
FROM analysis_result
WHERE success = 1;"
开发指南
项目结构
text
financial-lead-system/
├── src/main/java/com/finance/lead/
│   ├── FinancialLeadApplication.java     # 应用入口
│   ├── config/                          # 配置类
│   ├── module/                          # 业务模块
│   │   ├── crawler/                     # 数据抓取模块
│   │   ├── analysis/                    # AI分析模块
│   │   └── scheduler/                   # 任务调度模块
│   ├── repository/                      # 数据访问层
│   └── common/                          # 公共工具类
├── src/main/resources/
│   ├── application.yml                  # 主配置文件
│   ├── application-dev.yml              # 开发环境配置
│   └── logback-spring.xml               # 日志配置
└── pom.xml                             # Maven配置
添加新的业务分析策略
创建策略类：

java
@Component
public class NewBusinessStrategy implements AnalysisStrategy {
@Override
public boolean supports(BusinessType businessType) {
return businessType == BusinessType.NEW_TYPE;
}

    @Override
    public AnalysisResult analyze(String description) {
        // 实现分析逻辑
    }
}
在配置中添加关键词：

yaml
finance:
analysis:
strategy:
new-business:
keywords:
- "关键词1"
- "关键词2"
weight: 1.0
扩展数据源
实现CrawlerService接口：

java
@Service
public class NewDataSourceCrawler implements CrawlerService {
@Override
public CompanyProfileDTO crawlCompany(String identifier) {
// 实现新数据源的抓取逻辑
}

    @Override
    public boolean supports(String url) {
        return url.contains("new-data-source.com");
    }
}
部署指南
Docker部署
dockerfile
# Dockerfile示例
FROM openjdk:17-jdk-slim
COPY target/financial-lead-system-1.0.0.jar app.jar
ENV DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY}
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
Kubernetes部署
yaml
# deployment.yaml示例
apiVersion: apps/v1
kind: Deployment
metadata:
name: financial-lead-system
spec:
replicas: 3
template:
spec:
containers:
- name: app
image: financial-lead-system:1.0.0
env:
- name: DEEPSEEK_API_KEY
valueFrom:
secretKeyRef:
name: api-secrets
key: deepseek-api-key
ports:
- containerPort: 8080
许可证
本项目采用 MIT 许可证。详见 LICENSE 文件。

支持与贡献
问题反馈
如遇到问题，请：

查看日志文件：logs/finance-lead.log

检查配置是否正确

在项目Issue中提交问题

贡献代码
Fork本仓库

创建功能分支

提交更改

发起Pull Request

免责声明
本系统仅供学习和研究使用，在实际业务中应用时请确保：

遵守LinkedIn的使用条款

获得必要的API访问权限

遵守数据隐私和版权法律法规

对分析结果进行人工验证和审核

系统状态: ✅ 运行正常
最后更新: 2024年1月
版本: 1.0.0
维护者: 金融科技团队
# OKX Trading 智能回测系统

## 项目概述
本项目是一个基于Java Spring Boot开发的智能加密货币交易策略回测系统，集成了AI策略生成、历史数据回测和性能分析功能。系统支持通过自然语言描述自动生成交易策略，并对历史K线数据进行策略回测，提供详细的回测分析结果。

## 🚀 核心功能

### AI策略生成
- **智能策略创建**：基于DeepSeek API，通过自然语言描述自动生成Ta4j交易策略
- **动态编译加载**：使用Janino编译器实时编译策略代码并动态加载
- **策略管理**：支持策略的创建、更新、删除和查询
- **热更新**：无需重启服务即可加载新策略

### 回测分析
- **历史数据获取**：从OKX交易所API获取历史K线数据
- **多策略支持**：支持SMA、布林带、RSI、成交量突破等多种技术指标策略
- **性能分析**：计算总收益率、夏普比率、胜率、最大回撤等关键指标
- **详细记录**：保存完整的交易记录和回测汇总信息

### 数据管理
- **数据存储**：MySQL数据库存储策略信息、回测结果和交易记录
- **缓存优化**：Redis缓存提升数据访问性能
- **API接口**：完整的RESTful API接口支持

## 🛠 技术栈
- **后端框架**：Spring Boot 2.7.8
- **编程语言**：Java 8
- **数据库**：MySQL 8.0 + Redis 6.0+
- **技术分析**：Ta4j 0.14 技术分析库
- **AI集成**：DeepSeek API
- **动态编译**：Janino 编译器
- **HTTP客户端**：OkHttp3 4.9.3
- **WebSocket**：实时数据获取
- **容器化**：Docker + Docker Compose

## 数据库表结构

### 回测交易明细表 (backtest_trade)
| 字段名 | 类型 | 描述 |
|-------|------|------|
| id | BIGINT | 主键ID |
| backtest_id | BIGINT | 回测ID |
| index | INT | 交易索引号 |
| type | VARCHAR(10) | 交易类型(BUY/SELL) |
| entry_time | DATETIME | 入场时间 |
| entry_price | DECIMAL(20,8) | 入场价格 |
| entry_amount | DECIMAL(20,8) | 入场金额 |
| exit_time | DATETIME | 出场时间 |
| exit_price | DECIMAL(20,8) | 出场价格 |
| exit_amount | DECIMAL(20,8) | 出场金额 |
| profit | DECIMAL(20,8) | 交易利润 |
| profit_percentage | DECIMAL(10,4) | 交易利润百分比 |
| closed | BOOLEAN | 是否已平仓 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 回测汇总表 (backtest_summary)
| 字段名 | 类型 | 描述 |
|-------|------|------|
| id | BIGINT | 主键ID |
| symbol | VARCHAR(20) | 交易对 |
| interval | VARCHAR(10) | K线周期 |
| strategy_type | VARCHAR(50) | 策略类型 |
| start_time | DATETIME | 回测开始时间 |
| end_time | DATETIME | 回测结束时间 |
| initial_amount | DECIMAL(20,8) | 初始资金 |
| final_amount | DECIMAL(20,8) | 最终资金 |
| total_profit | DECIMAL(20,8) | 总利润 |
| total_return | DECIMAL(10,4) | 总收益率 |
| win_rate | DECIMAL(10,4) | 胜率 |
| profit_factor | DECIMAL(10,4) | 盈亏比 |
| sharpe_ratio | DECIMAL(10,4) | 夏普比率 |
| max_drawdown | DECIMAL(10,4) | 最大回撤 |
| trade_count | INT | 交易次数 |
| parameters | TEXT | 策略参数(JSON格式) |
| status | VARCHAR(20) | 回测状态 |
| error_message | VARCHAR(500) | 错误信息 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

## 📚 API接口说明

### AI策略生成接口

#### 生成AI策略
- **URL**: `/api/api/backtest/ta4j/generate-strategy`
- **方法**: POST
- **请求体**: 策略描述文本(String)
- **示例**: `"基于RSI超买超卖策略，当RSI低于30时买入，高于70时卖出"`
- **返回**: 生成的策略信息，包含策略ID、名称、分类、参数等

#### 更新策略
- **URL**: `/api/api/backtest/ta4j/update-strategy/{strategyId}`
- **方法**: PUT
- **请求体**: 新的策略描述文本
- **返回**: 更新后的策略信息

#### 删除策略
- **URL**: `/api/api/backtest/ta4j/delete-strategy/{strategyId}`
- **方法**: DELETE
- **返回**: 删除结果

#### 查询策略列表
- **URL**: `/api/api/backtest/ta4j/strategies`
- **方法**: GET
- **参数**: 
  - page: 页码(默认0)
  - size: 每页大小(默认10)
- **返回**: 分页的策略列表

### 回测执行接口

#### 执行Ta4j回测
- **URL**: `/api/api/backtest/ta4j/run`
- **方法**: GET
- **参数**:
  - symbol: 交易对(如"BTC-USDT")
  - interval: K线周期(如"1h", "4h", "1d")
  - strategyType: 策略类型或策略ID
  - startTime: 回测开始时间(yyyy-MM-dd HH:mm:ss)
  - endTime: 回测结束时间(yyyy-MM-dd HH:mm:ss)
  - initialAmount: 初始资金
  - strategyParams: 策略参数(可选)
  - saveResult: 是否保存结果(true/false)
- **返回**: 回测结果，包含交易记录和性能指标

### 数据查询接口

#### 获取回测交易记录
- **URL**: `/api/v1/backtest/{backtestId}/trades`
- **方法**: GET
- **参数**:
  - backtestId: 回测ID
  - page: 页码
  - size: 每页记录数
- **返回**: 分页的交易记录列表

#### 获取回测汇总信息
- **URL**: `/api/v1/backtest/{backtestId}`
- **方法**: GET
- **参数**:
  - backtestId: 回测ID
- **返回**: 回测汇总信息

#### 获取所有回测记录
- **URL**: `/api/v1/backtest/list`
- **方法**: GET
- **参数**:
  - page: 页码
  - size: 每页记录数
  - symbol: 交易对(可选)
  - strategyType: 策略类型(可选)
- **返回**: 分页的回测记录列表

## 💡 使用示例

### AI策略生成示例

#### 1. 生成RSI策略
```bash
curl -X POST "http://localhost:8088/api/api/backtest/ta4j/generate-strategy" \
  -H "Content-Type: application/json" \
  -d '"基于RSI超买超卖策略，当RSI低于30时买入，高于70时卖出"'
```

#### 2. 生成成交量突破策略
```bash
curl -X POST "http://localhost:8088/api/api/backtest/ta4j/generate-strategy" \
  -H "Content-Type: application/json" \
  -d '"基于成交量突破策略，当成交量超过20日平均成交量1.5倍时买入，低于0.8倍时卖出"'
```

#### 3. 生成双均线交叉策略
```bash
curl -X POST "http://localhost:8088/api/api/backtest/ta4j/generate-strategy" \
  -H "Content-Type: application/json" \
  -d '"双均线交叉策略，当短期均线上穿长期均线时买入，下穿时卖出"'
```

### 回测执行示例

#### 1. 使用AI生成的策略进行回测
```bash
curl "http://localhost:8088/api/api/backtest/ta4j/run?symbol=BTC-USDT&interval=1h&strategyType=AI_RSI_001&startTime=2023-01-01%2000:00:00&endTime=2023-01-31%2023:59:59&initialAmount=10000&saveResult=true"
```

#### 2. 使用传统SMA策略回测
```bash
curl "http://localhost:8088/api/api/backtest/ta4j/run?symbol=BTC-USDT&interval=1h&strategyType=SMA&startTime=2023-01-01%2000:00:00&endTime=2023-01-31%2023:59:59&initialAmount=10000&strategyParams=5,20&saveResult=true"
```

#### 3. 使用布林带策略回测
```bash
curl "http://localhost:8088/api/api/backtest/ta4j/run?symbol=ETH-USDT&interval=4h&strategyType=BOLLINGER&startTime=2023-01-01%2000:00:00&endTime=2023-01-31%2023:59:59&initialAmount=10000&strategyParams=20,2.0&saveResult=true"
```

### PowerShell示例

#### 生成AI策略
```powershell
Invoke-RestMethod -Uri 'http://localhost:8088/api/api/backtest/ta4j/generate-strategy' -Method POST -Body '"基于MACD指标的交易策略，当MACD线上穿信号线时买入，下穿时卖出"' -ContentType 'application/json; charset=utf-8'
```

#### 执行回测
```powershell
Invoke-RestMethod -Uri 'http://localhost:8088/api/api/backtest/ta4j/run?symbol=BTC-USDT&interval=1h&strategyType=AI_MACD_001&startTime=2023-01-01%2000:00:00&endTime=2023-01-31%2023:59:59&initialAmount=10000&saveResult=true' -Method GET
```

## ⚙️ 安装与配置

### 环境要求
- **JDK 8+**
- **Maven 3.6+**
- **MySQL 8.0+**
- **Redis 6.0+**
- **Docker & Docker Compose** (可选)

### 数据库初始化

#### 1. 创建数据库
```sql
CREATE DATABASE okx_trading CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 2. 添加AI策略支持字段
```sql
USE okx_trading;
ALTER TABLE strategy_info ADD COLUMN source_code TEXT COMMENT '策略源代码，存储lambda函数的序列化字符串';
```

### 配置文件

#### application.yml 配置示例
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/okx_trading?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password: ${MYSQL_PASSWORD:Password123?}
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect

# OKX API配置
okx:
  api:
    key: ${OKX_API_KEY:your_api_key}
    secret: ${OKX_API_SECRET:your_api_secret}
    passphrase: ${OKX_API_PASSPHRASE:your_passphrase}
    base-url: https://www.okx.com

# DeepSeek API配置
deepseek:
  api:
    key: ${DEEPSEEK_API_KEY:your_deepseek_api_key}
    url: https://api.deepseek.com/v1/chat/completions

# 代理配置(适用于中国区域)
proxy:
  host: ${PROXY_HOST:localhost}
  port: ${PROXY_PORT:10809}
  enabled: ${PROXY_ENABLED:false}

server:
  port: 8088
```

### 环境变量配置

创建 `.env` 文件：
```bash
# 数据库配置
MYSQL_PASSWORD=your_mysql_password

# OKX API配置
OKX_API_KEY=your_okx_api_key
OKX_API_SECRET=your_okx_api_secret
OKX_API_PASSPHRASE=your_okx_passphrase

# DeepSeek API配置
DEEPSEEK_API_KEY=your_deepseek_api_key

# 代理配置
PROXY_HOST=localhost
PROXY_PORT=10809
PROXY_ENABLED=false
```

### 构建与运行

#### 方式一：Maven 直接运行
```bash
# 克隆仓库
git clone https://github.com/ralph-wren/okx-trading.git

# 进入项目目录
cd okx-trading

# 编译打包
mvn clean package -DskipTests

# 运行应用
mvn spring-boot:run

# 或者运行jar包
java -jar target/okx-trading-0.0.1-SNAPSHOT.jar
```

#### 方式二：Docker Compose 运行
```bash
# 启动所有服务(包括MySQL和Redis)
docker-compose up -d

# 查看日志
docker-compose logs -f okx-trading

# 停止服务
docker-compose down
```

#### 方式三：Docker 单独运行
```bash
# 构建镜像
docker build -t okx-trading .

# 运行容器
docker run -d -p 8088:8088 \
  -e MYSQL_PASSWORD=your_password \
  -e DEEPSEEK_API_KEY=your_api_key \
  --name okx-trading okx-trading
```

## 🌟 项目特色

### AI驱动的策略生成
- 支持中文自然语言描述
- 自动生成符合Ta4j规范的策略代码
- 智能参数提取和默认值设置
- 策略分类和描述自动生成

### 高性能回测引擎
- 基于Ta4j 0.14技术分析库
- 支持多种技术指标组合
- 详细的性能指标计算
- 完整的交易记录追踪

### 灵活的架构设计
- 动态策略加载机制
- 热更新支持，无需重启
- 模块化设计，易于扩展
- RESTful API接口

## 📁 项目结构

```
okx-trading/
├── src/main/java/com/okx/trading/
│   ├── controller/          # REST API控制器
│   │   ├── BacktestController.java
│   │   └── Ta4jBacktestController.java
│   ├── service/            # 业务逻辑服务
│   │   ├── DeepSeekApiService.java    # AI策略生成
│   │   ├── Ta4jBacktestService.java   # 回测服务
│   │   └── StrategyFactory.java       # 策略工厂
│   ├── entity/             # 数据库实体
│   │   ├── StrategyInfo.java
│   │   ├── BacktestSummary.java
│   │   └── BacktestTrade.java
│   ├── repository/         # 数据访问层
│   ├── config/            # 配置类
│   └── util/              # 工具类
├── src/main/resources/
│   ├── application.yml    # 主配置文件
│   └── static/           # 静态资源
├── docker/               # Docker相关文件
├── logs/                 # 日志目录
├── strategy_test_results/ # 测试结果
├── README_AI_STRATEGY.md  # AI策略详细文档
├── README_BACKTEST.md     # 回测功能详细文档
└── docker-compose.yml     # Docker编排文件
```

## 🔧 支持的策略类型

### 传统技术指标策略
- **SMA**: 简单移动平均线交叉策略
- **BOLLINGER**: 布林带策略
- **RSI**: 相对强弱指数策略
- **MACD**: 指数平滑移动平均线策略

### AI生成策略
- **趋势策略**: 基于均线、趋势线的策略
- **震荡策略**: 基于RSI、KDJ等震荡指标
- **突破策略**: 基于成交量、价格突破
- **组合策略**: 多指标组合的复合策略

## 📊 性能指标

系统计算以下关键性能指标：

- **总收益率**: 策略的总体收益表现
- **年化收益率**: 按年计算的收益率
- **夏普比率**: 风险调整后的收益率
- **最大回撤**: 策略的最大亏损幅度
- **胜率**: 盈利交易占总交易的比例
- **盈亏比**: 平均盈利与平均亏损的比值
- **交易次数**: 策略执行的总交易次数
- **平均持仓时间**: 每笔交易的平均持续时间

## 🚀 快速开始

### 1. 启动服务
```bash
# 使用Docker Compose快速启动
docker-compose up -d

# 或使用Maven启动
mvn spring-boot:run
```

### 2. 生成第一个AI策略
```bash
curl -X POST "http://localhost:8088/api/api/backtest/ta4j/generate-strategy" \
  -H "Content-Type: application/json" \
  -d '"当RSI指标低于30时买入，高于70时卖出的超买超卖策略"'
```

### 3. 执行回测
```bash
curl "http://localhost:8088/api/api/backtest/ta4j/run?symbol=BTC-USDT&interval=1h&strategyType=AI_RSI_001&startTime=2023-01-01%2000:00:00&endTime=2023-01-31%2023:59:59&initialAmount=10000&saveResult=true"
```

## 📚 相关文档

- [AI策略生成详细文档](README_AI_STRATEGY.md)
- [回测功能详细文档](README_BACKTEST.md)
- [Docker部署文档](README-DOCKER.md)

## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进本项目！

### 贡献流程
1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范
- 遵循Java编码规范
- 添加必要的单元测试
- 更新相关文档
- 确保代码通过所有测试

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 🙏 致谢

- [Ta4j](https://github.com/ta4j/ta4j) - 优秀的技术分析库
- [DeepSeek](https://www.deepseek.com/) - 强大的AI代码生成能力
- [Spring Boot](https://spring.io/projects/spring-boot) - 优秀的Java框架
- [OKX](https://www.okx.com/) - 可靠的交易所API

---

**⭐ 如果这个项目对你有帮助，请给个Star支持一下！**

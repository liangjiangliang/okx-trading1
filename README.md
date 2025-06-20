# OKX Trading 智能回测系统

## 📖 项目概述
本项目是一个基于Java Spring Boot开发的智能加密货币交易策略回测系统，集成了AI策略生成、历史数据回测和性能分析功能。系统支持通过自然语言描述自动生成交易策略，并对历史K线数据进行策略回测，提供详细的回测分析结果。

## 🚀 核心功能

### AI策略生成
- **智能策略创建**：基于DeepSeek API，通过自然语言描述自动生成Ta4j交易策略
- **动态编译加载**：使用Janino编译器实时编译策略代码并动态加载
- **策略管理**：支持策略的创建、更新、删除和查询
- **热更新**：无需重启服务即可加载新策略

### 回测分析
- **历史数据获取**：从OKX交易所API获取历史K线数据，支持智能时间边界调整
- **多策略支持**：支持SMA、布林带、RSI、MACD、成交量突破等多种技术指标策略
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

## 📊 数据库表结构

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

### 策略信息表 (strategy_info)
| 字段名 | 类型 | 描述 |
|-------|------|------|
| id | BIGINT | 主键ID |
| strategy_code | VARCHAR(50) | 策略代码 |
| strategy_name | VARCHAR(100) | 策略名称 |
| description | TEXT | 策略描述 |
| source_code | TEXT | 策略源代码(AI生成的lambda函数) |
| category | VARCHAR(50) | 策略分类 |
| params_desc | VARCHAR(255) | 参数描述 |
| default_params | VARCHAR(255) | 默认参数 |
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

#### 批量生成策略
- **URL**: `/api/api/backtest/ta4j/batch-generate-strategies`
- **方法**: POST
- **请求体**: 策略描述数组
- **返回**: 批量生成结果

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

#### 批量回测
- **URL**: `/api/api/backtest/ta4j/run-all`
- **方法**: GET
- **参数**:
  - startTime: 回测开始时间
  - endTime: 回测结束时间
  - initialAmount: 初始资金
  - symbol: 交易对
  - interval: K线周期
  - saveResult: 是否保存结果
  - feeRatio: 手续费比率
- **返回**: 所有策略的回测结果汇总

### 历史数据接口

#### 智能获取历史K线数据
- **URL**: `/api/api/market/history/fetch-with-integrity`
- **方法**: GET
- **参数**:
  - symbol: 交易对
  - interval: K线周期
  - startTime: 开始时间
  - endTime: 结束时间
- **功能**: 
  - 智能计算所需K线数量
  - 检查MySQL已有数据
  - 按需获取缺失数据
  - 智能处理时间边界问题
  - 避免获取未完成的时间周期数据

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

#### 4. 批量回测所有策略
```bash
curl "http://localhost:8088/api/api/backtest/ta4j/run-all?startTime=2024-06-17%2000%3A00%3A00&endTime=2025-06-17%2000%3A00%3A00&initialAmount=10000&symbol=BTC-USDT&interval=1D&saveResult=True&feeRatio=0.001"
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

## 🐳 Docker部署详细说明

### 目录结构
```
.
├── Dockerfile               # Docker镜像构建文件
├── docker-compose.yml       # Docker Compose配置文件
├── .dockerignore            # Docker构建忽略文件
├── deploy.sh                # Linux/MacOS部署脚本
├── deploy.ps1               # Windows部署脚本
└── README.md                # 部署文档
```

### 前提条件
- 安装 [Docker](https://www.docker.com/get-started)
- 安装 [Docker Compose](https://docs.docker.com/compose/install/)
- 安装 Java 8 JDK (用于本地构建)
- 安装 Maven (或使用项目中的mvnw)
- 本地运行 MySQL 服务 (端口3306)
- 本地运行 Redis 服务 (端口6379)

### 本地服务配置要求

#### MySQL
- 地址: localhost:3306
- 用户名: root
- 密码: Password123?
- 数据库: okx_trading

#### Redis
- 地址: localhost:6379
- 无密码

### Dockerfile配置说明
- 基于OpenJDK 8 Alpine镜像构建
- 配置了代理设置，可通过构建参数修改
- 内存配置通过环境变量`JAVA_OPTS`设置
- 时区设置为Asia/Shanghai

### docker-compose.yml配置说明
- 仅包含应用服务，使用本地的MySQL和Redis
- 端口映射：应用：8088 -> 8088
- 使用`host.docker.internal`连接宿主机上的MySQL和Redis服务
- 数据卷：logs：应用日志持久化

### 部署步骤

#### Windows系统
```powershell
# 确保本地MySQL和Redis服务正在运行
# 进入项目根目录
.\deploy.ps1
```

#### Linux/MacOS系统
```bash
# 确保本地MySQL和Redis服务正在运行
# 给部署脚本添加执行权限
chmod +x deploy.sh
# 执行部署脚本
./deploy.sh
```

### 手动部署步骤
```bash
# 1. 确保本地MySQL和Redis服务正在运行
# 2. 构建应用
./mvnw clean package -DskipTests

# 3. 构建Docker镜像并启动容器
docker-compose up -d

# 4. 查看容器状态
docker-compose ps

# 5. 查看应用日志
docker-compose logs -f app
```

### 停止和清理
```bash
docker-compose down
```

## 🔧 支持的策略类型

### 传统技术指标策略
- **SMA**: 简单移动平均线交叉策略
- **BOLLINGER**: 布林带策略
- **RSI**: 相对强弱指数策略
- **MACD**: 指数平滑移动平均线策略
- **STOCHASTIC**: 随机指标策略
- **WILLIAMS_R**: 威廉指标策略
- **VOLUME_BREAKOUT**: 成交量突破策略

### AI生成策略
- **趋势策略**: 基于均线、趋势线的策略
- **震荡策略**: 基于RSI、KDJ等震荡指标
- **突破策略**: 基于成交量、价格突破
- **组合策略**: 多指标组合的复合策略

### Ta4j策略详细说明

#### 1. 简单移动平均线策略（SMA交叉）
当短期移动平均线上穿长期移动平均线时买入，下穿时卖出。这是一种经典的趋势跟踪策略。

**参数说明**：
- `shortPeriod`：短期移动平均线周期，默认值：5
- `longPeriod`：长期移动平均线周期，默认值：20

#### 2. 布林带策略
布林带策略有两种模式：

**反转模式（REVERSAL）**：
- 当价格触及下轨时买入（认为超卖）
- 当价格触及上轨时卖出（认为超买）
- 适合震荡市场

**突破模式（BREAKOUT）**：
- 当价格突破上轨时买入（认为上涨趋势确立）
- 当价格跌破下轨时卖出（认为下跌趋势确立）
- 适合趋势市场

**参数说明**：
- `period`：布林带周期，默认值：20
- `deviation`：标准差倍数，默认值：2.0
- `tradingMode`：交易模式，可选值：REVERSAL（反转）、BREAKOUT（突破）

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

### 智能数据管理
- 智能K线数据获取功能
- 时间边界智能调整
- 避免获取未完成的时间周期数据
- MySQL数据完整性检查

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
│   │   ├── Ta4jBacktestController.java
│   │   └── MarketController.java
│   ├── service/            # 业务逻辑服务
│   │   ├── DeepSeekApiService.java           # AI策略生成
│   │   ├── Ta4jBacktestService.java          # 回测服务
│   │   ├── HistoricalDataService.java       # 历史数据服务
│   │   └── StrategyFactory.java              # 策略工厂
│   ├── entity/             # 数据库实体
│   │   ├── StrategyInfo.java
│   │   ├── BacktestSummary.java
│   │   └── BacktestTrade.java
│   ├── repository/         # 数据访问层
│   │   └── redis/         # Redis存储库接口
│   ├── config/            # 配置类
│   ├── util/              # 工具类
│   └── strategy/          # 策略注册中心
├── src/main/resources/
│   ├── application.yml    # 主配置文件
│   ├── data.sql          # 策略初始化数据
│   └── static/           # 静态资源
├── docker/               # Docker相关文件
├── logs/                 # 日志目录
├── strategy_test_results/ # 测试结果
└── docker-compose.yml     # Docker编排文件
```

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

### 4. 验证策略可用性
```bash
# 验证单个策略
curl --location 'http://localhost:8088/api/api/backtest/ta4j/run?endTime=2025-04-01%2000%3A00%3A00&initialAmount=100000&interval=1D&saveResult=True&startTime=2018-01-01%2000%3A00%3A00&strategyType=ADX&symbol=BTC-USDT'

# 批量验证所有策略
curl "http://localhost:8088/api/api/backtest/ta4j/run-all?startTime=2024-06-17%2000%3A00%3A00&endTime=2025-06-17%2000%3A00%3A00&initialAmount=10000&symbol=BTC-USDT&interval=1D&saveResult=True&feeRatio=0.001"
```

## 📋 注意事项

### AI策略生成
1. **API密钥安全**: 请妥善保管DeepSeek API密钥，不要提交到代码仓库
2. **策略验证**: AI生成的策略代码需要经过测试验证后再用于实际交易
3. **性能考虑**: 动态编译会消耗一定的CPU资源，建议在低峰期进行策略更新
4. **错误处理**: 如果策略代码编译失败，请检查生成的代码是否符合Ta4j语法规范

### 数据获取
1. **时间边界**: 查询时间范围包含最新一天或最新小时会被自动调整
2. **数据完整性**: 系统会自动检查并获取缺失的K线数据
3. **API限制**: 注意OKX API的调用频率限制

### 回测验证
1. **策略有效性**: 如果回测没有任何交易，则认为策略有问题，需要修复
2. **数据充足性**: 确保回测时间段内有足够的历史数据
3. **参数合理性**: 检查策略参数是否合理，避免无法成交的异常参数

## 🔍 故障排除

### API调用失败
- 检查DeepSeek API密钥是否正确配置
- 确认网络连接正常
- 查看应用日志获取详细错误信息

### 策略编译失败
- 检查生成的策略代码语法
- 确认Ta4j库版本兼容性
- 查看编译错误信息进行修复

### Docker部署问题
1. **应用无法连接数据库**
   - 确保本地MySQL服务正在运行
   - 检查MySQL用户名和密码配置
   - 确认数据库okx_trading已创建

2. **应用无法连接Redis**
   - 确保本地Redis服务正在运行
   - 检查Redis连接配置

3. **代理设置问题**
   - 修改docker-compose.yml中的代理配置
   - 重新构建：`docker-compose up -d --build`

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

- 邮箱: your-email@example.com
- GitHub Issues: 提交问题和建议

---

**注意**: 本系统仅用于教育和研究目的，不构成投资建议。使用本系统进行实际交易的风险由用户自行承担。

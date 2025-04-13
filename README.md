# OKX交易回测系统

## 项目概述
本项目是一个基于Java Spring Boot开发的加密货币交易策略回测系统，支持对历史K线数据进行策略回测，并提供详细的回测分析结果。系统可以评估不同交易策略在历史行情下的表现，帮助交易者优化交易决策。

## 主要功能
- **历史数据获取**：从OKX交易所API获取历史K线数据
- **策略回测**：支持SMA(简单移动平均线)和布林带(Bollinger Bands)等技术指标策略的回测
- **性能分析**：计算回测结果的各项指标，如总收益率、夏普比率、胜率等
- **数据存储**：将回测结果和交易记录保存到数据库中
- **API接口**：提供RESTful API接口用于执行回测和查询结果

## 技术栈
- Java 8
- Spring Boot 2.x
- MySQL 数据库
- Redis 缓存
- Ta4j 技术分析库
- WebSocket 实时数据获取
- Swagger API文档

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

## API接口说明

### 执行回测
- **URL**: `/api/v1/backtest`
- **方法**: POST
- **参数**:
  - symbol: 交易对(如"BTC-USDT")
  - interval: K线周期(如"1h", "4h", "1d")
  - strategyType: 策略类型("SMA"或"BOLLINGER_BANDS")
  - startTime: 回测开始时间
  - endTime: 回测结束时间
  - initialAmount: 初始资金
  - parameters: 策略参数(JSON格式)
- **返回**: 回测结果，包含回测ID和汇总指标

### 获取回测交易记录
- **URL**: `/api/v1/backtest/{backtestId}/trades`
- **方法**: GET
- **参数**:
  - backtestId: 回测ID
  - page: 页码
  - size: 每页记录数
- **返回**: 分页的交易记录列表

### 获取回测汇总信息
- **URL**: `/api/v1/backtest/{backtestId}`
- **方法**: GET
- **参数**:
  - backtestId: 回测ID
- **返回**: 回测汇总信息

### 获取所有回测记录
- **URL**: `/api/v1/backtest/list`
- **方法**: GET
- **参数**:
  - page: 页码
  - size: 每页记录数
  - symbol: 交易对(可选)
  - strategyType: 策略类型(可选)
- **返回**: 分页的回测记录列表

## 使用示例

### 执行SMA策略回测
```json
{
  "symbol": "BTC-USDT",
  "interval": "1h",
  "strategyType": "SMA",
  "startTime": "2023-01-01T00:00:00",
  "endTime": "2023-01-31T23:59:59",
  "initialAmount": 10000,
  "parameters": {
    "shortPeriod": 5,
    "longPeriod": 20
  }
}
```

### 执行布林带策略回测
```json
{
  "symbol": "ETH-USDT",
  "interval": "4h",
  "strategyType": "BOLLINGER_BANDS",
  "startTime": "2023-01-01T00:00:00",
  "endTime": "2023-01-31T23:59:59",
  "initialAmount": 10000,
  "parameters": {
    "period": 20,
    "deviation": 2.0
  }
}
```

## 安装与配置

### 环境要求
- JDK 8
- Maven 3.6+
- MySQL 5.7+
- Redis 6.0+

### 配置文件
在`application.properties`中配置数据库和Redis连接：

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/okx_trading?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=Password123?

# Redis配置
spring.redis.host=localhost
spring.redis.port=6379

# OKX API配置
okx.api.key=your_api_key
okx.api.secret=your_api_secret
okx.api.passphrase=your_passphrase

# 代理配置(适用于中国区域)
proxy.host=localhost
proxy.port=10809
```

### 构建与运行
```bash
# 克隆仓库
git clone https://github.com/ralph-wren/okx-trading.git

# 进入项目目录
cd okx-trading

# 编译打包
mvn clean package

# 运行应用
java -jar target/okx-trading.jar
```

## 贡献指南
欢迎提交Issue和Pull Request来改进本项目。提交代码前，请确保：
1. 代码符合Java编码规范
2. 添加单元测试和集成测试
3. 更新相关文档

## 许可证
MIT License

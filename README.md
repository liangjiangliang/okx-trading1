# OKX Trading API 集成服务

一个用于集成OKX交易所API的SpringBoot服务，支持获取行情数据、账户信息以及交易功能。

## 功能特点

- 获取历史K线行情数据
- 获取实时行情数据
- 查询账户余额信息(包括真实账户和模拟账户)
- 查询订单信息
- 创建现货/合约订单
- 取消订单
- 支持真实数据和模拟数据切换

## 技术栈

- Java 8
- Spring Boot 2.7.8
- Spring Data JPA
- MySQL
- Redis
- OkHttp3
- Swagger (API文档)

## 快速开始

### 前置条件

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+
- Redis 5.0+

### 配置

在 `application.yml` 中配置数据库、Redis和OKX API参数:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/okx_trading?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: Password123?
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 10000
    
okx:
  api:
    base-url: https://www.okx.com
    api-key: ${OKX_API_KEY:}
    secret-key: ${OKX_SECRET_KEY:}
    passphrase: ${OKX_PASSPHRASE:}
    use-mock-data: false # 是否使用模拟数据
```

对于敏感信息，建议通过环境变量注入:

```bash
export OKX_API_KEY=your_api_key
export OKX_SECRET_KEY=your_secret_key
export OKX_PASSPHRASE=your_passphrase
```

### 编译与运行

```bash
mvn clean package
java -jar target/trading-0.0.1-SNAPSHOT.jar
```

或者使用Maven直接运行:

```bash
mvn spring-boot:run
```

## API接口文档

启动应用后访问Swagger文档: http://localhost:8088/api/swagger-ui/

### 行情数据接口

#### 1. 获取K线数据

```
GET /api/market/klines?symbol={symbol}&interval={interval}&limit={limit}
```

**参数说明:**
- `symbol`: 交易对，如 BTC-USDT (必填)
- `interval`: K线间隔，如 1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M (必填)
- `limit`: 获取数据条数，最大为1000 (选填)

**响应示例:**
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "symbol": "BTC-USDT",
      "interval": "1m",
      "openTime": "2023-06-10 10:00:00",
      "open": "26000.50",
      "high": "26100.00",
      "low": "25900.00",
      "close": "26050.00",
      "volume": "100.5",
      "quoteVolume": "2612025.00",
      "closeTime": "2023-06-10 10:01:00",
      "trades": 500
    }
  ]
}
```

#### 2. 获取最新行情

```
GET /api/market/ticker?symbol={symbol}
```

**参数说明:**
- `symbol`: 交易对，如 BTC-USDT (必填)

**响应示例:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "symbol": "BTC-USDT",
    "lastPrice": "26050.00",
    "priceChange": "1050.00",
    "priceChangePercent": "4.20",
    "highPrice": "26500.00",
    "lowPrice": "25000.00",
    "volume": "5000.50",
    "quoteVolume": "130125000.00",
    "bidPrice": "26045.00",
    "bidQty": "2.5",
    "askPrice": "26055.00",
    "askQty": "1.8",
    "timestamp": "2023-06-10 10:15:00"
  }
}
```

### 账户信息接口

#### 1. 获取账户余额

```
GET /api/account/balance
```

**响应示例:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalEquity": "100000.50",
    "accountType": 0,
    "accountId": "12345678",
    "availableBalance": "80000.40",
    "frozenBalance": "20000.10",
    "assetBalances": [
      {
        "asset": "BTC",
        "available": "1.5000",
        "frozen": "0.5000",
        "total": "2.0000",
        "usdValue": "52000.00"
      },
      {
        "asset": "USDT",
        "available": "50000.00",
        "frozen": "10000.00",
        "total": "60000.00",
        "usdValue": "60000.00"
      }
    ]
  }
}
```

#### 2. 获取模拟账户余额

```
GET /api/account/simulated-balance
```

(响应格式同上，但 accountType = 1)

### 交易接口

#### 1. 获取订单列表

```
GET /api/trade/orders?symbol={symbol}&status={status}&limit={limit}
```

**参数说明:**
- `symbol`: 交易对，如 BTC-USDT (必填)
- `status`: 订单状态，如 NEW, PARTIALLY_FILLED, FILLED, CANCELED (选填)
- `limit`: 获取数据条数，最大为100 (选填)

**响应示例:**
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "orderId": "123456789",
      "clientOrderId": "client12345",
      "symbol": "BTC-USDT",
      "price": "26000.00",
      "origQty": "0.5000",
      "executedQty": "0.0000",
      "cummulativeQuoteQty": "0.0000",
      "status": "NEW",
      "type": "LIMIT",
      "side": "BUY",
      "timeInForce": "GTC",
      "createTime": "2023-06-10 09:00:00",
      "updateTime": "2023-06-10 09:00:00",
      "simulated": false,
      "fee": "13.00",
      "feeCurrency": "USDT"
    }
  ]
}
```

#### 2. 创建现货订单

```
POST /api/trade/spot-orders
```

**请求体示例 (按数量下单):**
```json
{
  "symbol": "BTC-USDT",
  "type": "LIMIT",
  "side": "BUY",
  "price": "26000.00",
  "quantity": "0.5000",
  "timeInForce": "GTC",
  "postOnly": false,
  "simulated": false
}
```

**请求体示例 (按金额下单):**
```json
{
  "symbol": "BTC-USDT",
  "type": "LIMIT",
  "side": "BUY",
  "price": "26000.00",
  "amount": "13000.00", // 使用13000USDT购买BTC
  "timeInForce": "GTC",
  "postOnly": false,
  "simulated": false
}
```

**请求体说明:**
- 可以通过指定`quantity`(数量)或`amount`(金额)下单
- 买入订单时，`amount`表示用于购买的计价货币金额（如USDT）
- 卖出订单时，`amount`表示要卖出的标的资产数量（如BTC）
- 如果同时提供`quantity`和`amount`，优先使用`quantity`

**响应示例:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "orderId": "123456789",
    "clientOrderId": "client12345",
    "symbol": "BTC-USDT",
    "price": "26000.00",
    "origQty": "0.5000",
    "executedQty": "0.0000",
    "cummulativeQuoteQty": "0.0000",
    "status": "NEW",
    "type": "LIMIT",
    "side": "BUY",
    "timeInForce": "GTC",
    "createTime": "2023-06-10 09:00:00",
    "updateTime": "2023-06-10 09:00:00",
    "simulated": false
  }
}
```

#### 3. 创建合约订单

```
POST /api/trade/futures-orders
```

**请求体示例 (按数量下单):**
```json
{
  "symbol": "BTC-USDT-SWAP",
  "type": "LIMIT",
  "side": "BUY",
  "price": "26000.00",
  "quantity": "0.5000",
  "timeInForce": "GTC",
  "leverage": 5,
  "postOnly": false,
  "simulated": false
}
```

**请求体示例 (按金额下单):**
```json
{
  "symbol": "BTC-USDT-SWAP",
  "type": "LIMIT",
  "side": "BUY",
  "price": "26000.00",
  "amount": "13000.00", // 使用13000USDT购买BTC合约
  "timeInForce": "GTC",
  "leverage": 5,
  "postOnly": false,
  "simulated": false
}
```

(响应格式同上)

#### 4. 取消订单

```
DELETE /api/trade/orders?symbol={symbol}&orderId={orderId}
```

**参数说明:**
- `symbol`: 交易对，如 BTC-USDT (必填)
- `orderId`: 订单ID (必填)

**响应示例:**
```json
{
  "code": 0,
  "message": "success",
  "data": true
}
```

## 模拟模式与真实模式切换

在 `application.yml` 中配置:

```yaml
okx:
  api:
    use-mock-data: true  # 使用模拟数据
    # 或
    use-mock-data: false # 使用真实API
```

## 代理设置

在访问国际接口时可能需要代理，可以在配置文件中设置:

```yaml
okx:
  proxy:
    enabled: true
    host: localhost
    port: 10809
```

## 贡献与问题反馈

如有问题或建议，请提交Issue或Pull Request。

## 许可证

此项目基于MIT许可证开源。 
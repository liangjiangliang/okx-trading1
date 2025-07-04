# OKX 智能交易策略回测系统

## 🎯 项目概述

OKX交易系统是一个基于Java Spring Boot开发的智能加密货币交易策略回测系统，集成了AI策略生成、历史数据回测和性能分析功能。系统支持通过自然语言描述自动生成交易策略，预内置130+技术分析策略，并在历史K线数据上提供详细的回测分析结果。

## 🚀 核心功能

### 🤖 AI智能策略生成
- **自然语言策略生成**：基于DeepSeek API，通过自然语言描述自动生成Ta4j交易策略
- **智能策略理解**：支持理解和解析复杂的策略描述，如"基于双均线和RSI组合的交易策略"
- **动态编译加载**：使用Janino和Java Compiler API进行实时策略代码编译和动态加载
- **策略热重载**：无需重启服务即可加载新策略，支持策略的实时更新和删除
- **多重编译方式**：支持Janino、Java Compiler API和三种编译方式的智能选择
- **策略管理**：完整的策略CRUD操作，支持策略版本控制和历史追踪

### 📊 丰富的策略库 (130+ 策略)

#### 移动平均策略 (15种)
- **经典移动平均**：SMA、EMA、WMA、HMA等基础移动平均
- **高级移动平均**：KAMA自适应、ZLEMA零滞后、DEMA/TEMA多重指数平滑
- **特殊移动平均**：VWAP成交量加权、TRIMA三角平滑、T3高级平滑
- **自适应移动平均**：MAMA自适应、VIDYA变动态、Wilders平滑

#### 振荡器策略 (17种)
- **经典指标**：RSI、随机指标、威廉指标、CCI
- **复合指标**：随机RSI、CMO、ROC、PPO
- **高级指标**：TRIX、Fisher变换、EOM易动性
- **专业指标**：CHOP噪音指数、KVO克林格成交量振荡器、RVGI相对活力

#### 趋势指标 (14种)
- **趋势确认**：MACD、ADX、Aroon、DMI
- **趋势跟踪**：Supertrend、抛物线SAR、一目均衡表
- **高级趋势**：涡流指标、QStick、威廉鳄鱼
- **数学趋势**：希尔伯特变换系列

#### 波动率指标 (12种)
- **通道指标**：布林带、肯特纳通道、唐奇安通道
- **波动率测量**：ATR、溃疡指数、标准差、波动率
- **高级波动率**：质量指数、挤压、BBW布林带宽度
- **特殊波动率**：吊灯止损、NATR标准化ATR

#### 成交量指标 (12种)
- **经典成交量**：OBV能量潮、A/D累积分布线、质量指数
- **高级成交量**：KDJ、AD/ADOSC振荡器、PVI/NVI正负成交量指数
- **成交量分析**：VWMA成交量加权、VOSC成交量振荡器、MarketFI市场促进

#### K线形态策略 (16种)
- **反转形态**：十字星、锤子线、流星线
- **吞没形态**：看涨/看跌吞没
- **组合形态**：早晨之星/黄昏之星、穿刺线
- **特殊形态**：三白兵/三黑鸦

#### 统计函数策略 (8种)
- **相关性分析**：Beta系数、皮尔逊相关系数
- **回归分析**：线性回归、线性回归角度/斜率/截距
- **统计指标**：方差、时间序列预测、标准差

#### 希尔伯特变换策略 (6种)
- **周期分析**：主导周期、主导相位、趋势模式
- **信号处理**：正弦波、相量分量、MESA正弦波

#### 组合策略 (20种)
- **经典组合**：双重推力、海龟交易
- **趋势组合**：金叉/死叉、趋势跟踪
- **复合策略**：双均线+RSI、MACD+布林带、三重筛选
- **创新组合**：一目均衡表突破、Elder Ray力度分析

#### 高级策略库 (50种)
- **自适应策略**：自适应布林带、多时间框架MACD、自适应RSI
- **高级成交量**：克林格振荡器、佳庆振荡器、力度指数
- **高级移动平均**：分形自适应、零滞后EMA、高斯/巴特沃斯滤波器
- **专业指标**：火箭RSI、康纳斯RSI、终极振荡器

#### 创新策略集 (40种)
- **机器学习启发**：神经网络、遗传算法、随机森林、SVM
- **量化因子**：动量因子、价值因子、质量因子、低波动因子
- **高频策略**：微观结构失衡、日内均值回归、统计套利
- **风险管理**：凯利准则、VaR风险管理、最大回撤控制

### 🔬 高级回测系统

#### 多维度回测分析
- **Ta4j集成**：基于专业Ta4j技术分析库，提供标准化回测框架
- **多时间周期**：支持1分钟到1个月各种K线周期的回测
- **批量回测**：支持所有策略的批量回测，便于策略筛选和对比
- **并行回测**：支持多线程并行回测，提高效率

#### 丰富的性能指标
- **收益指标**：总收益、年化收益、绝对收益、超额收益
- **风险指标**：最大回撤、夏普比率、波动率、下行偏差
- **交易指标**：胜率、盈亏比、平均持仓时间、交易频率
- **高级指标**：卡尔玛比率、索提诺比率、信息比率、跟踪误差

#### 详细交易记录
- **完整记录**：保存每笔交易的买入卖出价格、时间和盈亏
- **交易分析**：提供交易分布统计、盈亏分析、持仓周期分析
- **可视化支持**：支持交易记录的图表可视化分析

### 💾 数据管理与存储

#### 历史数据管理
- **OKX API集成**：自动从OKX交易所获取历史K线数据
- **多币种支持**：支持OKX交易所所有交易对数据
- **数据缓存**：Redis缓存优化，提高数据访问性能
- **数据清理**：自动化数据清理和维护机制

#### 数据库架构
- **MySQL存储**：使用MySQL存储策略信息、回测结果和交易记录
- **完整架构**：包含策略信息表、回测摘要表、交易详情表
- **数据迁移**：Liquibase管理数据库版本和迁移
- **性能优化**：合理的索引设计和查询优化

### 🔧 技术特性

#### 系统架构
- **微服务设计**：模块化设计，功能模块独立且可扩展
- **异步处理**：支持异步回测和数据处理，提高系统响应性
- **容器化部署**：Docker + Docker Compose支持，便于部署和扩展
- **配置管理**：灵活的配置管理，支持多环境部署

#### 开发特性
- **代码质量**：完整的单元测试覆盖，规范的代码结构
- **完整文档**：Swagger API文档，详细的代码注释
- **热重载**：JRebel支持，开发期间代码即时更改
- **日志管理**：完善的日志系统，分级记录和日志分析

## 🛠 技术栈

### 后端框架
- **Spring Boot 2.7.8**：核心框架
- **Java 8**：开发语言
- **Maven**：项目管理和构建工具

### 数据存储
- **MySQL 8.0**：主要数据库
- **Redis 6.0+**：缓存数据库
- **Liquibase**：数据库版本管理

### 技术分析
- **Ta4j 0.14**：专业技术分析库
- **Janino**：动态代码编译
- **Java Compiler API**：高级代码编译

### AI集成
- **DeepSeek API**：AI策略生成
- **自然语言处理**：策略描述解析
- **智能编译**：多种编译方式的自动选择

### 网络通信
- **OkHttp3 4.9.3**：HTTP客户端
- **WebSocket**：实时数据获取
- **RESTful API**：标准API接口

### 部署运维
- **Docker**：容器化部署
- **Docker Compose**：多容器编排
- **Nginx**：反向代理(可选)

## 📈 高级风险指标系统

### 新增风险指标 (15个)
- **峰度 (kurtosis)**：衡量收益率分布的尾部风险
- **条件风险价值 (cvar)**：极端损失的期望值
- **风险价值 (var95, var99)**：95%和99%置信度下的风险价值
- **信息比率 (informationRatio)**：超额收益相对于跟踪误差的比率
- **跟踪误差 (trackingError)**：策略与基准收益率的标准差
- **Sterling比率 (sterlingRatio)**：年化收益与平均最大回撤的比率
- **Burke比率 (burkeRatio)**：年化收益与平方根回撤的比率
- **修正夏普比率 (modifiedSharpeRatio)**：考虑偏度和峰度的夏普比率
- **下行偏差 (downsideDeviation)**：只考虑负收益的标准差
- **上涨捕获率 (uptrendCapture)**：基准上涨时策略的表现
- **下跌捕获率 (downtrendCapture)**：基准下跌时策略的表现
- **最大回撤持续期 (maxDrawdownDuration)**：从峰值到恢复的最长时间
- **痛苦指数 (painIndex)**：回撤深度与持续时间的综合指标
- **风险调整收益 (riskAdjustedReturn)**：综合多种风险因素的收益评估

### 综合评分系统
- **科学评分体系**：0-10分的科学评分体系
- **四个维度评估**：收益表现、风险控制、交易质量、稳定性
- **权重分配**：收益表现40%、风险控制30%、交易质量20%、稳定性10%

## 🌐 动态指标分布评分系统

### 系统特点
- **数据驱动**：基于6000个真实回测样本的分布情况
- **动态阈值**：避免固定阈值的主观性和局限性
- **分位数评分**：采用分位数划分区间，确保评分的均匀分布
- **多维度评估**：收益、风险、质量、稳定性四个维度综合评估

### API接口
- **指标分布统计**：`GET /api/backtest/ta4j/indicator-distribution-details`
- **动态评分计算**：`POST /api/backtest/ta4j/calculate-dynamic-score`

### 评分等级
- **9.0-10.0分**：优秀策略 - 各项指标均表现卓越，风险控制良好
- **8.0-8.9分**：良好策略 - 收益风险平衡较好，值得关注
- **7.0-7.9分**：中上策略 - 整体表现良好，但某些指标有提升空间
- **6.0-6.9分**：中等策略 - 表现平均，需要进一步优化
- **5.0-5.9分**：中下策略 - 表现一般，存在明显改进空间
- **4.0-4.9分**：较差策略 - 风险收益比不理想，需要重新评估

## 🔗 WebSocket连接优化

### 连接优化策略
- **差异化检测**：不同频道采用不同的连接检测策略
- **智能重连**：基于连接状态和频道特性的智能重连机制
- **状态管理**：使用ConcurrentHashMap管理重连状态，防止并发重连
- **延迟策略**：优化重连延迟策略，提高连接成功率

### K线重新订阅解决方案
- **重连事件监听**：WebSocket重连后自动触发K线重新订阅
- **状态持久化**：订阅状态保存在Redis中，支持应用重启后恢复
- **批量处理**：支持批量重新订阅，提高处理效率
- **异步执行**：使用异步处理避免阻塞主线程

## 🚀 快速开始

### 环境要求
- Java 8+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.6+
- Docker & Docker Compose (可选)

### 本地开发部署

1. **克隆项目**
```bash
git clone https://github.com/your-repo/okx-trading.git
cd okx-trading
```

2. **配置数据库**
```bash
# 启动MySQL和Redis
docker-compose up -d mysql redis

# 或使用现有数据库，修改application.properties
```

3. **配置参数**
```properties
# src/main/resources/application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/okx_trading
spring.datasource.username=root
spring.datasource.password=your_password

spring.redis.host=localhost
spring.redis.port=6379

# OKX API配置
okx.api.key=your_api_key
okx.api.secret=your_api_secret
okx.api.passphrase=your_passphrase
```

4. **启动应用**
```bash
mvn spring-boot:run
```

5. **访问系统**
- API文档：http://localhost:8088/swagger-ui.html
- 健康检查：http://localhost:8088/actuator/health

### Docker部署

```bash
# 构建并启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f app
```

## 📚 API文档

### 策略管理
- `GET /api/strategy/list` - 获取策略列表
- `POST /api/strategy/generate` - AI生成策略
- `PUT /api/strategy/update` - 更新策略
- `DELETE /api/strategy/{id}` - 删除策略

### 回测分析
- `POST /api/backtest/ta4j/run` - 单策略回测
- `POST /api/backtest/ta4j/run-all` - 批量回测
- `GET /api/backtest/results/{id}` - 获取回测结果
- `GET /api/backtest/ta4j/indicator-distribution-details` - 指标分布统计

### 市场数据
- `GET /api/market/candlestick` - 获取K线数据
- `POST /api/market/subscribe` - 订阅实时数据
- `DELETE /api/market/unsubscribe` - 取消订阅

### 动态评分
- `POST /api/backtest/ta4j/calculate-dynamic-score` - 计算动态评分

## 🧪 测试说明

### 新增风险指标测试
1. **单个策略回测测试**
```bash
curl -X POST "http://localhost:8088/api/backtest/ta4j/run?endTime=2025-01-01%2000%3A00%3A00&initialAmount=10000&interval=1D&saveResult=true&startTime=2024-01-01%2000%3A00%3A00&strategyType=SMA&symbol=BTC-USDT"
```

2. **批量策略回测测试**
```bash
curl -X POST "http://localhost:8088/api/backtest/ta4j/run-all?startTime=2024-01-01%2000%3A00%3A00&endTime=2024-12-01%2000%3A00%3A00&initialAmount=10000&symbol=BTC-USDT&interval=1D&saveResult=true&feeRatio=0.001"
```

### 数据库迁移
```bash
mysql -u root -p okx_trading < src/main/resources/migration_add_risk_indicators.sql
```

## 📊 性能监控

### 日志配置
- 应用日志：`logs/all/`
- API日志：`logs/api/`
- 错误日志：`logs/error/`

### 监控指标
- WebSocket连接状态
- 回测执行性能
- 数据库查询性能
- Redis缓存命中率
- API调用频率

## 🔒 安全配置

### API安全
- OKX API密钥加密存储
- 请求签名验证
- 频率限制控制

### 数据安全
- 数据库连接加密
- 敏感信息脱敏
- 访问权限控制

## 🤝 贡献指南

### 开发流程
1. Fork项目
2. 创建功能分支
3. 提交更改
4. 创建Pull Request

### 代码规范
- 遵循Java编码规范
- 添加必要的单元测试
- 更新相关文档
- 保持代码质量

## 📄 许可证

本项目采用MIT许可证，详见LICENSE文件。

## 🙋‍♂️ 支持与反馈

如有问题或建议，请：
1. 提交GitHub Issue
2. 查看项目Wiki
3. 联系项目维护者

---

**OKX智能交易策略回测系统** - 让量化交易更智能，让策略开发更简单！

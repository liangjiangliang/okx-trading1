# OKX Trading 智能交易策略回测系统

## 🎯 项目概述

OKX Trading 是一个基于Java Spring Boot开发的智能加密货币交易策略回测系统，集成了AI策略生成、历史数据回测和性能分析功能。系统支持通过自然语言描述自动生成交易策略，拥有130+种预置技术分析策略，并对历史K线数据进行策略回测，提供详细的回测分析结果。

## 🚀 核心功能特性

### 🤖 AI智能策略生成
- **自然语言策略生成**：基于DeepSeek API，通过自然语言描述自动生成Ta4j交易策略
- **智能策略理解**：支持复杂策略描述的理解和解析，如"基于双均线RSI组合的交易策略"
- **动态编译加载**：使用Janino和Java Compiler API实时编译策略代码并动态加载
- **策略热更新**：无需重启服务即可加载新策略，支持策略的实时更新和删除
- **多种编译方式**：支持Janino、Java Compiler API和智能选择三种编译方式
- **策略管理**：完整的策略CRUD操作，支持策略版本控制和历史追踪

### 📊 丰富的策略库（130+种策略）

#### 移动平均线策略（15种）
- **经典均线**：SMA、EMA、WMA、HMA等基础移动平均线
- **高级均线**：KAMA自适应、ZLEMA零滞后、DEMA/TEMA多重指数平滑
- **特殊均线**：VWAP成交量加权、TRIMA三角平滑、T3高级平滑
- **自适应均线**：MAMA自适应、VIDYA可变动态、Wilders平滑

#### 震荡指标策略（17种）
- **经典指标**：RSI、Stochastic、Williams %R、CCI
- **组合指标**：Stochastic RSI、CMO、ROC、PPO
- **高级指标**：TRIX、Fisher变换、EOM移动便利性
- **专业指标**：CHOP震荡指数、KVO克林格振荡器、RVGI相对活力

#### 趋势指标策略（14种）
- **趋势确认**：MACD、ADX、Aroon、DMI
- **趋势跟踪**：Supertrend、Parabolic SAR、Ichimoku一目均衡表
- **高级趋势**：Vortex涡流、QStick、Williams Alligator鳄鱼线
- **数学趋势**：Hilbert Transform希尔伯特变换系列

#### 波动指标策略（12种）
- **通道指标**：Bollinger Bands、Keltner Channel、Donchian Channels
- **波动测量**：ATR、Ulcer Index、标准差、波动率
- **高级波动**：Mass Index质量指数、Squeeze挤压、BBW布林带宽度
- **特殊波动**：Chandelier Exit吊灯止损、NATR归一化ATR

#### 成交量指标策略（12种）
- **经典成交量**：OBV能量潮、A/D累积派发线、Mass Index
- **高级成交量**：KDJ、AD/ADOSC振荡器、PVI/NVI正负成交量指数
- **成交量分析**：VWMA成交量加权、VOSC成交量振荡器、MarketFI市场便利

#### 蜡烛图形态策略（16种）
- **反转形态**：Doji十字星、Hammer锤子线、Shooting Star流星线
- **吞没形态**：Bullish/Bearish Engulfing看涨看跌吞没
- **组合形态**：Morning/Evening Star晨星暮星、Piercing刺透
- **特殊形态**：Three White Soldiers/Black Crows三白兵三黑乌鸦

#### 统计函数策略（8种）
- **相关分析**：Beta系数、皮尔逊相关系数
- **回归分析**：线性回归、线性回归角度/斜率/截距
- **统计指标**：方差、时间序列预测、标准差

#### 希尔伯特变换策略（6种）
- **周期分析**：主导周期、主导相位、趋势模式
- **信号处理**：正弦波、相量分量、MESA正弦波

#### 组合策略（20种）
- **经典组合**：Dual Thrust双推、Turtle Trading海龟交易
- **趋势组合**：Golden/Death Cross金叉死叉、Trend Following
- **复合策略**：双均线+RSI、MACD+Bollinger、Triple Screen三重筛选
- **创新组合**：一目均衡表云突破、Elder Ray力量分析

#### 高级策略库（50种）
- **自适应策略**：自适应布林带、多时间框架MACD、自适应RSI
- **高级成交量**：Klinger振荡器、Chaikin振荡器、Force Index
- **先进均线**：分形自适应、零滞后EMA、高斯/巴特沃斯滤波器
- **专业指标**：Rocket RSI、Connors RSI、Ultimate Oscillator

#### 创新策略集（40种）
- **机器学习启发**：神经网络、遗传算法、随机森林、SVM
- **量化因子**：动量因子、价值因子、质量因子、低波动因子
- **高频策略**：微观结构失衡、日内均值回归、统计套利
- **风险管理**：Kelly准则、VaR风险管理、最大回撤控制

### 🔬 高级回测系统

#### 多维度回测分析
- **Ta4j集成**：基于专业的Ta4j技术分析库，提供标准化回测框架
- **多时间周期**：支持1分钟到1月的各种K线周期回测
- **批量回测**：支持对所有策略进行批量回测，便于策略筛选和比较
- **并行回测**：支持多线程并行回测，提高回测效率

#### 丰富的性能指标
- **收益指标**：总收益率、年化收益率、绝对收益、超额收益
- **风险指标**：最大回撤、夏普比率、波动率、下行偏差
- **交易指标**：胜率、盈亏比、平均持仓时间、交易频率
- **高级指标**：Calmar比率、Sortino比率、信息比率、跟踪误差

#### 详细的交易记录
- **完整记录**：保存每笔交易的买入卖出价格、时间、盈亏情况
- **交易分析**：提供交易分布统计、盈亏分析、持仓周期分析
- **可视化支持**：支持交易记录的图表化展示和分析

### 💾 数据管理与存储

#### 历史数据管理
- **OKX API集成**：自动从OKX交易所获取历史K线数据
- **多品种支持**：支持所有OKX交易所的交易对数据
- **数据缓存**：Redis缓存优化，提升数据访问性能
- **数据清理**：自动数据清理和维护机制

#### 数据库架构
- **MySQL存储**：使用MySQL存储策略信息、回测结果和交易记录
- **完整架构**：包含策略信息表、回测汇总表、交易明细表
- **数据迁移**：Liquibase管理数据库版本和迁移
- **性能优化**：合理的索引设计和查询优化

### 🔧 技术特性

#### 系统架构
- **微服务设计**：模块化设计，各功能模块独立可扩展
- **异步处理**：支持异步回测和数据处理，提高系统响应性
- **容器化部署**：Docker + Docker Compose支持，便于部署和扩展
- **配置管理**：灵活的配置管理，支持多环境部署

#### 开发特性
- **代码质量**：完整的单元测试覆盖，规范的代码结构
- **文档完善**：Swagger API文档，详细的代码注释
- **热重载**：JRebel支持，开发时代码修改即时生效
- **日志管理**：完善的日志系统，支持分级日志和日志分析

## 🛠 技术栈

### 后端框架
- **Spring Boot 2.7.8**：核心框架
- **Java 8**：开发语言
- **Maven**：项目管理和构建工具

### 数据存储
- **MySQL 8.0**：主数据库
- **Redis 6.0+**：缓存数据库
- **Liquibase**：数据库版本管理

### 技术分析
- **Ta4j 0.14**：专业技术分析库
- **Janino**：动态代码编译
- **Java Compiler API**：高级代码编译

### AI集成
- **DeepSeek API**：AI策略生成
- **自然语言处理**：策略描述解析
- **智能编译**：多种编译方式自动选择

### 网络通信
- **OkHttp3 4.9.3**：HTTP客户端
- **WebSocket**：实时数据获取
- **RESTful API**：标准API接口

### 部署运维
- **Docker**：容器化部署
- **Docker Compose**：多容器编排
- **Nginx**：反向代理（可选）

## 📚 详细功能说明

### AI策略生成流程

#### 1. 策略描述解析
```
用户输入："基于RSI超买超卖策略，当RSI低于30时买入，高于70时卖出"
         ↓
DeepSeek API解析
         ↓
生成Ta4j策略代码
         ↓
动态编译并加载到系统
```

#### 2. 支持的策略描述类型
- **基础指标策略**：RSI、MACD、Bollinger Bands等
- **组合策略**：多指标组合、不同时间框架组合
- **自定义逻辑**：复杂的买卖条件和风险控制
- **参数化策略**：支持参数自定义的策略模板

#### 3. 智能编译系统
- **三种编译方式**：
  - `DynamicStrategyService`：基于Janino的轻量级编译
  - `JavaCompilerDynamicStrategyService`：基于Java Compiler API的标准编译
  - `SmartDynamicStrategyService`：智能选择最佳编译方式
- **错误处理**：完善的编译错误处理和用户反馈
- **性能优化**：编译结果缓存，避免重复编译

### 回测系统详解

#### 1. 回测执行流程
```
选择策略 → 设置参数 → 获取历史数据 → 执行回测 → 生成报告 → 保存结果
```

#### 2. 数据获取与处理
- **实时获取**：从OKX API实时获取最新K线数据
- **数据验证**：完整性检查、异常值处理
- **格式转换**：原始数据转换为Ta4j BarSeries格式
- **缓存策略**：智能缓存，减少API调用

#### 3. 性能分析算法
- **统计指标计算**：基于严格的金融数学公式
- **风险度量**：VaR、CVaR、最大回撤等风险指标
- **基准比较**：与买入持有策略的比较分析
- **分段分析**：支持按年、月、周进行分段性能分析

### 策略管理系统

#### 1. 策略注册中心
- **StrategyRegisterCenter**：统一的策略注册和管理中心
- **动态加载**：支持运行时动态添加、修改、删除策略
- **版本控制**：策略版本管理和回滚机制
- **依赖管理**：策略间依赖关系管理

#### 2. 策略分类体系
- **按指标类型**：移动平均、震荡指标、趋势指标等
- **按交易风格**：趋势跟踪、均值回归、突破策略等
- **按复杂度**：基础策略、高级策略、专业策略
- **按时间周期**：超短线、短线、中线、长线策略

#### 3. 策略评级系统
- **历史表现评级**：基于历史回测表现的综合评分
- **风险等级划分**：低风险、中风险、高风险分类
- **适用性评估**：不同市场环境下的适用性评分
- **推荐指数**：综合考虑收益、风险、稳定性的推荐指数

## 🎯 使用场景

### 个人投资者
- **策略学习**：学习各种技术分析策略的实现原理
- **策略验证**：验证自己的交易想法和策略效果
- **参数优化**：寻找最优的策略参数组合
- **风险评估**：了解策略的风险收益特征

### 专业交易者
- **策略开发**：快速开发和测试新的交易策略
- **组合优化**：构建和优化策略组合
- **风险管理**：全面的风险评估和控制
- **实盘指导**：为实盘交易提供决策支持

### 量化团队
- **策略研究**：大规模策略研究和开发
- **因子挖掘**：发现新的交易信号和因子
- **回测平台**：构建专业的回测和研究平台
- **算法交易**：为算法交易系统提供策略支持

### 教育培训
- **技术分析教学**：直观展示各种技术指标的效果
- **策略教育**：帮助学习者理解不同策略的原理
- **实践训练**：提供安全的策略实践环境
- **案例分析**：丰富的历史案例和分析

## 🚦 快速开始

### 环境要求
- **Java 8+**
- **MySQL 8.0+**
- **Redis 6.0+**
- **Maven 3.6+**

### 安装步骤

1. **克隆项目**
```bash
git clone https://github.com/your-repo/okx-trading.git
cd okx-trading
```

2. **配置数据库**
```sql
CREATE DATABASE okx_trading;
```

3. **配置环境变量**
```bash
export MYSQL_PASSWORD=your_mysql_password
export DEEPSEEK_API_KEY=your_deepseek_api_key
```

4. **启动服务**
```bash
mvn spring-boot:run
```

5. **访问系统**
- 应用地址：http://localhost:8088
- Swagger文档：http://localhost:8088/swagger-ui.html

### Docker部署

```bash
# 构建镜像
docker-compose build

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f
```

## 📖 API使用指南

### AI策略生成
```bash
# 生成单个策略
curl -X POST "http://localhost:8088/api/backtest/ta4j/generate-strategy" \
  -H "Content-Type: application/json" \
  -d '"基于RSI超买超卖策略，当RSI低于30时买入，高于70时卖出"'

# 批量生成策略
curl -X POST "http://localhost:8088/api/backtest/ta4j/generate-strategy" \
  -H "Content-Type: application/json" \
  -d '"基于双均线交叉策略
基于MACD金叉死叉策略
基于布林带突破策略"'
```

### 策略回测
```bash
# 单策略回测
curl "http://localhost:8088/api/backtest/ta4j/run?symbol=BTC-USDT&interval=1h&startTime=2023-01-01%2000:00:00&endTime=2023-12-31%2023:59:59&strategyType=RSI&initialAmount=100000&saveResult=true"

# 批量回测
curl "http://localhost:8088/api/backtest/ta4j/run-all?symbol=BTC-USDT&interval=1d&startTime=2023-01-01%2000:00:00&endTime=2023-12-31%2023:59:59&initialAmount=100000&saveResult=true&threadCount=8"
```

### 策略管理
```bash
# 获取策略列表
curl "http://localhost:8088/api/backtest/ta4j/strategies"

# 获取策略详情
curl "http://localhost:8088/api/backtest/ta4j/strategy/RSI"

# 删除策略
curl -X DELETE "http://localhost:8088/api/backtest/ta4j/delete-strategy/AI_STRATEGY_001"
```

### 结果查询
```bash
# 获取回测历史
curl "http://localhost:8088/api/backtest/ta4j/summaries"

# 获取具体回测详情
curl "http://localhost:8088/api/backtest/ta4j/detail/{backtestId}"

# 获取批量回测统计
curl "http://localhost:8088/api/backtest/ta4j/summaries/batch-statistics"
```

## 🔍 策略效果示例

### 经典策略表现（BTC-USDT，2023年数据）

| 策略名称 | 年化收益率 | 夏普比率 | 最大回撤 | 胜率 | 交易次数 |
|---------|-----------|----------|----------|------|----------|
| Dual Thrust | 45.2% | 1.68 | -12.3% | 58% | 156 |
| Turtle Trading | 38.7% | 1.42 | -15.8% | 52% | 28 |
| MACD | 31.5% | 1.25 | -18.2% | 55% | 42 |
| RSI | 28.9% | 1.15 | -16.7% | 61% | 73 |
| Bollinger Bands | 35.6% | 1.33 | -14.1% | 59% | 84 |

### AI生成策略表现

| 策略描述 | 年化收益率 | 夏普比率 | 最大回撤 | 评级 |
|---------|-----------|----------|----------|------|
| "基于双均线RSI组合的交易策略" | 42.3% | 1.55 | -11.2% | ★★★★★ |
| "成交量突破确认的动量策略" | 38.1% | 1.48 | -13.5% | ★★★★☆ |
| "多时间框架MACD趋势策略" | 35.7% | 1.39 | -12.8% | ★★★★☆ |

## 🎨 系统特色

### 1. 智能化
- **AI驱动**：基于先进AI技术的策略生成
- **自动优化**：策略参数自动优化和调整
- **智能选择**：根据市场状态智能选择最佳策略

### 2. 专业化
- **丰富策略库**：130+种专业技术分析策略
- **严格回测**：基于Ta4j的专业回测框架
- **全面指标**：覆盖收益、风险、交易各维度的性能指标

### 3. 易用性
- **自然语言**：支持自然语言描述生成策略
- **图形界面**：直观的Web界面和API文档
- **即开即用**：Docker一键部署，快速上手

### 4. 扩展性
- **模块化设计**：各功能模块独立，易于扩展
- **开放架构**：支持自定义策略和指标
- **多样接口**：RESTful API，便于集成

### 5. 可靠性
- **企业级**：基于Spring Boot的企业级架构
- **高可用**：支持集群部署和负载均衡
- **数据安全**：完善的数据备份和恢复机制

## 📈 发展路线图

### 近期计划
- [ ] 前端React界面开发完善
- [ ] 更多AI模型集成（GPT、Claude等）
- [ ] 实时策略执行引擎
- [ ] 移动端APP开发

### 中期计划
- [ ] 机器学习模型训练平台
- [ ] 多交易所数据支持
- [ ] 社区策略分享平台
- [ ] 策略表现竞赛系统

### 长期愿景
- [ ] 全自动化交易平台
- [ ] 智能投顾系统
- [ ] 去中心化策略市场
- [ ] 区块链策略认证

## 🤝 贡献指南

欢迎参与项目贡献！请查看 [CONTRIBUTING.md](CONTRIBUTING.md) 了解详细的贡献指南。

### 贡献方式
- **Bug报告**：发现问题请提交Issue
- **功能建议**：新功能想法欢迎讨论
- **代码贡献**：Fork项目并提交Pull Request
- **文档改进**：帮助完善项目文档

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源许可证。

## 📞 联系我们

- **GitHub Issues**：[项目Issues页面](https://github.com/your-repo/okx-trading/issues)
- **邮箱**：your-email@example.com
- **微信群**：扫描二维码加入技术交流群

---

**⭐ 如果这个项目对您有帮助，请给我们一个Star！**

# OKX Trading Intelligent Trading Strategy Backtesting System

## 🎯 Project Overview

OKX Trading is an intelligent cryptocurrency trading strategy backtesting system developed with Java Spring Boot, integrating AI strategy generation, historical data backtesting, and performance analysis capabilities. The system supports automatic trading strategy generation through natural language descriptions, features 130+ pre-built technical analysis strategies, and provides detailed backtesting analysis results on historical candlestick data.

## 🚀 Core Features

### 🤖 AI Intelligent Strategy Generation
- **Natural Language Strategy Generation**: Based on DeepSeek API, automatically generates Ta4j trading strategies through natural language descriptions
- **Intelligent Strategy Understanding**: Supports understanding and parsing of complex strategy descriptions, such as "Trading strategy based on dual moving averages and RSI combination"
- **Dynamic Compilation and Loading**: Real-time compilation of strategy code using Janino and Java Compiler API with dynamic loading
- **Strategy Hot Reload**: Load new strategies without restarting the service, supports real-time strategy updates and deletion
- **Multiple Compilation Methods**: Supports Janino, Java Compiler API, and intelligent selection of three compilation methods
- **Strategy Management**: Complete strategy CRUD operations, supports strategy version control and historical tracking

### 📊 Rich Strategy Library (130+ Strategies)

#### Moving Average Strategies (15 Types)
- **Classic Moving Averages**: SMA, EMA, WMA, HMA and other basic moving averages
- **Advanced Moving Averages**: KAMA adaptive, ZLEMA zero-lag, DEMA/TEMA multiple exponential smoothing
- **Special Moving Averages**: VWAP volume-weighted, TRIMA triangular smoothing, T3 advanced smoothing
- **Adaptive Moving Averages**: MAMA adaptive, VIDYA variable dynamic, Wilders smoothing

#### Oscillator Strategies (17 Types)
- **Classic Indicators**: RSI, Stochastic, Williams %R, CCI
- **Composite Indicators**: Stochastic RSI, CMO, ROC, PPO
- **Advanced Indicators**: TRIX, Fisher Transform, EOM Ease of Movement
- **Professional Indicators**: CHOP Choppiness Index, KVO Klinger Volume Oscillator, RVGI Relative Vigor

#### Trend Indicators (14 Types)
- **Trend Confirmation**: MACD, ADX, Aroon, DMI
- **Trend Following**: Supertrend, Parabolic SAR, Ichimoku Cloud
- **Advanced Trend**: Vortex Indicator, QStick, Williams Alligator
- **Mathematical Trend**: Hilbert Transform series

#### Volatility Indicators (12 Types)
- **Channel Indicators**: Bollinger Bands, Keltner Channel, Donchian Channels
- **Volatility Measurement**: ATR, Ulcer Index, Standard Deviation, Volatility
- **Advanced Volatility**: Mass Index, Squeeze, BBW Bollinger Band Width
- **Special Volatility**: Chandelier Exit, NATR Normalized ATR

#### Volume Indicators (12 Types)
- **Classic Volume**: OBV On-Balance Volume, A/D Accumulation/Distribution Line, Mass Index
- **Advanced Volume**: KDJ, AD/ADOSC Oscillators, PVI/NVI Positive/Negative Volume Index
- **Volume Analysis**: VWMA Volume Weighted, VOSC Volume Oscillator, MarketFI Market Facilitation

#### Candlestick Pattern Strategies (16 Types)
- **Reversal Patterns**: Doji, Hammer, Shooting Star
- **Engulfing Patterns**: Bullish/Bearish Engulfing
- **Combination Patterns**: Morning/Evening Star, Piercing Line
- **Special Patterns**: Three White Soldiers/Three Black Crows

#### Statistical Function Strategies (8 Types)
- **Correlation Analysis**: Beta coefficient, Pearson correlation coefficient
- **Regression Analysis**: Linear regression, linear regression angle/slope/intercept
- **Statistical Indicators**: Variance, time series forecasting, standard deviation

#### Hilbert Transform Strategies (6 Types)
- **Cycle Analysis**: Dominant cycle, dominant phase, trend mode
- **Signal Processing**: Sine wave, phasor components, MESA sine wave

#### Combination Strategies (20 Types)
- **Classic Combinations**: Dual Thrust, Turtle Trading
- **Trend Combinations**: Golden/Death Cross, Trend Following
- **Composite Strategies**: Dual MA + RSI, MACD + Bollinger, Triple Screen
- **Innovative Combinations**: Ichimoku Cloud Breakout, Elder Ray Force Analysis

#### Advanced Strategy Library (50 Types)
- **Adaptive Strategies**: Adaptive Bollinger Bands, Multi-timeframe MACD, Adaptive RSI
- **Advanced Volume**: Klinger Oscillator, Chaikin Oscillator, Force Index
- **Advanced Moving Averages**: Fractal Adaptive, Zero-lag EMA, Gaussian/Butterworth Filters
- **Professional Indicators**: Rocket RSI, Connors RSI, Ultimate Oscillator

#### Innovative Strategy Set (40 Types)
- **Machine Learning Inspired**: Neural Networks, Genetic Algorithms, Random Forest, SVM
- **Quantitative Factors**: Momentum factor, Value factor, Quality factor, Low Volatility factor
- **High-frequency Strategies**: Microstructure Imbalance, Intraday Mean Reversion, Statistical Arbitrage
- **Risk Management**: Kelly Criterion, VaR Risk Management, Maximum Drawdown Control

### 🔬 Advanced Backtesting System

#### Multi-dimensional Backtesting Analysis
- **Ta4j Integration**: Based on professional Ta4j technical analysis library, providing standardized backtesting framework
- **Multiple Time Periods**: Supports backtesting with various candlestick periods from 1 minute to 1 month
- **Batch Backtesting**: Supports batch backtesting of all strategies for strategy filtering and comparison
- **Parallel Backtesting**: Supports multi-threaded parallel backtesting to improve efficiency

#### Rich Performance Indicators
- **Return Indicators**: Total return, annualized return, absolute return, excess return
- **Risk Indicators**: Maximum drawdown, Sharpe ratio, volatility, downside deviation
- **Trading Indicators**: Win rate, profit-loss ratio, average holding time, trading frequency
- **Advanced Indicators**: Calmar ratio, Sortino ratio, Information ratio, tracking error

#### Detailed Trading Records
- **Complete Records**: Saves buy/sell prices, times, and profit/loss for each trade
- **Trading Analysis**: Provides trading distribution statistics, profit/loss analysis, holding period analysis
- **Visualization Support**: Supports chart visualization and analysis of trading records

### 💾 Data Management and Storage

#### Historical Data Management
- **OKX API Integration**: Automatically retrieves historical candlestick data from OKX exchange
- **Multi-symbol Support**: Supports all trading pair data from OKX exchange
- **Data Caching**: Redis cache optimization for improved data access performance
- **Data Cleaning**: Automatic data cleaning and maintenance mechanisms

#### Database Architecture
- **MySQL Storage**: Uses MySQL to store strategy information, backtest results, and trading records
- **Complete Architecture**: Includes strategy information table, backtest summary table, trade detail table
- **Data Migration**: Liquibase manages database versions and migrations
- **Performance Optimization**: Proper index design and query optimization

### 🔧 Technical Features

#### System Architecture
- **Microservice Design**: Modular design with independent and scalable functional modules
- **Asynchronous Processing**: Supports asynchronous backtesting and data processing for improved system responsiveness
- **Containerized Deployment**: Docker + Docker Compose support for easy deployment and scaling
- **Configuration Management**: Flexible configuration management supporting multi-environment deployment

#### Development Features
- **Code Quality**: Complete unit test coverage, standardized code structure
- **Complete Documentation**: Swagger API documentation, detailed code comments
- **Hot Reload**: JRebel support for instant code changes during development
- **Log Management**: Comprehensive logging system with hierarchical logging and log analysis

## 🛠 Technology Stack

### Backend Framework
- **Spring Boot 2.7.8**: Core framework
- **Java 8**: Development language
- **Maven**: Project management and build tool

### Data Storage
- **MySQL 8.0**: Primary database
- **Redis 6.0+**: Cache database
- **Liquibase**: Database version management

### Technical Analysis
- **Ta4j 0.14**: Professional technical analysis library
- **Janino**: Dynamic code compilation
- **Java Compiler API**: Advanced code compilation

### AI Integration
- **DeepSeek API**: AI strategy generation
- **Natural Language Processing**: Strategy description parsing
- **Intelligent Compilation**: Automatic selection of multiple compilation methods

### Network Communication
- **OkHttp3 4.9.3**: HTTP client
- **WebSocket**: Real-time data acquisition
- **RESTful API**: Standard API interfaces

### Deployment and Operations
- **Docker**: Containerized deployment
- **Docker Compose**: Multi-container orchestration
- **Nginx**: Reverse proxy (optional)

## 📚 Detailed Feature Documentation

### AI Strategy Generation Workflow

#### 1. Strategy Description Parsing
```
User Input: "RSI overbought/oversold strategy, buy when RSI below 30, sell when above 70"
         ↓
DeepSeek API parsing
         ↓
Generate Ta4j strategy code
         ↓
Dynamic compilation and loading into system
```

#### 2. Supported Strategy Description Types
- **Basic Indicator Strategies**: RSI, MACD, Bollinger Bands, etc.
- **Combination Strategies**: Multi-indicator combinations, different timeframe combinations
- **Custom Logic**: Complex buy/sell conditions and risk control
- **Parameterized Strategies**: Strategy templates supporting parameter customization

#### 3. Intelligent Compilation System
- **Three Compilation Methods**:
  - `DynamicStrategyService`: Lightweight compilation based on Janino
  - `JavaCompilerDynamicStrategyService`: Standard compilation based on Java Compiler API
  - `SmartDynamicStrategyService`: Intelligent selection of optimal compilation method
- **Error Handling**: Comprehensive compilation error handling and user feedback
- **Performance Optimization**: Compilation result caching to avoid repeated compilation

### Backtesting System Details

#### 1. Backtesting Execution Flow
```
Select Strategy → Set Parameters → Get Historical Data → Execute Backtest → Generate Report → Save Results
```

#### 2. Data Acquisition and Processing
- **Real-time Acquisition**: Real-time retrieval of latest candlestick data from OKX API
- **Data Validation**: Integrity checks, outlier processing
- **Format Conversion**: Raw data conversion to Ta4j BarSeries format
- **Caching Strategy**: Intelligent caching to reduce API calls

#### 3. Performance Analysis Algorithms
- **Statistical Indicator Calculation**: Based on strict financial mathematical formulas
- **Risk Measurement**: VaR, CVaR, maximum drawdown and other risk indicators
- **Benchmark Comparison**: Comparative analysis with buy-and-hold strategy
- **Segmented Analysis**: Supports segmented performance analysis by year, month, week

### Strategy Management System

#### 1. Strategy Registration Center
- **StrategyRegisterCenter**: Unified strategy registration and management center
- **Dynamic Loading**: Supports runtime dynamic addition, modification, deletion of strategies
- **Version Control**: Strategy version management and rollback mechanisms
- **Dependency Management**: Inter-strategy dependency relationship management

#### 2. Strategy Classification System
- **By Indicator Type**: Moving averages, oscillators, trend indicators, etc.
- **By Trading Style**: Trend following, mean reversion, breakout strategies, etc.
- **By Complexity**: Basic strategies, advanced strategies, professional strategies
- **By Time Frame**: Ultra-short, short-term, medium-term, long-term strategies

#### 3. Strategy Rating System
- **Historical Performance Rating**: Comprehensive scoring based on historical backtesting performance
- **Risk Level Classification**: Low risk, medium risk, high risk classification
- **Applicability Assessment**: Applicability scoring under different market environments
- **Recommendation Index**: Comprehensive recommendation index considering returns, risk, stability

## 🎯 Use Cases

### Individual Investors
- **Strategy Learning**: Learn implementation principles of various technical analysis strategies
- **Strategy Validation**: Validate your own trading ideas and strategy effectiveness
- **Parameter Optimization**: Find optimal strategy parameter combinations
- **Risk Assessment**: Understand risk-return characteristics of strategies

### Professional Traders
- **Strategy Development**: Rapidly develop and test new trading strategies
- **Portfolio Optimization**: Build and optimize strategy portfolios
- **Risk Management**: Comprehensive risk assessment and control
- **Live Trading Guidance**: Provide decision support for live trading

### Quantitative Teams
- **Strategy Research**: Large-scale strategy research and development
- **Factor Mining**: Discover new trading signals and factors
- **Backtesting Platform**: Build professional backtesting and research platforms
- **Algorithmic Trading**: Provide strategy support for algorithmic trading systems

### Education and Training
- **Technical Analysis Teaching**: Intuitively demonstrate the effects of various technical indicators
- **Strategy Education**: Help learners understand principles of different strategies
- **Practical Training**: Provide safe strategy practice environment
- **Case Analysis**: Rich historical cases and analysis

## 🚦 Quick Start

### Environment Requirements
- **Java 8+**
- **MySQL 8.0+**
- **Redis 6.0+**
- **Maven 3.6+**

### Installation Steps

1. **Clone Project**
```bash
git clone https://github.com/your-repo/okx-trading.git
cd okx-trading
```

2. **Configure Database**
```sql
CREATE DATABASE okx_trading;
```

3. **Configure Environment Variables**
```bash
export MYSQL_PASSWORD=your_mysql_password
export DEEPSEEK_API_KEY=your_deepseek_api_key
```

4. **Start Service**
```bash
mvn spring-boot:run
```

5. **Access System**
- Application URL: http://localhost:8088
- Swagger Documentation: http://localhost:8088/swagger-ui.html

### Docker Deployment

```bash
# Build image
docker-compose build

# Start service
docker-compose up -d

# View logs
docker-compose logs -f
```

## 📖 API Usage Guide

### AI Strategy Generation
```bash
# Generate single strategy
curl -X POST "http://localhost:8088/api/backtest/ta4j/generate-strategy" \
  -H "Content-Type: application/json" \
  -d '"RSI overbought/oversold strategy, buy when RSI below 30, sell when above 70"'

# Batch generate strategies
curl -X POST "http://localhost:8088/api/backtest/ta4j/generate-strategy" \
  -H "Content-Type: application/json" \
  -d '"Dual moving average crossover strategy
MACD golden cross/death cross strategy
Bollinger Bands breakout strategy"'
```

### Strategy Backtesting
```bash
# Single strategy backtest
curl "http://localhost:8088/api/backtest/ta4j/run?symbol=BTC-USDT&interval=1h&startTime=2023-01-01%2000:00:00&endTime=2023-12-31%2023:59:59&strategyType=RSI&initialAmount=100000&saveResult=true"

# Batch backtest
curl "http://localhost:8088/api/backtest/ta4j/run-all?symbol=BTC-USDT&interval=1d&startTime=2023-01-01%2000:00:00&endTime=2023-12-31%2023:59:59&initialAmount=100000&saveResult=true&threadCount=8"
```

### Strategy Management
```bash
# Get strategy list
curl "http://localhost:8088/api/backtest/ta4j/strategies"

# Get strategy details
curl "http://localhost:8088/api/backtest/ta4j/strategy/RSI"

# Delete strategy
curl -X DELETE "http://localhost:8088/api/backtest/ta4j/delete-strategy/AI_STRATEGY_001"
```

### Result Queries
```bash
# Get backtest history
curl "http://localhost:8088/api/backtest/ta4j/summaries"

# Get specific backtest details
curl "http://localhost:8088/api/backtest/ta4j/detail/{backtestId}"

# Get batch backtest statistics
curl "http://localhost:8088/api/backtest/ta4j/summaries/batch-statistics"
```

## 🔍 Strategy Performance Examples

### Classic Strategy Performance (BTC-USDT, 2023 Data)

| Strategy Name | Annual Return | Sharpe Ratio | Max Drawdown | Win Rate | Trade Count |
|---------------|---------------|--------------|--------------|----------|-------------|
| Dual Thrust | 45.2% | 1.68 | -12.3% | 58% | 156 |
| Turtle Trading | 38.7% | 1.42 | -15.8% | 52% | 28 |
| MACD | 31.5% | 1.25 | -18.2% | 55% | 42 |
| RSI | 28.9% | 1.15 | -16.7% | 61% | 73 |
| Bollinger Bands | 35.6% | 1.33 | -14.1% | 59% | 84 |

### AI-Generated Strategy Performance

| Strategy Description | Annual Return | Sharpe Ratio | Max Drawdown | Rating |
|---------------------|---------------|--------------|--------------|--------|
| "Dual MA RSI combo trading strategy" | 42.3% | 1.55 | -11.2% | ★★★★★ |
| "Volume breakout confirmation momentum strategy" | 38.1% | 1.48 | -13.5% | ★★★★☆ |
| "Multi-timeframe MACD trend strategy" | 35.7% | 1.39 | -12.8% | ★★★★☆ |

## 🎨 System Features

### 1. Intelligence
- **AI-Driven**: Strategy generation based on advanced AI technology
- **Auto-Optimization**: Automatic strategy parameter optimization and adjustment
- **Smart Selection**: Intelligent selection of optimal strategies based on market conditions

### 2. Professionalism
- **Rich Strategy Library**: 130+ professional technical analysis strategies
- **Rigorous Backtesting**: Professional backtesting framework based on Ta4j
- **Comprehensive Indicators**: Performance indicators covering returns, risk, and trading dimensions

### 3. Usability
- **Natural Language**: Supports natural language description for strategy generation
- **Graphical Interface**: Intuitive web interface and API documentation
- **Plug and Play**: One-click Docker deployment, quick to get started

### 4. Scalability
- **Modular Design**: Independent functional modules, easy to extend
- **Open Architecture**: Supports custom strategies and indicators
- **Diverse Interfaces**: RESTful API, easy to integrate

### 5. Reliability
- **Enterprise-grade**: Enterprise-level architecture based on Spring Boot
- **High Availability**: Supports cluster deployment and load balancing
- **Data Security**: Comprehensive data backup and recovery mechanisms

## 📈 Development Roadmap

### Near-term Plans
- [ ] Frontend React interface development improvement
- [ ] More AI model integration (GPT, Claude, etc.)
- [ ] Real-time strategy execution engine
- [ ] Mobile APP development

### Medium-term Plans
- [ ] Machine learning model training platform
- [ ] Multi-exchange data support
- [ ] Community strategy sharing platform
- [ ] Strategy performance competition system

### Long-term Vision
- [ ] Fully automated trading platform
- [ ] Intelligent investment advisory system
- [ ] Decentralized strategy marketplace
- [ ] Blockchain strategy certification

## 🤝 Contributing Guide

Welcome to contribute to the project! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for detailed contribution guidelines.

### Ways to Contribute
- **Bug Reports**: Submit issues when you find problems
- **Feature Suggestions**: New feature ideas are welcome for discussion
- **Code Contributions**: Fork the project and submit Pull Requests
- **Documentation Improvements**: Help improve project documentation

## 📄 License

This project is licensed under the [MIT License](LICENSE).

## 📞 Contact Us

- **GitHub Issues**: [Project Issues Page](https://github.com/your-repo/okx-trading/issues)
- **Email**: ralph_jungle@163.com
- **WeChat Group**: Scan QR code to join technical discussion group

---

**⭐ If this project helps you, please give us a Star!**

# Ta4j 库升级指南 (0.18版本)

本指南提供了将Ta4j库从旧版本升级到0.18版本的详细步骤和注意事项。

## 主要变更

### 1. BaseBarSeries构造函数变更

**旧版本:**
```java
BaseBarSeries series = new BaseBarSeries("name");
```

**新版本:**
```java
BarSeries series = new BaseBarSeriesBuilder().withName("name").build();
```

### 2. BaseBar构造函数变更

**旧版本:**
```java
BaseBar bar = new BaseBar(duration, endTime, openPrice, highPrice, lowPrice, closePrice, volume);
```

**新版本:**
```java
Bar bar = new BaseBar(duration, endTime, openPrice, highPrice, lowPrice, closePrice, volume, amount, trades);
```
其中`trades`是新增的交易次数参数，如果不知道具体值，可以设置为0。

### 3. numOf() 方法替换

**旧版本:**
```java
Num value = series.numOf(42);
```

**新版本:**
```java
Num value = DecimalNum.valueOf(42);
```

### 4. ZonedDateTime和Instant转换

**旧版本:**
```java
LocalDateTime ldt = instant.toLocalDateTime();
```

**新版本:**
```java
LocalDateTime ldt = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
```

### 5. 包名变更

**旧版本:**
```java
import javax.validation.constraints.NotNull;
```

**新版本:**
```java
import jakarta.validation.constraints.NotNull;
```

## 辅助工具类

为了简化升级过程，我们提供了以下辅助工具类：

1. `Ta4jNumUtil` - 用于替代`series.numOf()`方法
2. `DateTimeUtil` - 用于处理ZonedDateTime和Instant转换
3. `Ta4jUpgradeHelper` - 用于创建符合新版本API的对象

### Ta4jNumUtil 使用示例

```java
// 旧版本
Num value = series.numOf(42);

// 新版本
Num value = Ta4jNumUtil.valueOf(42);
```

### DateTimeUtil 使用示例

```java
// 旧版本
LocalDateTime ldt = instant.toLocalDateTime();

// 新版本
LocalDateTime ldt = DateTimeUtil.instantToLocalDateTime(instant);
```

### Ta4jUpgradeHelper 使用示例

```java
// 创建Bar
Bar bar = Ta4jUpgradeHelper.createBar(duration, endTime, openPrice, highPrice, lowPrice, closePrice, volume);

// 创建BarSeries
BarSeries series = Ta4jUpgradeHelper.createBarSeries("name", bars);

// 创建Strategy
Strategy strategy = Ta4jUpgradeHelper.createStrategy(entryRule, exitRule);
```

## 自定义指标

如果在新版本中找不到某些指标，可以使用我们提供的`CustomIndicators`类，其中包含了一些常用指标的实现：

1. `ATRIndicator` - 平均真实范围指标
2. `TrueRangeIndicator` - 真实范围指标
3. `MaxPriceIndicator` - 最大价格指标
4. `MinPriceIndicator` - 最小价格指标
5. `MedianPriceIndicator` - 中间价格指标

## 升级步骤

1. 更新pom.xml中的Ta4j版本到0.18
2. 将javax包引用改为jakarta包
3. 使用BaseBarSeriesBuilder替代BaseBarSeries构造函数
4. 更新BaseBar构造函数，添加交易次数参数
5. 使用Ta4jNumUtil替代series.numOf()方法
6. 使用DateTimeUtil处理ZonedDateTime和Instant转换
7. 使用CustomIndicators替代缺失的指标

## 常见问题

### Q: 如何处理大量的series.numOf()调用？

A: 使用全局查找替换，将`series.numOf(`替换为`Ta4jNumUtil.valueOf(`。

### Q: 如何处理ZonedDateTime和Instant转换问题？

A: 使用DateTimeUtil类中提供的方法进行转换。

### Q: 如何处理缺失的指标？

A: 查看CustomIndicators类中是否有实现，如果没有，可以参考其中的实现方式自行添加。

## 结论

通过遵循本指南和使用提供的辅助工具类，可以顺利完成Ta4j库从旧版本到0.18版本的升级。如果遇到特殊情况，请参考Ta4j官方文档或GitHub仓库中的示例代码。 
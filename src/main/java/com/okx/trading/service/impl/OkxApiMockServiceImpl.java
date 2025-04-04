package com.okx.trading.service.impl;

import com.okx.trading.model.account.AccountBalance;
import com.okx.trading.model.market.Candlestick;
import com.okx.trading.model.market.Ticker;
import com.okx.trading.model.trade.Order;
import com.okx.trading.model.trade.OrderRequest;
import com.okx.trading.service.OkxApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * OKX API模拟服务实现类
 * 用于在不调用真实API的情况下模拟数据
 */
@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "okx.api.use-mock-data", havingValue = "true")
public class OkxApiMockServiceImpl implements OkxApiService {

    private final Map<String, List<Candlestick>> candlestickCache = new ConcurrentHashMap<>();
    private final Map<String,Ticker> tickerCache = new ConcurrentHashMap<>();
    private final Map<String, Order> ordersCache = new ConcurrentHashMap<>();
    private final AtomicLong orderIdGenerator = new AtomicLong(1);

    /**
     * 获取K线数据
     *
     * @param symbol    交易对，如BTC-USDT
     * @param interval  K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param limit     获取数据条数，最大为1000
     * @return K线数据列表
     */
    @Override
    public List<Candlestick> getKlineData(String symbol, String interval, Integer limit) {
        String cacheKey = symbol + "_" + interval;

        if (!candlestickCache.containsKey(cacheKey)) {
            candlestickCache.put(cacheKey, generateMockCandlesticks(symbol, interval, 1000));
        }

        List<Candlestick> dataList = candlestickCache.get(cacheKey);

        int size = limit != null && limit > 0 ? Math.min(limit, dataList.size()) : dataList.size();
        return dataList.subList(0, size);
    }

    /**
     * 获取最新行情数据
     *
     * @param symbol 交易对，如BTC-USDT
     * @return 行情数据
     */
    @Override
    public Ticker getTicker(String symbol) {
        if (!tickerCache.containsKey(symbol)) {
            tickerCache.put(symbol, generateMockTicker(symbol));
        }

        return tickerCache.get(symbol);
    }

    /**
     * 获取账户余额
     *
     * @return 账户余额信息
     */
    @Override
    public AccountBalance getAccountBalance() {
        return generateMockAccountBalance(false);
    }

    /**
     * 获取模拟账户余额
     *
     * @return 模拟账户余额信息
     */
    @Override
    public AccountBalance getSimulatedAccountBalance() {
        return generateMockAccountBalance(true);
    }

    /**
     * 获取订单列表
     *
     * @param symbol    交易对，如BTC-USDT
     * @param status    订单状态：live, partially_filled, filled, cancelled
     * @param limit     获取数据条数，最大为100
     * @return 订单列表
     */
    @Override
    public List<Order> getOrders(String symbol, String status, Integer limit) {
        List<Order> filteredOrders = ordersCache.values().stream()
                .filter(order -> (symbol == null || symbol.equals(order.getSymbol())))
                .filter(order -> (status == null || status.equalsIgnoreCase(order.getStatus())))
                .collect(Collectors.toList());

        int size = limit != null && limit > 0 ? Math.min(limit, filteredOrders.size()) : filteredOrders.size();
        return filteredOrders.subList(0, Math.min(size, filteredOrders.size()));
    }

    /**
     * 创建现货订单
     *
     * @param orderRequest 订单请求参数
     * @return 创建的订单
     */
    @Override
    public Order createSpotOrder(OrderRequest orderRequest) {
        return createOrder(orderRequest, false);
    }

    /**
     * 创建合约订单
     *
     * @param orderRequest 订单请求参数
     * @return 创建的订单
     */
    @Override
    public Order createFuturesOrder(OrderRequest orderRequest) {
        return createOrder(orderRequest, true);
    }

    /**
     * 取消订单
     *
     * @param symbol  交易对，如BTC-USDT
     * @param orderId 订单ID
     * @return 是否成功
     */
    @Override
    public boolean cancelOrder(String symbol, String orderId) {
        if (ordersCache.containsKey(orderId)) {
            Order order = ordersCache.get(orderId);
            if (symbol.equals(order.getSymbol())) {
                order.setStatus("CANCELED");
                order.setUpdateTime(LocalDateTime.now());
                ordersCache.put(orderId, order);
                return true;
            }
        }
        return false;
    }

    /**
     * 生成模拟K线数据
     *
     * @param symbol    交易对
     * @param interval  K线间隔
     * @param count     数据条数
     * @return K线数据列表
     */
    private List<Candlestick> generateMockCandlesticks(String symbol, String interval, int count) {
        List<Candlestick> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 基础价格，根据币种设置不同的基础价格
        BigDecimal basePrice = getBasePrice(symbol);
        BigDecimal lastClose = basePrice;

        for (int i = count - 1; i >= 0; i--) {
            LocalDateTime time = now.minusMinutes(i * getMinutesFromInterval(interval));

            // 随机波动，但保持一定的连续性
            BigDecimal range = basePrice.multiply(new BigDecimal("0.02")); // 2%的价格波动范围
            BigDecimal randomChange = new BigDecimal(ThreadLocalRandom.current().nextDouble(-1.0, 1.0))
                    .multiply(range);

            BigDecimal open = lastClose;
            BigDecimal close = open.add(randomChange);

            // 确保价格为正
            if (close.compareTo(BigDecimal.ZERO) <= 0) {
                close = open.multiply(new BigDecimal("0.95")); // 如果为负，则下跌5%
            }

            // 高低价在开盘价和收盘价的基础上波动
            BigDecimal high = open.max(close).add(range.multiply(new BigDecimal(ThreadLocalRandom.current().nextDouble(0, 0.5))));
            BigDecimal low = open.min(close).subtract(range.multiply(new BigDecimal(ThreadLocalRandom.current().nextDouble(0, 0.5))));

            // 成交量随机
            BigDecimal volume = new BigDecimal(ThreadLocalRandom.current().nextDouble(1000, 10000));

            Candlestick candlestick = new Candlestick();
            candlestick.setSymbol(symbol);
            candlestick.setInterval(interval);
            candlestick.setOpenTime(time);
            candlestick.setOpen(open);
            candlestick.setHigh(high);
            candlestick.setLow(low);
            candlestick.setClose(close);
            candlestick.setVolume(volume);
            candlestick.setQuoteVolume(volume.multiply(close));
            candlestick.setCloseTime(time.plusMinutes(getMinutesFromInterval(interval) - 1));
            candlestick.setTrades(ThreadLocalRandom.current().nextLong(100, 1000));

            result.add(candlestick);

            // 下一个周期的开盘价是当前周期的收盘价
            lastClose = close;
        }

        return result;
    }

    /**
     * 生成模拟Ticker数据
     *
     * @param symbol 交易对
     * @return Ticker数据
     */
    private Ticker generateMockTicker(String symbol) {
        BigDecimal basePrice = getBasePrice(symbol);

        // 随机价格波动，在基础价格上下5%范围内
        BigDecimal range = basePrice.multiply(new BigDecimal("0.05"));
        BigDecimal lastPrice = basePrice.add(new BigDecimal(ThreadLocalRandom.current().nextDouble(-1, 1)).multiply(range));

        // 确保价格为正
        if (lastPrice.compareTo(BigDecimal.ZERO) <= 0) {
            lastPrice = basePrice;
        }

        // 24小时前的价格
        BigDecimal open24h = lastPrice.multiply(new BigDecimal("0.95"));
        // 价格变动
        BigDecimal priceChange = lastPrice.subtract(open24h);
        BigDecimal priceChangePercent = priceChange.multiply(new BigDecimal("100")).divide(open24h, 2, BigDecimal.ROUND_HALF_UP);

        // 24小时高低价
        BigDecimal highPrice = lastPrice.multiply(new BigDecimal("1.05"));
        BigDecimal lowPrice = lastPrice.multiply(new BigDecimal("0.95"));

        // 成交量和成交额
        BigDecimal volume = new BigDecimal(ThreadLocalRandom.current().nextDouble(10000, 100000));
        BigDecimal quoteVolume = volume.multiply(lastPrice);

        // 买卖价格
        BigDecimal bidPrice = lastPrice.multiply(new BigDecimal("0.999"));
        BigDecimal askPrice = lastPrice.multiply(new BigDecimal("1.001"));

        // 买卖数量
        BigDecimal bidQty = new BigDecimal(ThreadLocalRandom.current().nextDouble(1, 10));
        BigDecimal askQty = new BigDecimal(ThreadLocalRandom.current().nextDouble(1, 10));

        Ticker ticker = new Ticker();
        ticker.setSymbol(symbol);
        ticker.setLastPrice(lastPrice);
        ticker.setPriceChange(priceChange);
        ticker.setPriceChangePercent(priceChangePercent);
        ticker.setHighPrice(highPrice);
        ticker.setLowPrice(lowPrice);
        ticker.setVolume(volume);
        ticker.setQuoteVolume(quoteVolume);
        ticker.setBidPrice(bidPrice);
        ticker.setBidQty(bidQty);
        ticker.setAskPrice(askPrice);
        ticker.setAskQty(askQty);
        ticker.setTimestamp(LocalDateTime.now());

        return ticker;
    }

    /**
     * 生成模拟账户余额数据
     *
     * @param isSimulated 是否为模拟账户
     * @return 账户余额数据
     */
    private AccountBalance generateMockAccountBalance(boolean isSimulated) {
        List<AccountBalance.AssetBalance> assetBalances = new ArrayList<>();

        // 添加几种常见币种
        assetBalances.add(createAssetBalance("BTC", "1.2345", "0.1234", "50000"));
        assetBalances.add(createAssetBalance("ETH", "15.6789", "2.3456", "3000"));
        assetBalances.add(createAssetBalance("USDT", "10000.5678", "2000.1234", "1"));
        assetBalances.add(createAssetBalance("OKB", "500.1234", "100.5678", "5"));

        // 计算总资产价值
        BigDecimal totalEquity = assetBalances.stream()
                .map(AccountBalance.AssetBalance::getUsdValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 可用余额（假设是总资产的80%）
        BigDecimal availableBalance = totalEquity.multiply(new BigDecimal("0.8"));

        // 冻结余额（总资产的20%）
        BigDecimal frozenBalance = totalEquity.subtract(availableBalance);

        AccountBalance accountBalance = new AccountBalance();
        accountBalance.setTotalEquity(totalEquity);
        accountBalance.setAccountType(isSimulated ? 1 : 0);
        accountBalance.setAccountId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        accountBalance.setAvailableBalance(availableBalance);
        accountBalance.setFrozenBalance(frozenBalance);
        accountBalance.setAssetBalances(assetBalances);

        return accountBalance;
    }

    /**
     * 创建资产余额
     *
     * @param asset     币种
     * @param available 可用余额
     * @param frozen    冻结余额
     * @param price     单价(USD)
     * @return 资产余额
     */
    private AccountBalance.AssetBalance createAssetBalance(String asset, String available, String frozen, String price) {
        BigDecimal availableBD = new BigDecimal(available);
        BigDecimal frozenBD = new BigDecimal(frozen);
        BigDecimal totalBD = availableBD.add(frozenBD);
        BigDecimal priceBD = new BigDecimal(price);

        AccountBalance.AssetBalance assetBalance = new AccountBalance.AssetBalance();
        assetBalance.setAsset(asset);
        assetBalance.setAvailable(availableBD);
        assetBalance.setFrozen(frozenBD);
        assetBalance.setTotal(totalBD);
        assetBalance.setUsdValue(totalBD.multiply(priceBD));

        return assetBalance;
    }

    /**
     * 创建订单（内部方法）
     *
     * @param orderRequest 订单请求参数
     * @param isFutures    是否为合约订单
     * @return 创建的订单
     */
    private Order createOrder(OrderRequest orderRequest, boolean isFutures) {
        // 生成唯一订单ID
        String orderId = String.valueOf(orderIdGenerator.getAndIncrement());
        String clientOrderId = orderRequest.getClientOrderId() != null ?
                orderRequest.getClientOrderId() :
                "mock_" + orderId;

        // 获取当前市场价格
        Ticker ticker = getTicker(orderRequest.getSymbol());
        BigDecimal marketPrice = ticker.getLastPrice();

        // 计算实际价格和数量
        BigDecimal price;
        BigDecimal quantity;

        // 根据订单类型设置价格
        if ("LIMIT".equalsIgnoreCase(orderRequest.getType())) {
            price = orderRequest.getPrice();
        } else {
            // 市价单使用当前市场价格
            price = marketPrice;
        }

        // 获取订单方向
        boolean isBuy = "BUY".equalsIgnoreCase(orderRequest.getSide());

        // 交易对信息解析
        String[] symbolParts = orderRequest.getSymbol().split("-");
        String baseAsset = symbolParts[0]; // 基础资产，如BTC
        String quoteAsset = symbolParts.length > 1 ? symbolParts[1] : "USDT"; // 计价资产，如USDT

        // 按优先级确定数量
        if (orderRequest.getQuantity() != null && orderRequest.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            // 1. 优先使用指定的数量
            quantity = orderRequest.getQuantity();
        } else if (orderRequest.getAmount() != null && orderRequest.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            // 2. 其次使用金额
            if (isBuy) {
                // 买入：数量 = 金额 / 价格
                quantity = orderRequest.getAmount().divide(price, 8, BigDecimal.ROUND_DOWN);
            } else {
                // 卖出：直接使用金额作为数量
                quantity = orderRequest.getAmount();
            }
        } else if (isBuy && orderRequest.getBuyRatio() != null &&
                 orderRequest.getBuyRatio().compareTo(BigDecimal.ZERO) > 0) {
            // 3. 按可用余额比例买入
            // 获取账户余额
            AccountBalance accountBalance = isFutures ? getSimulatedAccountBalance() : getAccountBalance();

            // 找到计价资产的可用余额（如USDT）
            BigDecimal availableBalance = accountBalance.getAssetBalances().stream()
                    .filter(asset -> quoteAsset.equals(asset.getAsset()))
                    .findFirst()
                    .map(AccountBalance.AssetBalance::getAvailable)
                    .orElse(BigDecimal.ZERO);

            // 计算要使用的金额 = 可用余额 * 比例
            BigDecimal amountToUse = availableBalance.multiply(orderRequest.getBuyRatio());

            // 计算数量 = 金额 / 价格
            quantity = amountToUse.divide(price, 8, BigDecimal.ROUND_DOWN);

            log.info("按比例买入：可用{}余额 = {}, 使用比例 = {}, 买入金额 = {}, 买入数量 = {}",
                    quoteAsset, availableBalance, orderRequest.getBuyRatio(), amountToUse, quantity);

        } else if (!isBuy && orderRequest.getSellRatio() != null &&
                  orderRequest.getSellRatio().compareTo(BigDecimal.ZERO) > 0) {
            // 4. 按持仓比例卖出
            // 获取账户余额
            AccountBalance accountBalance = isFutures ? getSimulatedAccountBalance() : getAccountBalance();

            // 找到基础资产的总余额（如BTC）
            BigDecimal totalHolding = accountBalance.getAssetBalances().stream()
                    .filter(asset -> baseAsset.equals(asset.getAsset()))
                    .findFirst()
                    .map(AccountBalance.AssetBalance::getTotal)
                    .orElse(BigDecimal.ZERO);

            // 计算要卖出的数量 = 总持仓 * 比例
            quantity = totalHolding.multiply(orderRequest.getSellRatio());

            log.info("按比例卖出：持有{}数量 = {}, 使用比例 = {}, 卖出数量 = {}",
                    baseAsset, totalHolding, orderRequest.getSellRatio(), quantity);

        } else {
            // 如果没有提供有效的数量、金额或比例，抛出异常
            throw new IllegalArgumentException("下单失败：必须指定数量、金额或比例");
        }

        // 确保数量大于零
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("下单失败：计算得到的数量必须大于0");
        }

        // 生成手续费，通常是订单金额的0.1%
        BigDecimal fee = price.multiply(quantity).multiply(new BigDecimal("0.001"));
        String feeCurrency = quoteAsset; // 使用计价货币作为手续费

        // 创建订单对象
        Order order = new Order();
        order.setOrderId(orderId);
        order.setClientOrderId(clientOrderId);
        order.setSymbol(orderRequest.getSymbol());
        order.setPrice(price);
        order.setOrigQty(quantity);
        order.setExecutedQty(BigDecimal.ZERO); // 新订单未成交
        order.setCummulativeQuoteQty(BigDecimal.ZERO); // 新订单未成交
        order.setStatus("NEW");
        order.setType(orderRequest.getType());
        order.setSide(orderRequest.getSide());
        order.setTimeInForce(orderRequest.getTimeInForce() != null ? orderRequest.getTimeInForce() : "GTC");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setSimulated(orderRequest.getSimulated() != null ? orderRequest.getSimulated() : false);
        order.setFee(fee);
        order.setFeeCurrency(feeCurrency);

        // 保存到订单缓存
        ordersCache.put(orderId, order);

        return order;
    }

    /**
     * 根据币种获取基础价格
     *
     * @param symbol 交易对
     * @return 基础价格
     */
    private BigDecimal getBasePrice(String symbol) {
        if (symbol.startsWith("BTC")) {
            return new BigDecimal("50000");
        } else if (symbol.startsWith("ETH")) {
            return new BigDecimal("3000");
        } else if (symbol.startsWith("OKB")) {
            return new BigDecimal("5");
        } else {
            return new BigDecimal("100"); // 默认价格
        }
    }

    /**
     * 根据K线间隔获取分钟数
     *
     * @param interval K线间隔
     * @return 分钟数
     */
    private int getMinutesFromInterval(String interval) {
        if (interval.endsWith("m")) {
            return Integer.parseInt(interval.substring(0, interval.length() - 1));
        } else if (interval.endsWith("H")) {
            return Integer.parseInt(interval.substring(0, interval.length() - 1)) * 60;
        } else if (interval.endsWith("D")) {
            return Integer.parseInt(interval.substring(0, interval.length() - 1)) * 24 * 60;
        } else if (interval.endsWith("W")) {
            return Integer.parseInt(interval.substring(0, interval.length() - 1)) * 7 * 24 * 60;
        } else if (interval.endsWith("M")) {
            return Integer.parseInt(interval.substring(0, interval.length() - 1)) * 30 * 24 * 60;
        } else {
            return 1; // 默认1分钟
        }
    }
}

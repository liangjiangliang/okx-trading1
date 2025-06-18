package com.okx.trading.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class BenchmarkUtils {

    // 缓存机制：避免重复调用API
    private static final ConcurrentHashMap<String, List<BigDecimal>> priceCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<BigDecimal>> returnCache = new ConcurrentHashMap<>();
    private static final ReentrantLock cacheLock = new ReentrantLock();
    
    // 请求限流：避免API限制
    private static long lastRequestTime = 0;
    private static final long REQUEST_INTERVAL = 1000; // 1秒间隔

    /**
     * 从Yahoo Finance获取指定标的的历史收盘价（CSV格式）
     * 添加缓存机制，相同参数的请求会直接返回缓存结果
     *
     * @param symbol    指数或股票代码，例如 "^GSPC" 表示标普500，"000300.SS" 表示沪深300
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 收盘价列表，按日期升序排列
     */
    public static List<BigDecimal> fetchHistoricalClosePrices(String symbol, ZonedDateTime startTime, ZonedDateTime endTime) throws Exception {
        
        // 生成缓存键
        String cacheKey = String.format("%s_%s_%s", symbol, 
            String.valueOf(startTime.toEpochSecond()), String.valueOf(endTime.toEpochSecond()));
        
        // 检查缓存
        if (priceCache.containsKey(cacheKey)) {
            System.out.println("使用缓存的价格数据: " + symbol);
            return new ArrayList<>(priceCache.get(cacheKey));
        }

        cacheLock.lock();
        try {
            // 双重检查锁定
            if (priceCache.containsKey(cacheKey)) {
                return new ArrayList<>(priceCache.get(cacheKey));
            }

            // 请求限流
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRequestTime < REQUEST_INTERVAL) {
                Thread.sleep(REQUEST_INTERVAL - (currentTime - lastRequestTime));
            }
            lastRequestTime = System.currentTimeMillis();

            long period1 = startTime.toEpochSecond();
            long period2 = endTime.toEpochSecond();
            String urlStr = String.format(
                    "https://query1.finance.yahoo.com/v7/finance/download/%s?period1=%s&period2=%s&interval=1d&events=history&includeAdjustedClose=true",
                    symbol, String.valueOf(period1), String.valueOf(period2));

            System.out.println("正在获取基准数据: " + symbol + " 从 " + startTime.toLocalDate() + " 到 " + endTime.toLocalDate());
            
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000); // 10秒连接超时
            conn.setReadTimeout(15000);    // 15秒读取超时
            
            // 添加User-Agent避免被拒绝
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            List<BigDecimal> closePrices = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false; // 跳过标题行
                        continue;
                    }
                    String[] parts = line.split(",");
                    if (parts.length < 6) continue;

                    String closeStr = parts[4]; // 第5列是Close价格
                    if ("null".equalsIgnoreCase(closeStr) || closeStr.isEmpty()) continue;

                    try {
                        double close = Double.parseDouble(closeStr);
                        closePrices.add(BigDecimal.valueOf(close));
                    } catch (NumberFormatException e) {
                        // 跳过无效数据
                        continue;
                    }
                }
            }
            
            // 缓存结果
            if (!closePrices.isEmpty()) {
                priceCache.put(cacheKey, new ArrayList<>(closePrices));
                System.out.println("成功获取并缓存价格数据，共 " + closePrices.size() + " 个数据点");
            }
            
            return closePrices;
            
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * 计算每日简单收益率
     * r_t = (P_t - P_{t-1}) / P_{t-1}
     *
     * @param prices 收盘价列表，按时间升序排列
     * @return 每日收益率列表，长度 = prices.size() - 1
     */
    public static List<BigDecimal> calculateDailyReturns(List<BigDecimal> prices) {
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < prices.size(); i++) {
            BigDecimal prev = prices.get(i - 1);
            BigDecimal current = prices.get(i);
            if (prev.compareTo(BigDecimal.ZERO) == 0) continue;
            
            BigDecimal ret = current.subtract(prev).divide(prev, 8, RoundingMode.HALF_UP);
            returns.add(ret);
        }
        return returns;
    }

    /**
     * 获取基准收益率（带缓存）
     * 
     * @param symbol    基准标的代码
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 基准日收益率列表
     */
    public static List<BigDecimal> getBenchmarkReturns(String symbol, ZonedDateTime startTime, ZonedDateTime endTime) {
        // 生成缓存键
        String cacheKey = String.format("%s_returns_%s_%s", symbol, 
            String.valueOf(startTime.toEpochSecond()), String.valueOf(endTime.toEpochSecond()));
        
        // 检查收益率缓存
        if (returnCache.containsKey(cacheKey)) {
            System.out.println("使用缓存的收益率数据: " + symbol);
            return new ArrayList<>(returnCache.get(cacheKey));
        }

        try {
            List<BigDecimal> prices = fetchHistoricalClosePrices(symbol, startTime, endTime);
            List<BigDecimal> returns = calculateDailyReturns(prices);
            
            // 缓存收益率
            if (!returns.isEmpty()) {
                returnCache.put(cacheKey, new ArrayList<>(returns));
            }
            
            return returns;
        } catch (Exception e) {
            System.err.println("获取基准收益率失败: " + e.getMessage());
            // 返回空列表而不是抛出异常
            return new ArrayList<>();
        }
    }

    /**
     * 清理缓存（可选，用于内存管理）
     */
    public static void clearCache() {
        cacheLock.lock();
        try {
            priceCache.clear();
            returnCache.clear();
            System.out.println("基准数据缓存已清理");
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * 获取缓存统计信息
     */
    public static String getCacheStats() {
        return String.format("价格缓存: %d 项, 收益率缓存: %d 项", 
            priceCache.size(), returnCache.size());
    }

    public static void main(String[] args) throws Exception {
        // 测试缓存功能
        ZonedDateTime start = ZonedDateTime.now().minusDays(30);
        ZonedDateTime end = ZonedDateTime.now();
        
        System.out.println("第一次调用...");
        List<BigDecimal> returns1 = getBenchmarkReturns("^GSPC", start, end);
        
        System.out.println("第二次调用...");
        List<BigDecimal> returns2 = getBenchmarkReturns("^GSPC", start, end);
        
        System.out.println("收益率数量: " + returns1.size());
        System.out.println(getCacheStats());
    }
} 
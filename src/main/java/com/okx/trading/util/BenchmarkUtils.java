package com.okx.trading.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class BenchmarkUtils {

    /**
     * 从Yahoo Finance获取指定标的的历史收盘价（CSV格式）
     *
     * @param symbol  指数或股票代码，例如 "^GSPC" 表示标普500，"000300.SS" 表示沪深300
     * @param period1 开始时间，Unix时间戳秒
     * @param period2 结束时间，Unix时间戳秒
     * @return List<Double> 收盘价列表，按日期升序排列
     */
    public static List<BigDecimal> fetchHistoricalClosePrices(String symbol, ZonedDateTime startTime, ZonedDateTime endTime) throws Exception {

        double period1 = startTime.toLocalDateTime().toInstant(ZoneOffset.of("+8")).toEpochMilli() / 1000;
        double period2 = endTime.toLocalDateTime().toInstant(ZoneOffset.of("+8")).toEpochMilli() / 1000;
        String urlStr = String.format(
                "https://query1.finance.yahoo.com/v7/finance/download/%s?period1=%d&period2=%d&interval=1d&events=history&includeAdjustedClose=true",
                symbol, period1, period2);

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

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

                double close = Double.parseDouble(closeStr);
                closePrices.add(BigDecimal.valueOf(close));
            }
        }
        return closePrices;
    }

    /**
     * 计算每日简单收益率
     * r_t = (P_t - P_{t-1}) / P_{t-1}
     *
     * @param prices 收盘价列表，按时间升序排列
     * @return 每日收益率列表，长度 = prices.size() - 1
     */
    public static List<Double> calculateDailyReturns(List<Double> prices) {
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < prices.size(); i++) {
            double prev = prices.get(i - 1);
            if (prev == 0) continue;
            double ret = (prices.get(i) - prev) / prev;
            returns.add(ret);
        }
        return returns;
    }

    public static void main(String[] args) throws Exception {
        // 以标普500为例：代码 ^GSPC
        // 时间范围，单位秒，举例：2023-01-01 ~ 2023-06-01
        long period1 = 1672531200; // 2023-01-01 00:00:00 UTC
        long period2 = 1685577600; // 2023-06-01 00:00:00 UTC

        String benchmarkSymbol = "^GSPC";

//        List<Double> closePrices = fetchHistoricalClosePrices(benchmarkSymbol, BigDecimal.valueOf(period1), BigDecimal.valueOf(period2));
//        List<Double> dailyReturns = calculateDailyReturns(closePrices);
//
//        System.out.println("基准收盘价数量: " + closePrices.size());
//        System.out.println("基准日收益率数量: " + dailyReturns.size());
//
//        // 打印前5个收益率示例
//        for (int i = 0; i < Math.min(5, dailyReturns.size()); i++) {
//            System.out.printf("Day %d return: %.6f\n", i + 1, dailyReturns.get(i));
//        }
    }
}

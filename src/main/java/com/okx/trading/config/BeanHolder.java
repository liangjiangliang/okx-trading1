package com.okx.trading.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BeanHolder {
    private static BacktestParameterConfig config;

    @Autowired
    public BeanHolder(BacktestParameterConfig config) {
        BeanHolder.config = config;
    }

    public static BacktestParameterConfig getBacktestParameterConfig() {
        return config;
    }
}

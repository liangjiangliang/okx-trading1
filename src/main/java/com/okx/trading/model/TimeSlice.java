package com.okx.trading.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 时间分片类
 * 用于将时间范围分割成多个小的时间片段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlice {
    private LocalDateTime start;
    private LocalDateTime end;

    @Override
    public String toString() {
        return String.format("[%s - %s]", start, end);
    }
} 
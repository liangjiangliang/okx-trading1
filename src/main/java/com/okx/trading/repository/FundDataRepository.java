package com.okx.trading.repository;

import com.okx.trading.model.entity.FundDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FundDataRepository extends JpaRepository<FundDataEntity, Long> {

    /**
     * 查询指定时间范围内的资金数据
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 资金数据列表
     */
    List<FundDataEntity> findByRecordTimeBetweenOrderByRecordTimeAsc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询最近一条记录
     * @return 最新的资金数据
     */
    FundDataEntity findTopByOrderByRecordTimeDesc();
    
    /**
     * 按照时间间隔查询数据（避免数据过多）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 资金数据列表
     */
    @Query(value = "SELECT * FROM fund_data " +
            "WHERE record_time BETWEEN ?1 AND ?2 " +
            "ORDER BY record_time ASC", nativeQuery = true)
    List<FundDataEntity> findByTimeRangeWithInterval(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查询当天的资金数据
     * @param startTime 当天开始时间
     * @param endTime 当天结束时间
     * @return 资金数据列表
     */
    @Query(value = "SELECT * FROM fund_data " +
            "WHERE record_time BETWEEN ?1 AND ?2 " +
            "ORDER BY record_time ASC", nativeQuery = true)
    List<FundDataEntity> findTodayData(LocalDateTime startTime, LocalDateTime endTime);
} 
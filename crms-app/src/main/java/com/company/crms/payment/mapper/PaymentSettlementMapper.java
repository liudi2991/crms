package com.company.crms.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.crms.payment.entity.PaymentSettlement;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PaymentSettlementMapper extends BaseMapper<PaymentSettlement> {

    @Select("SELECT * FROM payment_settlement WHERE payment_record_id = #{recordId} FOR UPDATE")
    List<PaymentSettlement> selectByRecordForUpdate(@Param("recordId") Long recordId);

    @Select("SELECT * FROM payment_settlement WHERE payment_plan_id = #{planId}")
    List<PaymentSettlement> listByPlan(@Param("planId") Long planId);

    @Delete("DELETE FROM payment_settlement WHERE payment_record_id = #{recordId}")
    int deleteByRecord(@Param("recordId") Long recordId);
}

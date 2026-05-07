package com.company.crms.system.mapper;

import com.company.crms.system.vo.RecycleBinItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 跨表回收站查询。所有 SQL 都显式带 is_deleted = 1，绕开 MP 逻辑删除拦截。
 */
@Mapper
public interface RecycleBinMapper {

    // ============ 客户 ============

    @Select("""
        SELECT 'CUSTOMER' AS bizType, id, code, name, updated_by AS updatedBy, updated_at AS updatedAt
        FROM customer
        WHERE is_deleted = 1
          AND (#{kw} IS NULL OR name LIKE CONCAT('%', #{kw}, '%') OR code LIKE CONCAT('%', #{kw}, '%'))
        ORDER BY updated_at DESC
        LIMIT #{offset}, #{size}
        """)
    List<RecycleBinItemVO> pageCustomer(@Param("kw") String kw,
                                        @Param("offset") int offset,
                                        @Param("size") int size);

    @Select("""
        SELECT COUNT(1) FROM customer WHERE is_deleted = 1
          AND (#{kw} IS NULL OR name LIKE CONCAT('%', #{kw}, '%') OR code LIKE CONCAT('%', #{kw}, '%'))
        """)
    long countCustomer(@Param("kw") String kw);

    @Select("SELECT * FROM customer WHERE id = #{id}")
    Map<String, Object> selectCustomerSnapshot(@Param("id") Long id);

    @Update("UPDATE customer SET is_deleted = 0, updated_at = NOW() WHERE id = #{id} AND is_deleted = 1")
    int restoreCustomer(@Param("id") Long id);

    @Update("DELETE FROM customer WHERE id = #{id} AND is_deleted = 1")
    int hardDeleteCustomer(@Param("id") Long id);

    // ============ 合同 ============

    @Select("""
        SELECT 'CONTRACT' AS bizType, id, code, name, updated_by AS updatedBy, updated_at AS updatedAt
        FROM contract
        WHERE is_deleted = 1
          AND (#{kw} IS NULL OR name LIKE CONCAT('%', #{kw}, '%') OR code LIKE CONCAT('%', #{kw}, '%'))
        ORDER BY updated_at DESC
        LIMIT #{offset}, #{size}
        """)
    List<RecycleBinItemVO> pageContract(@Param("kw") String kw,
                                        @Param("offset") int offset,
                                        @Param("size") int size);

    @Select("""
        SELECT COUNT(1) FROM contract WHERE is_deleted = 1
          AND (#{kw} IS NULL OR name LIKE CONCAT('%', #{kw}, '%') OR code LIKE CONCAT('%', #{kw}, '%'))
        """)
    long countContract(@Param("kw") String kw);

    @Select("SELECT * FROM contract WHERE id = #{id}")
    Map<String, Object> selectContractSnapshot(@Param("id") Long id);

    @Update("UPDATE contract SET is_deleted = 0, updated_at = NOW() WHERE id = #{id} AND is_deleted = 1")
    int restoreContract(@Param("id") Long id);

    @Update("DELETE FROM contract WHERE id = #{id} AND is_deleted = 1")
    int hardDeleteContract(@Param("id") Long id);

    // ============ 回款 ============

    @Select("""
        SELECT 'PAYMENT_RECORD' AS bizType, id,
               CAST(id AS CHAR) AS code,
               CONCAT('合同 ', contract_id, ' / ', amount) AS name,
               updated_by AS updatedBy, updated_at AS updatedAt
        FROM payment_record
        WHERE is_deleted = 1
          AND (#{kw} IS NULL OR voucher_no LIKE CONCAT('%', #{kw}, '%') OR payer LIKE CONCAT('%', #{kw}, '%'))
        ORDER BY updated_at DESC
        LIMIT #{offset}, #{size}
        """)
    List<RecycleBinItemVO> pagePaymentRecord(@Param("kw") String kw,
                                             @Param("offset") int offset,
                                             @Param("size") int size);

    @Select("""
        SELECT COUNT(1) FROM payment_record WHERE is_deleted = 1
          AND (#{kw} IS NULL OR voucher_no LIKE CONCAT('%', #{kw}, '%') OR payer LIKE CONCAT('%', #{kw}, '%'))
        """)
    long countPaymentRecord(@Param("kw") String kw);

    @Select("SELECT * FROM payment_record WHERE id = #{id}")
    Map<String, Object> selectPaymentRecordSnapshot(@Param("id") Long id);

    @Update("UPDATE payment_record SET is_deleted = 0, updated_at = NOW() WHERE id = #{id} AND is_deleted = 1")
    int restorePaymentRecord(@Param("id") Long id);

    @Update("DELETE FROM payment_record WHERE id = #{id} AND is_deleted = 1")
    int hardDeletePaymentRecord(@Param("id") Long id);
}

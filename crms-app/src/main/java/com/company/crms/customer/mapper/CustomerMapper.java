package com.company.crms.customer.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.company.crms.customer.entity.Customer;
import com.company.crms.customer.vo.CustomerAggregateVO;
import com.company.crms.customer.vo.CustomerDuplicateVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 受数据范围保护的分页查询。方法名后缀 {@code WithDataScope} 触发拦截器。
     */
    @Select("""
        <script>
        SELECT * FROM customer WHERE is_deleted = 0
          <choose>
            <when test="ew != null and ew.nonEmptyOfWhere">AND ${ew.sqlSegment}</when>
            <when test="ew != null and ew.sqlSegment != null and ew.sqlSegment != ''">${ew.sqlSegment}</when>
          </choose>
        </script>
        """)
    IPage<Customer> selectPageWithDataScope(IPage<Customer> page,
                                            @Param(Constants.WRAPPER) Wrapper<Customer> wrapper);

    /** 查询客户下未终止的合同数量。 */
    @Select("""
        SELECT COUNT(1) FROM contract
         WHERE customer_id = #{customerId}
           AND is_deleted = 0
           AND status NOT IN ('TERMINATED','EXPIRED','COMPLETED')
        """)
    long countActiveContracts(@Param("customerId") Long customerId);

    /** 物理删除（绕过 @TableLogic）。 */
    @Delete("DELETE FROM customer WHERE id = #{id}")
    int physicalDelete(@Param("id") Long id);

    /** 名称/USCC 查重（排除自身、已合并、已软删的）。 */
    @Select("""
        <script>
        SELECT id, code, name, uscc, status,
          CASE WHEN uscc IS NOT NULL AND uscc != '' AND uscc = #{uscc} THEN 'USCC' ELSE 'NAME' END AS hitField
        FROM customer
        WHERE is_deleted = 0
          AND status != 'MERGED'
          <if test="selfId != null">AND id != #{selfId}</if>
          AND (
            (#{name} IS NOT NULL AND #{name} != '' AND name LIKE CONCAT('%', #{name}, '%'))
            <if test="uscc != null and uscc != ''">OR uscc = #{uscc}</if>
          )
        ORDER BY (uscc = #{uscc}) DESC, id DESC
        LIMIT 10
        </script>
        """)
    List<CustomerDuplicateVO> checkDuplicate(@Param("name") String name,
                                             @Param("uscc") String uscc,
                                             @Param("selfId") Long selfId);

    /** 客户最近合同（最多 10 条）。 */
    @Select("""
        SELECT id, code, name, amount, status, signed_at AS signedAt
        FROM contract
        WHERE customer_id = #{customerId} AND is_deleted = 0
        ORDER BY signed_at DESC
        LIMIT 10
        """)
    List<CustomerAggregateVO.RecentContract> recentContracts(@Param("customerId") Long customerId);

    @Select("SELECT COUNT(1) FROM contract WHERE customer_id = #{customerId} AND is_deleted = 0")
    long countContracts(@Param("customerId") Long customerId);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM contract WHERE customer_id = #{customerId} AND is_deleted = 0")
    BigDecimal sumContractAmount(@Param("customerId") Long customerId);

    /** 合并：把被合并客户的合同改写到主体客户。 */
    @Update({
        "<script>",
        "UPDATE contract SET customer_id = #{mainId}, updated_at = NOW()",
        " WHERE is_deleted = 0 AND customer_id IN ",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    int reassignContracts(@Param("ids") List<Long> ids, @Param("mainId") Long mainId);

    /** 合并：把被合并客户的联系人改写到主体客户（取消原主联系人状态）。 */
    @Update({
        "<script>",
        "UPDATE customer_contact SET customer_id = #{mainId}, is_primary = 0, updated_at = NOW()",
        " WHERE is_deleted = 0 AND customer_id IN ",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    int reassignContacts(@Param("ids") List<Long> ids, @Param("mainId") Long mainId);

    /** 合并：把被合并客户标记 merged_to=mainId, status=MERGED, is_deleted=1。 */
    @Update({
        "<script>",
        "UPDATE customer SET merged_to = #{mainId}, status = 'MERGED', is_deleted = 1,",
        " updated_at = NOW(), updated_by = #{operatorId}",
        " WHERE is_deleted = 0 AND id IN ",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    int markMerged(@Param("ids") List<Long> ids,
                   @Param("mainId") Long mainId,
                   @Param("operatorId") Long operatorId);
}

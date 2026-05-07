package com.company.crms.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.crms.customer.entity.CustomerContact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CustomerContactMapper extends BaseMapper<CustomerContact> {

    /** 在指定客户范围内，将所有联系人 isPrimary 重置为 0。 */
    @Update("UPDATE customer_contact SET is_primary = 0, updated_at = NOW() " +
            "WHERE customer_id = #{customerId} AND is_deleted = 0")
    int clearPrimary(@Param("customerId") Long customerId);

    /** 合并：把被合并客户的联系人全部迁移到主体客户。 */
    @Update("UPDATE customer_contact SET customer_id = #{mainId}, is_primary = 0, updated_at = NOW() " +
            "WHERE customer_id IN (SELECT * FROM (SELECT id FROM customer WHERE id = #{fromId}) t)")
    int reassignTo(@Param("fromId") Long fromId, @Param("mainId") Long mainId);
}

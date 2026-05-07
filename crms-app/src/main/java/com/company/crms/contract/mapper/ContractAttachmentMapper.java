package com.company.crms.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.crms.contract.entity.ContractAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ContractAttachmentMapper extends BaseMapper<ContractAttachment> {

    @Select("SELECT COUNT(1) FROM contract_attachment WHERE contract_id = #{contractId} AND is_deleted = 0")
    long countByContract(@Param("contractId") Long contractId);
}

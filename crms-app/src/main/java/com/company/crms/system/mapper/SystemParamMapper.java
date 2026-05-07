package com.company.crms.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.crms.system.entity.SystemParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SystemParamMapper extends BaseMapper<SystemParam> {

    @Select("SELECT * FROM system_param WHERE param_key = #{key}")
    SystemParam selectByKey(@Param("key") String key);
}

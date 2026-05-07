package com.company.crms.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.crms.notification.entity.NotificationSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NotificationSettingMapper extends BaseMapper<NotificationSetting> {

    @Select("SELECT * FROM notification_setting WHERE user_id = #{userId}")
    List<NotificationSetting> listByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM notification_setting WHERE user_id = #{userId} AND scene = #{scene} LIMIT 1")
    NotificationSetting findByUserScene(@Param("userId") Long userId, @Param("scene") String scene);
}

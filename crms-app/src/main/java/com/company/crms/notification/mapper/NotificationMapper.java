package com.company.crms.notification.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.company.crms.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 去重检查：是否在最近 since 时间内向同一用户已发送过同 scene + biz_id 的通知。
     */
    @Select("""
        SELECT COUNT(1) FROM notification
         WHERE receiver_id = #{receiverId}
           AND scene = #{scene}
           AND (biz_id <=> #{bizId})
           AND created_at >= #{since}
           AND archived = 0
        """)
    long countDuplicate(@Param("receiverId") Long receiverId,
                        @Param("scene") String scene,
                        @Param("bizId") Long bizId,
                        @Param("since") LocalDateTime since);

    @Select("""
        <script>
        SELECT * FROM notification
         WHERE receiver_id = #{receiverId}
           <choose>
             <when test="ew != null and ew.nonEmptyOfWhere">AND ${ew.sqlSegment}</when>
             <when test="ew != null and ew.sqlSegment != null and ew.sqlSegment != ''">${ew.sqlSegment}</when>
           </choose>
        </script>
        """)
    IPage<Notification> selectPageForUser(IPage<Notification> page,
                                          @Param("receiverId") Long receiverId,
                                          @Param(Constants.WRAPPER) Wrapper<Notification> wrapper);

    @Select("""
        SELECT COUNT(1) FROM notification
         WHERE receiver_id = #{receiverId} AND is_read = 0 AND archived = 0
        """)
    long countUnread(@Param("receiverId") Long receiverId);

    @Update("""
        UPDATE notification
           SET is_read = 1, read_at = NOW()
         WHERE id = #{id} AND receiver_id = #{receiverId} AND is_read = 0
        """)
    int markRead(@Param("id") Long id, @Param("receiverId") Long receiverId);

    @Update("""
        UPDATE notification
           SET is_read = 1, read_at = NOW()
         WHERE receiver_id = #{receiverId} AND is_read = 0 AND archived = 0
        """)
    int markAllRead(@Param("receiverId") Long receiverId);

    @Update("""
        UPDATE notification SET archived = 1
         WHERE id = #{id} AND receiver_id = #{receiverId}
        """)
    int archive(@Param("id") Long id, @Param("receiverId") Long receiverId);
}

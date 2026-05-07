package com.company.crms.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.crms.iam.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("""
        SELECT * FROM iam_user
         WHERE username = #{username} AND is_deleted = 0
        """)
    User selectByUsername(@Param("username") String username);

    @Select("""
        SELECT r.code FROM iam_role r
         JOIN iam_user_role ur ON ur.role_id = r.id
         WHERE ur.user_id = #{userId} AND r.is_deleted = 0
        """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT DISTINCT rp.permission_code
          FROM iam_role_permission rp
          JOIN iam_user_role ur ON ur.role_id = rp.role_id
         WHERE ur.user_id = #{userId}
        """)
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT data_scope FROM iam_role r
         JOIN iam_user_role ur ON ur.role_id = r.id
         WHERE ur.user_id = #{userId} AND r.is_deleted = 0
         ORDER BY FIELD(data_scope, 'ALL', 'DEPT', 'SELF')
         LIMIT 1
        """)
    String selectMaxDataScope(@Param("userId") Long userId);
}

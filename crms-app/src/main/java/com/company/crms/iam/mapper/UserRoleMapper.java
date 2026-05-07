package com.company.crms.iam.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户-角色关系。无业务实体，直接 SQL。
 */
@Mapper
public interface UserRoleMapper {

    @Select("SELECT role_id FROM iam_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO iam_user_role (id, user_id, role_id) VALUES (#{id}, #{userId}, #{roleId})")
    int insert(@Param("id") Long id,
               @Param("userId") Long userId,
               @Param("roleId") Long roleId);

    @Delete("DELETE FROM iam_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM iam_user_role WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT COUNT(*) FROM iam_user_role WHERE role_id = #{roleId}")
    long countByRoleId(@Param("roleId") Long roleId);
}

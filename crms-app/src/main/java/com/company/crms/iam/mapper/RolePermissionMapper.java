package com.company.crms.iam.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-权限点关系。
 */
@Mapper
public interface RolePermissionMapper {

    @Select("SELECT permission_code FROM iam_role_permission WHERE role_id = #{roleId}")
    List<String> selectCodesByRoleId(@Param("roleId") Long roleId);

    @Insert("INSERT INTO iam_role_permission (id, role_id, permission_code) VALUES (#{id}, #{roleId}, #{code})")
    int insert(@Param("id") Long id,
               @Param("roleId") Long roleId,
               @Param("code") String code);

    @Delete("DELETE FROM iam_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);
}

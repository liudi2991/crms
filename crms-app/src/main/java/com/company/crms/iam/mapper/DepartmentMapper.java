package com.company.crms.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.crms.iam.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {

    /** 取自身 + 全部子孙部门 ID。 */
    @Select("""
        WITH RECURSIVE tree AS (
            SELECT id, parent_id FROM iam_department WHERE id = #{deptId} AND is_deleted = 0
            UNION ALL
            SELECT d.id, d.parent_id FROM iam_department d
              JOIN tree t ON d.parent_id = t.id
             WHERE d.is_deleted = 0
        )
        SELECT id FROM tree
        """)
    List<Long> selectSelfAndDescendantIds(@Param("deptId") Long deptId);

    /** 直接子部门数量（不递归）。 */
    @Select("SELECT COUNT(*) FROM iam_department WHERE parent_id = #{deptId} AND is_deleted = 0")
    long countDirectChildren(@Param("deptId") Long deptId);

    /** 部门下用户数量（不递归）。 */
    @Select("SELECT COUNT(*) FROM iam_user WHERE dept_id = #{deptId} AND is_deleted = 0")
    long countUsersInDept(@Param("deptId") Long deptId);
}

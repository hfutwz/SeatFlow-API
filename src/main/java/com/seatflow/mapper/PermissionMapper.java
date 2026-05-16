package com.seatflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seatflow.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("SELECT DISTINCT p.* FROM t_permission p " +
            "INNER JOIN t_role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN t_user_role ur ON rp.role_id = ur.role_id " +
            "INNER JOIN t_role r ON rp.role_id = r.id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0")
    List<Permission> selectByUserId(Long userId);

    @Select("<script>" +
            "SELECT p.* FROM t_permission p " +
            "INNER JOIN t_role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id IN " +
            "<foreach item='item' index='index' open='(' separator=',' close=')' collection='roleIds'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    List<Permission> selectPermissionsByRoleIds(List<Long> roleIds);
}

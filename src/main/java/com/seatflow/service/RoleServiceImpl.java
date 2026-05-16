package com.seatflow.service;

import com.seatflow.entity.Permission;
import com.seatflow.entity.Role;
import com.seatflow.mapper.PermissionMapper;
import com.seatflow.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RBAC 权限服务
 *
 * <p>提供基于角色的权限查询能力：
 * <ul>
 *   <li>根据用户 ID 获取其所有角色的权限码列表</li>
 *   <li>多角色用户权限取并集</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    /**
     * 获取指定用户的所有权限码列表（多角色取并集）
     *
     * @param userId 用户 ID
     * @return 权限码列表，无角色则返回空列表
     */
    public List<String> getUserPermissions(Long userId) {
        // 查询用户所有角色
        List<Role> roles = roleMapper.selectRolesByUserId(userId);

        if (roles.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取角色 ID 列表
        List<Long> roleIds = roles.stream()
                .map(Role::getId)
                .collect(Collectors.toList());

        // 批量查询各角色关联的权限
        List<Permission> permissions = permissionMapper.selectPermissionsByRoleIds(roleIds);

        // 返回去重的权限码列表
        return permissions.stream()
                .map(Permission::getCode)
                .distinct()
                .collect(Collectors.toList());
    }
}

package com.seatflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.seatflow.entity.Permission;
import com.seatflow.entity.Role;
import com.seatflow.mapper.PermissionMapper;
import com.seatflow.mapper.RoleMapper;
import com.seatflow.mapper.UserRoleMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RBAC (Role-Based Access Control) 单元测试 — 基于 PRD F010-F012
 *
 * <p>验证角色权限模型核心行为：
 * <ul>
 *   <li>T-RBAC-01: 学生角色不含管理权限</li>
 *   <li>T-RBAC-02: super_admin 包含全部权限</li>
 *   <li>T-RBAC-03: 多角色用户权限取并集</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

    @Mock private RoleMapper roleMapper;
    @Mock private PermissionMapper permissionMapper;
    @Mock private UserRoleMapper userRoleMapper;

    @InjectMocks private RoleServiceImpl roleService;

    private Permission reservationView;
    private Permission violationView;
    private Permission reservationManage;
    private Permission seatManage;
    private Permission roomManage;
    private Permission systemConfig;
    private Permission roleManage;
    private Permission userManage;

    private Role superAdminRole;
    private Role roomAdminRole;
    private Role serviceAdminRole;
    private Role viewerRole;
    private Role studentRole;

    @BeforeEach
    void setUp() {
        // 8 个权限项 (PRD F011)
        reservationView = perm(1L, "reservation:view", "查看预约记录");
        violationView = perm(2L, "violation:view", "查看违约记录");
        reservationManage = perm(3L, "reservation:manage", "为用户预约和取消预约");
        seatManage = perm(4L, "seat:manage", "座位登记和注销");
        roomManage = perm(5L, "room:manage", "自习室登记和注销");
        systemConfig = perm(6L, "system:config", "调整系统参数");
        roleManage = perm(7L, "role:manage", "角色和权限管理");
        userManage = perm(8L, "user:manage", "用户角色分配");

        // 默认角色 (PRD F010)
        superAdminRole = role(1L, "super_admin", "超级管理员");
        roomAdminRole = role(2L, "room_admin", "自习室管理员");
        serviceAdminRole = role(3L, "service_admin", "服务管理员");
        viewerRole = role(4L, "viewer", "查看员");
        studentRole = role(5L, "student", "学生");
    }

    private Permission perm(Long id, String code, String name) {
        Permission p = new Permission();
        p.setId(id);
        p.setCode(code);
        p.setName(name);
        return p;
    }

    private Role role(Long id, String code, String name) {
        Role r = new Role();
        r.setId(id);
        r.setCode(code);
        r.setName(name);
        return r;
    }

    // ==================== T-RBAC-01: 学生角色权限 ====================
    @Test
    @DisplayName("should not contain management permissions for student role")
    void shouldNotContainManagementPermissionsForStudent() {
        // Given: 学生仅有基础预约权限
        when(roleMapper.selectRolesByUserId(2L)).thenReturn(List.of(studentRole));
        when(permissionMapper.selectPermissionsByRoleIds(anyList()))
                .thenReturn(List.of(reservationView));

        // When
        List<String> permissions = roleService.getUserPermissions(2L);

        // Then
        assertThat(permissions)
                .doesNotContain("room:manage", "role:manage", "seat:manage",
                        "system:config", "user:manage", "reservation:manage");
    }

    // ==================== T-RBAC-02: 管理员角色权限 ====================
    @Test
    @DisplayName("should contain all permissions for super_admin role")
    void shouldContainAllPermissionsForSuperAdmin() {
        // Given
        when(roleMapper.selectRolesByUserId(1L)).thenReturn(List.of(superAdminRole));
        when(permissionMapper.selectPermissionsByRoleIds(anyList()))
                .thenReturn(Arrays.asList(
                        reservationView, violationView, reservationManage,
                        seatManage, roomManage, systemConfig, roleManage, userManage));

        // When
        List<String> permissions = roleService.getUserPermissions(1L);

        // Then
        assertThat(permissions).hasSize(8);
        assertThat(permissions).containsExactlyInAnyOrder(
                "reservation:view", "violation:view", "reservation:manage",
                "seat:manage", "room:manage", "system:config", "role:manage", "user:manage");
    }

    // ==================== T-RBAC-03: 多角色用户权限取并集 ====================
    @Test
    @DisplayName("should return union of permissions when user has multiple roles")
    void shouldReturnUnionOfPermissionsForMultipleRoles() {
        // Given: 用户同时拥有 room_admin 和 service_admin
        when(roleMapper.selectRolesByUserId(3L))
                .thenReturn(List.of(roomAdminRole, serviceAdminRole));

        // room_admin: room:manage, seat:manage, reservation:view
        // service_admin: reservation:view, violation:view, reservation:manage
        // 并集应包含 5 个权限（reservation:view 不重复）
        when(permissionMapper.selectPermissionsByRoleIds(anyList()))
                .thenReturn(Arrays.asList(
                        roomManage, seatManage, reservationView,
                        violationView, reservationManage));

        // When
        List<String> permissions = roleService.getUserPermissions(3L);

        // Then
        assertThat(permissions).hasSize(5);
        assertThat(permissions).contains(
                "room:manage", "seat:manage", "reservation:view",
                "violation:view", "reservation:manage");
    }

    // ==================== 异常路径：无角色用户 ====================
    @Test
    @DisplayName("should return empty permissions when user has no roles")
    void shouldReturnEmptyPermissionsWhenUserHasNoRoles() {
        when(roleMapper.selectRolesByUserId(99L)).thenReturn(Collections.emptyList());
        when(permissionMapper.selectPermissionsByRoleIds(anyList()))
                .thenReturn(Collections.emptyList());

        List<String> permissions = roleService.getUserPermissions(99L);

        assertThat(permissions).isEmpty();
    }

    // ==================== 异常路径：viewer 角色仅查看 ====================
    @Test
    @DisplayName("should only contain view permissions for viewer role")
    void shouldOnlyContainViewPermissionsForViewer() {
        when(roleMapper.selectRolesByUserId(4L)).thenReturn(List.of(viewerRole));
        when(permissionMapper.selectPermissionsByRoleIds(anyList()))
                .thenReturn(List.of(reservationView, violationView));

        List<String> permissions = roleService.getUserPermissions(4L);

        assertThat(permissions).hasSize(2);
        assertThat(permissions).containsExactlyInAnyOrder("reservation:view", "violation:view");
        assertThat(permissions).doesNotContain("reservation:manage", "room:manage");
    }
}

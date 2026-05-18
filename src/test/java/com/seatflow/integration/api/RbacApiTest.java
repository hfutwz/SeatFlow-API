package com.seatflow.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatflow.entity.Permission;
import com.seatflow.entity.Role;
import com.seatflow.entity.User;
import com.seatflow.mapper.PermissionMapper;
import com.seatflow.mapper.RoleMapper;
import com.seatflow.mapper.UserMapper;
import com.seatflow.security.CustomUserDetails;
import com.seatflow.security.JwtTokenProvider;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/**
 * RBAC API 集成测试 — 基于 PRD F010-F012
 *
 * <p>验证管理端接口的权限拦截行为：
 * <ul>
 *   <li>super_admin 可访问所有管理接口</li>
 *   <li>学生角色访问管理接口 → 403</li>
 *   <li>无权限用户访问需权限接口 → 403</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class RbacApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockBean private UserDetailsService userDetailsService;
    @MockBean private UserMapper userMapper;
    @MockBean private RoleMapper roleMapper;
    @MockBean private PermissionMapper permissionMapper;

    private User adminUser;
    private User studentUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setPassword("$2a$10$encoded");
        adminUser.setRealName("系统管理员");
        adminUser.setEmail("admin@seatflow.com");
        adminUser.setUserType("ADMIN");

        studentUser = new User();
        studentUser.setId(2L);
        studentUser.setUsername("student01");
        studentUser.setPassword("$2a$10$encoded");
        studentUser.setRealName("张三");
        studentUser.setEmail("zhangsan@seatflow.com");
        studentUser.setDepartmentId(1L);
        studentUser.setUserType("STUDENT");
    }

    // ==================== T-RBAC-02: 管理员可访问管理接口 ====================
    @Test
    @DisplayName("GET /api/admin/roles as super_admin → 200")
    void shouldAllowSuperAdminToAccessRoleList() throws Exception {
        List<String> adminPerms = List.of("room:manage", "seat:manage", "reservation:manage", "system:config", "role:manage", "user:manage", "reservation:view", "violation:view");
        when(userDetailsService.loadUserByUsername("admin"))
                .thenReturn(buildUserDetails(adminUser, adminPerms));

        String token = jwtTokenProvider.generateToken(1L, "admin", List.of("super_admin"));

        mockMvc.perform(get("/api/admin/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/admin/users as super_admin → 200")
    void shouldAllowSuperAdminToAccessUserList() throws Exception {
        List<String> adminPerms = List.of("room:manage", "seat:manage", "reservation:manage", "system:config", "role:manage", "user:manage", "reservation:view", "violation:view");
        when(userDetailsService.loadUserByUsername("admin"))
                .thenReturn(buildUserDetails(adminUser, adminPerms));

        String token = jwtTokenProvider.generateToken(1L, "admin", List.of("super_admin"));

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ==================== T-RBAC-01: 学生不可访问管理接口 ====================
    @Test
    @DisplayName("GET /api/admin/roles as student → 403")
    void shouldDenyStudentAccessToRoleList() throws Exception {
        when(userDetailsService.loadUserByUsername("student01"))
                .thenReturn(buildUserDetails(studentUser, List.of("reservation:view", "reservation:create")));

        String token = jwtTokenProvider.generateToken(2L, "student01", List.of("student"));

        mockMvc.perform(get("/api/admin/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is(403)));
    }

    @Test
    @DisplayName("POST /api/admin/rooms as student → 403")
    void shouldDenyStudentAccessToCreateRoom() throws Exception {
        when(userDetailsService.loadUserByUsername("student01"))
                .thenReturn(buildUserDetails(studentUser, List.of("reservation:view", "reservation:create")));

        String token = jwtTokenProvider.generateToken(2L, "student01", List.of("student"));

        mockMvc.perform(post("/api/admin/rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试教室\",\"location\":\"测试楼\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is(403)));
    }

    // ==================== T-ROUTE-03 (后端等价): 无权限用户访问需权限接口 ====================
    @Test
    @DisplayName("PUT /api/admin/configs as viewer (no system:config) → 403")
    void shouldDenyViewerAccessToSystemConfig() throws Exception {
        User viewerUser = new User();
        viewerUser.setId(4L);
        viewerUser.setUsername("viewer01");
        viewerUser.setUserType("ADMIN");

        when(userDetailsService.loadUserByUsername("viewer01"))
                .thenReturn(buildUserDetails(viewerUser, List.of("reservation:view", "violation:view")));

        String token = jwtTokenProvider.generateToken(4L, "viewer01", List.of("viewer"));

        mockMvc.perform(put("/api/admin/configs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configs\":{\"max_reservation_hours\":\"6\"}}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is(403)));
    }

    // ==================== 异常路径：未登录访问管理接口 ====================
    @Test
    @DisplayName("GET /api/admin/roles without token → 401")
    void shouldReturn401WhenAccessingAdminEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/roles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is(401)));
    }

    @Test
    @DisplayName("DELETE /api/admin/rooms/1 without token → 401")
    void shouldReturn401WhenDeletingRoomWithoutToken() throws Exception {
        mockMvc.perform(delete("/api/admin/rooms/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is(401)));
    }

    // ==================== Helper methods ====================
    private CustomUserDetails buildUserDetails(User user, List<String> permissions) {
        List<SimpleGrantedAuthority> authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getUserType(),
                user.getDepartmentId(),
                List.of(),
                authorities
        );
    }

    private Role superAdminRole() {
        Role r = new Role(); r.setId(1L); r.setCode("super_admin"); r.setName("超级管理员"); return r;
    }
    private Role studentRole() {
        Role r = new Role(); r.setId(5L); r.setCode("student"); r.setName("学生"); return r;
    }
    private Role viewerRole() {
        Role r = new Role(); r.setId(4L); r.setCode("viewer"); r.setName("查看员"); return r;
    }

    private Permission[] allPermissions() {
        String[] codes = {
                "reservation:view", "violation:view", "reservation:manage",
                "seat:manage", "room:manage", "system:config", "role:manage", "user:manage"
        };
        return createPermissions(codes);
    }

    private Permission[] studentPermissions() {
        return createPermissions(new String[]{"reservation:view", "reservation:create"});
    }

    private Permission[] viewerPermissions() {
        return createPermissions(new String[]{"reservation:view", "violation:view"});
    }

    private Permission[] createPermissions(String[] codes) {
        Permission[] perms = new Permission[codes.length];
        for (int i = 0; i < codes.length; i++) {
            perms[i] = new Permission();
            perms[i].setId((long) (i + 1));
            perms[i].setCode(codes[i]);
        }
        return perms;
    }
}

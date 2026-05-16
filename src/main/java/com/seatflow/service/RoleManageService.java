package com.seatflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatflow.common.exception.BusinessException;
import com.seatflow.dto.request.RoleCreateRequest;
import com.seatflow.dto.request.RoleUpdateRequest;
import com.seatflow.dto.response.PermissionResponse;
import com.seatflow.dto.response.RoleResponse;
import com.seatflow.entity.Permission;
import com.seatflow.entity.Role;
import com.seatflow.entity.RolePermission;
import com.seatflow.mapper.PermissionMapper;
import com.seatflow.mapper.RoleMapper;
import com.seatflow.mapper.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleManageService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public List<RoleResponse> listAll() {
        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
        return roles.stream().map(this::toRoleResponse).collect(Collectors.toList());
    }

    public RoleResponse getById(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException(404, "角色不存在");
        return toRoleResponse(role);
    }

    @Transactional
    public RoleResponse create(RoleCreateRequest request) {
        // Check code uniqueness
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getCode, request.getCode());
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "角色编码已存在");
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        roleMapper.insert(role);

        // Assign permissions
        if (request.getPermissionIds() != null) {
            for (Long permId : request.getPermissionIds()) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(permId);
                rolePermissionMapper.insert(rp);
            }
        }
        return toRoleResponse(roleMapper.selectById(role.getId()));
    }

    @Transactional
    public RoleResponse update(Long id, RoleUpdateRequest request) {
        Role role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException(404, "角色不存在");

        if (request.getName() != null) role.setName(request.getName());
        if (request.getDescription() != null) role.setDescription(request.getDescription());
        roleMapper.updateById(role);

        // Update permissions
        if (request.getPermissionIds() != null) {
            LambdaQueryWrapper<RolePermission> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(RolePermission::getRoleId, id);
            rolePermissionMapper.delete(deleteWrapper);
            for (Long permId : request.getPermissionIds()) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(id);
                rp.setPermissionId(permId);
                rolePermissionMapper.insert(rp);
            }
        }
        return toRoleResponse(roleMapper.selectById(id));
    }

    @Transactional
    public void delete(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException(404, "角色不存在");
        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getRoleId, id);
        rolePermissionMapper.delete(wrapper);
        roleMapper.deleteById(id);
    }

    public List<PermissionResponse> listPermissions() {
        List<Permission> perms = permissionMapper.selectList(new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getId));
        return perms.stream().map(p -> PermissionResponse.builder()
                .id(p.getId()).name(p.getName()).code(p.getCode()).build())
                .collect(Collectors.toList());
    }

    private RoleResponse toRoleResponse(Role role) {
        LambdaQueryWrapper<RolePermission> rpWrapper = new LambdaQueryWrapper<>();
        rpWrapper.eq(RolePermission::getRoleId, role.getId());
        List<Long> permIds = rolePermissionMapper.selectList(rpWrapper).stream()
                .map(RolePermission::getPermissionId).collect(Collectors.toList());

        List<PermissionResponse> permResponses = permIds.stream().map(pid -> {
            Permission p = permissionMapper.selectById(pid);
            return p != null ? PermissionResponse.builder().id(p.getId()).name(p.getName()).code(p.getCode()).build() : null;
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .code(role.getCode())
                .description(role.getDescription())
                .permissionIds(permIds)
                .permissions(permResponses)
                .build();
    }
}

package com.seatflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatflow.common.exception.BusinessException;
import com.seatflow.dto.request.UserCreateRequest;
import com.seatflow.dto.request.UserUpdateRequest;
import com.seatflow.dto.response.UserResponse;
import com.seatflow.entity.*;
import com.seatflow.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManageService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final DepartmentMapper departmentMapper;
    private final PasswordEncoder passwordEncoder;

    public Page<UserResponse> list(int page, int size, String userType, String keyword) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (userType != null) wrapper.eq(User::getUserType, userType);
        if (keyword != null) wrapper.like(User::getUsername, keyword).or().like(User::getRealName, keyword);
        wrapper.orderByDesc(User::getId);

        Page<User> result = userMapper.selectPage(pageParam, wrapper);
        Page<UserResponse> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(result.getRecords().stream().map(this::toUserResponse).collect(Collectors.toList()));
        return responsePage;
    }

    public UserResponse getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        // Check username uniqueness
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setDepartmentId(request.getDepartmentId());
        user.setUserType(request.getUserType() != null ? request.getUserType() : "STUDENT");
        userMapper.insert(user);
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");

        if (request.getRealName() != null) user.setRealName(request.getRealName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getDepartmentId() != null) user.setDepartmentId(request.getDepartmentId());
        if (request.getUserType() != null) user.setUserType(request.getUserType());
        userMapper.updateById(user);

        // Update roles
        if (request.getRoleIds() != null) {
            // Delete old roles
            LambdaQueryWrapper<UserRole> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(UserRole::getUserId, id);
            userRoleMapper.delete(deleteWrapper);
            // Insert new roles
            for (Long roleId : request.getRoleIds()) {
                UserRole ur = new UserRole();
                ur.setUserId(id);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
        return toUserResponse(userMapper.selectById(id));
    }

    @Transactional
    public void delete(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");
        // Delete user roles
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, id);
        userRoleMapper.delete(wrapper);
        userMapper.deleteById(id);
    }

    private UserResponse toUserResponse(User user) {
        List<Role> roles = roleMapper.selectByUserId(user.getId());
        List<Permission> permissions = permissionMapper.selectByUserId(user.getId());

        // Get role IDs for this user
        LambdaQueryWrapper<UserRole> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(UserRole::getUserId, user.getId());
        List<Long> roleIds = userRoleMapper.selectList(urWrapper).stream()
                .map(UserRole::getRoleId).collect(Collectors.toList());

        String deptName = null;
        if (user.getDepartmentId() != null) {
            Department dept = departmentMapper.selectById(user.getDepartmentId());
            deptName = dept != null ? dept.getName() : null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .departmentId(user.getDepartmentId())
                .departmentName(deptName)
                .userType(user.getUserType())
                .roles(roles.stream().map(Role::getCode).collect(Collectors.toList()))
                .roleIds(roleIds)
                .permissions(permissions.stream().map(Permission::getCode).collect(Collectors.toList()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}

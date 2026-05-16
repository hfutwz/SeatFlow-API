package com.seatflow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatflow.common.result.Result;
import com.seatflow.dto.request.UserCreateRequest;
import com.seatflow.dto.request.UserUpdateRequest;
import com.seatflow.dto.response.UserResponse;
import com.seatflow.service.UserManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserManageService userManageService;

    @GetMapping
    public Result<Page<UserResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String keyword) {
        return Result.ok(userManageService.list(page, size, userType, keyword));
    }

    @GetMapping("/{id}")
    public Result<UserResponse> getById(@PathVariable Long id) {
        return Result.ok(userManageService.getById(id));
    }

    @PostMapping
    public Result<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.ok(userManageService.create(request));
    }

    @PutMapping("/{id}")
    public Result<UserResponse> update(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return Result.ok(userManageService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userManageService.delete(id);
        return Result.ok();
    }
}

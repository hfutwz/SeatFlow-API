package com.seatflow.controller;

import com.seatflow.common.result.Result;
import com.seatflow.dto.request.RoleCreateRequest;
import com.seatflow.dto.request.RoleUpdateRequest;
import com.seatflow.dto.response.PermissionResponse;
import com.seatflow.dto.response.RoleResponse;
import com.seatflow.service.RoleManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final RoleManageService roleManageService;

    @GetMapping
    public Result<List<RoleResponse>> listAll() {
        return Result.ok(roleManageService.listAll());
    }

    @GetMapping("/{id}")
    public Result<RoleResponse> getById(@PathVariable Long id) {
        return Result.ok(roleManageService.getById(id));
    }

    @PostMapping
    public Result<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        return Result.ok(roleManageService.create(request));
    }

    @PutMapping("/{id}")
    public Result<RoleResponse> update(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        return Result.ok(roleManageService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleManageService.delete(id);
        return Result.ok();
    }

    @GetMapping("/permissions")
    public Result<List<PermissionResponse>> listPermissions() {
        return Result.ok(roleManageService.listPermissions());
    }
}

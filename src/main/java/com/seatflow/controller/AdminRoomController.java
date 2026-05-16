package com.seatflow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatflow.common.result.Result;
import com.seatflow.dto.request.RoomCreateRequest;
import com.seatflow.dto.request.RoomUpdateRequest;
import com.seatflow.dto.response.RoomResponse;
import com.seatflow.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
public class AdminRoomController {

    private final RoomService roomService;

    /**
     * A-ROOM03: 管理端自习室列表（全量+分页）
     */
    @GetMapping
    public Result<Page<RoomResponse>> listRooms(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name) {
        return Result.ok(roomService.listForAdmin(page, size, status, name));
    }

    /**
     * A-ROOM04: 创建自习室
     */
    @PostMapping
    public Result<RoomResponse> create(@Valid @RequestBody RoomCreateRequest request) {
        return Result.ok(roomService.create(request));
    }

    /**
     * A-ROOM05: 更新自习室
     */
    @PutMapping("/{id}")
    public Result<RoomResponse> update(@PathVariable Long id, @RequestBody RoomUpdateRequest request) {
        return Result.ok(roomService.update(id, request));
    }

    /**
     * A-ROOM06: 注销自习室（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        roomService.remove(id);
        return Result.ok();
    }
}

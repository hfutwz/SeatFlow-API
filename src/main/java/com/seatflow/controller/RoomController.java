package com.seatflow.controller;

import com.seatflow.common.result.Result;
import com.seatflow.dto.response.RoomDetailResponse;
import com.seatflow.dto.response.RoomResponse;
import com.seatflow.entity.User;
import com.seatflow.mapper.UserMapper;
import com.seatflow.security.SecurityUtils;
import com.seatflow.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final UserMapper userMapper;

    /**
     * A-ROOM01: 学生端自习室列表（有权限+开放的）
     */
    @GetMapping
    public Result<List<RoomResponse>> listRooms() {
        Long userId = SecurityUtils.getCurrentUserId();
        Long departmentId = null;
        if (userId != null) {
            User user = userMapper.selectById(userId);
            if (user != null) departmentId = user.getDepartmentId();
        }
        return Result.ok(roomService.listForStudent(departmentId));
    }

    /**
     * A-ROOM02: 自习室详情（含座位概览）
     */
    @GetMapping("/{id}")
    public Result<RoomDetailResponse> getRoomDetail(@PathVariable Long id) {
        return Result.ok(roomService.getDetail(id));
    }
}

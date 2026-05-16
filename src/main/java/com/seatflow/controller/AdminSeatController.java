package com.seatflow.controller;

import com.seatflow.common.result.Result;
import com.seatflow.dto.request.SeatCreateRequest;
import com.seatflow.dto.request.SeatUpdateRequest;
import com.seatflow.dto.response.SeatResponse;
import com.seatflow.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rooms/{roomId}/seats")
@RequiredArgsConstructor
public class AdminSeatController {

    private final SeatService seatService;

    /**
     * A-SEAT03: 创建座位（单个）
     */
    @PostMapping
    public Result<SeatResponse> create(@PathVariable Long roomId, @Valid @RequestBody SeatCreateRequest request) {
        return Result.ok(seatService.create(roomId, request));
    }

    /**
     * A-SEAT03: 批量创建座位
     */
    @PostMapping("/batch")
    public Result<List<SeatResponse>> batchCreate(@PathVariable Long roomId, @Valid @RequestBody List<SeatCreateRequest> requests) {
        return Result.ok(seatService.batchCreate(roomId, requests));
    }

    /**
     * A-SEAT04: 更新座位
     */
    @PutMapping("/{id}")
    public Result<SeatResponse> update(@PathVariable Long roomId, @PathVariable Long id, @RequestBody SeatUpdateRequest request) {
        return Result.ok(seatService.update(id, request));
    }

    /**
     * A-SEAT05: 注销座位（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long roomId, @PathVariable Long id) {
        seatService.remove(id);
        return Result.ok();
    }
}

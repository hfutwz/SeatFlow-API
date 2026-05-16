package com.seatflow.controller;

import com.seatflow.common.result.Result;
import com.seatflow.dto.response.SeatResponse;
import com.seatflow.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    /**
     * A-SEAT01: 按自习室查询座位列表
     */
    @GetMapping
    public Result<List<SeatResponse>> listSeats(@PathVariable Long roomId) {
        return Result.ok(seatService.listByRoom(roomId));
    }

    /**
     * A-SEAT02: 座位详情
     */
    @GetMapping("/{id}")
    public Result<SeatResponse> getSeat(@PathVariable Long roomId, @PathVariable Long id) {
        return Result.ok(seatService.getById(id));
    }
}

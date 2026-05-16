package com.seatflow.controller;

import com.seatflow.common.result.Result;
import com.seatflow.dto.request.ReservationCreateRequest;
import com.seatflow.dto.response.ReservationResponse;
import com.seatflow.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * A-RES01: 创建预约
     */
    @PostMapping
    public Result<ReservationResponse> create(@Valid @RequestBody ReservationCreateRequest request) {
        return Result.ok(reservationService.create(request));
    }

    /**
     * A-RES02: 取消预约
     */
    @PutMapping("/{id}/cancel")
    public Result<ReservationResponse> cancel(@PathVariable Long id) {
        return Result.ok(reservationService.cancel(id));
    }

    /**
     * A-RES03: 签到（M4实现完整链路，M3仅占位）
     */
    @PostMapping("/check-in")
    public Result<Void> checkIn(@RequestBody java.util.Map<String, String> body) {
        // M4 will implement full check-in logic
        return Result.fail("签到功能将在M4阶段实现");
    }

    /**
     * A-RES04: 我的当前预约
     */
    @GetMapping("/current")
    public Result<List<ReservationResponse>> listCurrent() {
        return Result.ok(reservationService.listCurrent());
    }

    /**
     * A-RES05: 我的历史预约（分页）
     */
    @GetMapping("/history")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<ReservationResponse>> listHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(reservationService.listHistory(page, size));
    }

    /**
     * A-RES06: 再次预约
     */
    @PostMapping("/{id}/rebook")
    public Result<ReservationResponse> rebook(@PathVariable Long id) {
        return Result.ok(reservationService.rebook(id));
    }
}

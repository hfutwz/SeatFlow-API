package com.seatflow.controller;

import com.seatflow.common.result.Result;
import com.seatflow.entity.Reservation;
import com.seatflow.service.CheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/check-in")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    /**
     * A-CI01: 学生签到
     */
    @PostMapping
    public Result<Map<String, Object>> checkIn(@RequestBody Map<String, Object> body) {
        Long reservationId = Long.valueOf(body.get("reservationId").toString());
        String code = body.get("code") != null ? body.get("code").toString() : null;

        Reservation reservation = checkInService.checkIn(reservationId, code);
        return Result.ok(Map.of(
                "reservationId", reservation.getId(),
                "status", reservation.getStatus()
        ));
    }

    /**
     * A-CI02: 获取今日签到编码
     */
    @GetMapping("/code/{roomId}")
    public Result<Map<String, String>> getTodayCode(@PathVariable Long roomId) {
        String code = checkInService.getTodayCode(roomId);
        return Result.ok(Map.of("code", code));
    }
}

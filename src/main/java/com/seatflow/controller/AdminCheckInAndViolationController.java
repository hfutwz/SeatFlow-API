package com.seatflow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatflow.common.result.Result;
import com.seatflow.entity.Violation;
import com.seatflow.service.CheckInService;
import com.seatflow.service.ViolationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCheckInAndViolationController {

    private final CheckInService checkInService;
    private final ViolationService violationService;

    /**
     * A-CI03: 管理端获取/刷新签到编码
     */
    @GetMapping("/check-in-codes/{roomId}")
    public Result<Map<String, String>> getCheckInCode(@PathVariable Long roomId) {
        String code = checkInService.getTodayCode(roomId);
        return Result.ok(Map.of("code", code));
    }

    /**
     * A-VIO02: 管理端查看所有违约记录
     */
    @GetMapping("/violations")
    public Result<Page<Violation>> listViolations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String type) {
        return Result.ok(violationService.listForAdmin(page, size, userId, type));
    }
}

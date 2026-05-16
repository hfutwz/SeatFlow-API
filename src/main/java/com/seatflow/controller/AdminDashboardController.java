package com.seatflow.controller;

import com.seatflow.common.result.Result;
import com.seatflow.dto.response.DashboardResponse;
import com.seatflow.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public Result<DashboardResponse> getStats() {
        return Result.ok(dashboardService.getStats());
    }
}

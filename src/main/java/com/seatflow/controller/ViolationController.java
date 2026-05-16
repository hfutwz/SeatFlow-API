package com.seatflow.controller;

import com.seatflow.common.result.Result;
import com.seatflow.entity.Violation;
import com.seatflow.service.ViolationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/violations")
@RequiredArgsConstructor
public class ViolationController {

    private final ViolationService violationService;

    /**
     * A-VIO01: 学生查看自己的违约记录
     */
    @GetMapping("/my")
    public Result<List<Violation>> listMyViolations() {
        return Result.ok(violationService.listMyViolations());
    }
}

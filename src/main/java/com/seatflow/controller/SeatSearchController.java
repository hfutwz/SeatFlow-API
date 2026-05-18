package com.seatflow.controller;

import com.seatflow.common.result.Result;
import com.seatflow.dto.response.SeatResponse;
import com.seatflow.service.SeatSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * A-SCH01: 搜索可用座位
 * 支持多条件组合搜索：自习室、日期、时间段、插座类型、位置标记
 */
@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatSearchController {

    private final SeatSearchService seatSearchService;

    @GetMapping("/search")
    public Result<List<SeatResponse>> search(
            @RequestParam(required = false) Long roomId,
            @RequestParam String date,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) String socketType,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) Long departmentId) {
        return Result.ok(seatSearchService.search(roomId, date, startTime, endTime, socketType, position, departmentId));
    }
}

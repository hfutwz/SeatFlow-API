package com.seatflow.controller;

import com.seatflow.common.result.Result;
import com.seatflow.entity.SystemConfig;
import com.seatflow.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    public Result<List<SystemConfig>> listAll() {
        return Result.ok(systemConfigService.listAll());
    }

    @PutMapping
    public Result<Void> batchUpdate(@RequestBody Map<String, String> configs) {
        systemConfigService.batchUpdate(configs);
        return Result.ok();
    }

    @PutMapping("/{key}")
    public Result<SystemConfig> update(@PathVariable String key, @RequestBody Map<String, String> body) {
        return Result.ok(systemConfigService.update(key, body.get("value")));
    }
}

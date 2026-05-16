package com.seatflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatflow.common.exception.BusinessException;
import com.seatflow.entity.SystemConfig;
import com.seatflow.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;

    public List<SystemConfig> listAll() {
        return systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfig>().orderByAsc(SystemConfig::getId));
    }

    @Transactional
    public SystemConfig update(String key, String value) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig config = systemConfigMapper.selectOne(wrapper);
        if (config == null) throw new BusinessException(404, "配置项不存在: " + key);
        config.setConfigValue(value);
        systemConfigMapper.updateById(config);
        return config;
    }

    @Transactional
    public void batchUpdate(Map<String, String> configs) {
        configs.forEach(this::update);
    }
}

package com.seatflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatflow.common.exception.BusinessException;
import com.seatflow.entity.Reservation;
import com.seatflow.entity.User;
import com.seatflow.entity.Violation;
import com.seatflow.mapper.ReservationMapper;
import com.seatflow.mapper.UserMapper;
import com.seatflow.mapper.ViolationMapper;
import com.seatflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViolationService {

    private final ViolationMapper violationMapper;
    private final UserMapper userMapper;
    private final ReservationMapper reservationMapper;

    /**
     * 记录超时违约
     */
    @Transactional
    public void recordTimeout(Long userId, Long reservationId) {
        Violation violation = new Violation();
        violation.setUserId(userId);
        violation.setReservationId(reservationId);
        violation.setType("CHECK_IN_TIMEOUT");
        violationMapper.insert(violation);
        log.info("记录违约: userId={}, reservationId={}, type=CHECK_IN_TIMEOUT", userId, reservationId);
    }

    /**
     * 学生查看自己的违约记录
     */
    public List<Violation> listMyViolations() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "未登录");
        LambdaQueryWrapper<Violation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Violation::getUserId, userId).orderByDesc(Violation::getCreatedAt);
        return violationMapper.selectList(wrapper);
    }

    /**
     * 管理端：查看所有违约记录（分页）
     */
    public Page<Violation> listForAdmin(int page, int size, Long userId, String type) {
        Page<Violation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Violation> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(Violation::getUserId, userId);
        if (type != null) wrapper.eq(Violation::getType, type);
        wrapper.orderByDesc(Violation::getCreatedAt);
        return violationMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 检查用户是否有严重违约记录（用于预约限制）
     */
    public boolean hasRecentViolation(Long userId, int days) {
        LambdaQueryWrapper<Violation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Violation::getUserId, userId)
                .ge(Violation::getCreatedAt, java.time.LocalDateTime.now().minusDays(days));
        return violationMapper.selectCount(wrapper) > 0;
    }
}

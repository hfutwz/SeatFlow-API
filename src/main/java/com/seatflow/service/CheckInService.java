package com.seatflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatflow.common.exception.BusinessException;
import com.seatflow.entity.CheckInCode;
import com.seatflow.entity.Reservation;
import com.seatflow.entity.Room;
import com.seatflow.mapper.CheckInCodeMapper;
import com.seatflow.mapper.ReservationMapper;
import com.seatflow.mapper.RoomMapper;
import com.seatflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckInService {

    private final CheckInCodeMapper checkInCodeMapper;
    private final ReservationMapper reservationMapper;
    private final RoomMapper roomMapper;
    private final ViolationService violationService;

    /**
     * 获取/生成今日签到编码
     */
    @Transactional
    public String getTodayCode(Long roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) throw new BusinessException(404, "自习室不存在");

        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<CheckInCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckInCode::getRoomId, roomId).eq(CheckInCode::getCodeDate, today);
        CheckInCode existing = checkInCodeMapper.selectOne(wrapper);

        if (existing != null) {
            return existing.getCode();
        }

        // 生成6位随机数字编码
        String code = String.format("%06d", new Random().nextInt(1000000));
        CheckInCode newCode = new CheckInCode();
        newCode.setRoomId(roomId);
        newCode.setCodeDate(today);
        newCode.setCode(code);
        checkInCodeMapper.insert(newCode);

        log.info("生成签到编码: roomId={}, date={}, code={}", roomId, today, code);
        return code;
    }

    /**
     * 学生签到
     */
    @Transactional
    public Reservation checkIn(Long reservationId, String code) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "未登录");

        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) throw new BusinessException(404, "预约不存在");
        if (!reservation.getUserId().equals(userId)) throw new BusinessException(403, "无权操作此预约");
        if (!"PENDING".equals(reservation.getStatus())) throw new BusinessException(400, "预约状态不允许签到");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reservationStart = LocalDateTime.of(reservation.getDate(), reservation.getStartTime());
        LocalDateTime reservationEnd = LocalDateTime.of(reservation.getDate(), reservation.getEndTime());

        // 检查是否在签到时间窗口内 (开始前15min ~ 结束时间)
        if (now.isBefore(reservationStart.minusMinutes(15))) {
            throw new BusinessException(400, "签到时间未到，请在预约开始前15分钟内签到");
        }
        if (now.isAfter(reservationEnd)) {
            throw new BusinessException(400, "预约时间已过，无法签到");
        }

        // 验证签到编码
        if (code != null && !code.isEmpty()) {
            LambdaQueryWrapper<CheckInCode> codeWrapper = new LambdaQueryWrapper<>();
            codeWrapper.eq(CheckInCode::getRoomId, reservation.getRoomId())
                    .eq(CheckInCode::getCodeDate, reservation.getDate());
            CheckInCode checkInCode = checkInCodeMapper.selectOne(codeWrapper);

            if (checkInCode == null) {
                throw new BusinessException(400, "今日该自习室尚未生成签到编码");
            }
            if (!checkInCode.getCode().equals(code)) {
                throw new BusinessException(400, "签到编码错误");
            }
        }

        // 更新预约状态
        reservation.setStatus("CHECKED_IN");
        reservationMapper.updateById(reservation);

        log.info("用户{}签到成功, reservationId={}", userId, reservationId);
        return reservation;
    }

    /**
     * 超时未签到自动取消 + 记录违约
     * 由定时任务调用
     */
    @Transactional
    public int autoCancelTimeout() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        java.time.LocalTime currentTime = now.toLocalTime();

        // 查找所有PENDING且开始时间+15min < 当前时间的预约
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getStatus, "PENDING")
                .eq(Reservation::getDate, today)
                .lt(Reservation::getStartTime, currentTime.minusMinutes(15));

        java.util.List<Reservation> timeoutList = reservationMapper.selectList(wrapper);
        int count = 0;
        for (Reservation r : timeoutList) {
            r.setStatus("CANCELLED");
            r.setCancelledBy("SYSTEM");
            reservationMapper.updateById(r);

            // 记录违约
            violationService.recordTimeout(r.getUserId(), r.getId());
            count++;
        }

        if (count > 0) {
            log.info("自动取消超时预约: {}条", count);
        }
        return count;
    }

    /**
     * 签到前15分钟提醒
     * 由定时任务调用
     */
    public int remindBeforeCheckIn() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        java.time.LocalTime currentTime = now.toLocalTime();
        java.time.LocalTime remindAfter = currentTime.plusMinutes(14);
        java.time.LocalTime remindBefore = currentTime.plusMinutes(16);

        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getStatus, "PENDING")
                .eq(Reservation::getDate, today)
                .ge(Reservation::getStartTime, remindAfter)
                .le(Reservation::getStartTime, remindBefore)
                .eq(Reservation::getRemindedBefore, 0);

        java.util.List<Reservation> toRemind = reservationMapper.selectList(wrapper);
        for (Reservation r : toRemind) {
            r.setRemindedBefore(1);
            reservationMapper.updateById(r);
            // TODO: 实际推送通知（邮件/站内信）在M6实现
            log.info("签到提醒: userId={}, reservationId={}, startTime={}", r.getUserId(), r.getId(), r.getStartTime());
        }
        return toRemind.size();
    }

    /**
     * 签到后超时警告
     * 由定时任务调用
     */
    public int warnLateCheckIn() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        java.time.LocalTime currentTime = now.toLocalTime();

        // 已过开始时间10min但未签到的预约 → 发出警告
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getStatus, "PENDING")
                .eq(Reservation::getDate, today)
                .lt(Reservation::getStartTime, currentTime.minusMinutes(10))
                .gt(Reservation::getStartTime, currentTime.minusMinutes(15))
                .eq(Reservation::getWarnedLate, 0);

        java.util.List<Reservation> toWarn = reservationMapper.selectList(wrapper);
        for (Reservation r : toWarn) {
            r.setWarnedLate(1);
            reservationMapper.updateById(r);
            log.warn("签到逾期警告: userId={}, reservationId={}", r.getUserId(), r.getId());
        }
        return toWarn.size();
    }
}

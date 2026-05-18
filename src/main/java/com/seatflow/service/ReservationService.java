package com.seatflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatflow.common.exception.BusinessException;
import com.seatflow.dto.request.ReservationCreateRequest;
import com.seatflow.dto.response.ReservationResponse;
import com.seatflow.entity.Reservation;
import com.seatflow.entity.Room;
import com.seatflow.entity.Seat;
import com.seatflow.entity.User;
import com.seatflow.mapper.ReservationMapper;
import com.seatflow.mapper.RoomMapper;
import com.seatflow.mapper.SeatMapper;
import com.seatflow.mapper.UserMapper;
import com.seatflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationMapper reservationMapper;
    private final SeatMapper seatMapper;
    private final RoomMapper roomMapper;
    private final UserMapper userMapper;

    /**
     * M3-B02: 创建预约 — 冲突检测 + 开放时间校验 + 院系权限 + 最大时长
     */
    @Transactional
    public ReservationResponse create(ReservationCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "未登录");

        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        Seat seat = seatMapper.selectById(request.getSeatId());
        if (seat == null) throw new BusinessException(404, "座位不存在");
        if (!"AVAILABLE".equals(seat.getStatus())) throw new BusinessException(400, "该座位不可预约");

        Room room = roomMapper.selectById(seat.getRoomId());
        if (room == null) throw new BusinessException(404, "自习室不存在");
        if (!"OPEN".equals(room.getStatus())) throw new BusinessException(400, "自习室未开放");

        // 院系权限校验
        if (room.getDepartmentId() != null) {
            if (user.getDepartmentId() == null || !user.getDepartmentId().equals(room.getDepartmentId())) {
                throw new BusinessException(403, "您没有该自习室的预约权限");
            }
        }

        LocalDate date = LocalDate.parse(request.getDate());
        LocalTime startTime = LocalTime.parse(request.getStartTime());
        LocalTime endTime = LocalTime.parse(request.getEndTime());

        // 整点校验
        if (startTime.getMinute() != 0 || endTime.getMinute() != 0) {
            throw new BusinessException(400, "预约时间必须为整点");
        }

        // 时段校验
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(400, "开始时间必须早于结束时间");
        }

        // 开放时间校验
        if (startTime.isBefore(room.getOpenTime()) || endTime.isAfter(room.getCloseTime())) {
            throw new BusinessException(400, "预约时间不在自习室开放时间内(" + room.getOpenTime() + "-" + room.getCloseTime() + ")");
        }

        // 最大时长校验 (默认4小时，后续从SystemConfig读取)
        long hours = java.time.Duration.between(startTime, endTime).toHours();
        if (hours <= 0 || hours > 4) {
            throw new BusinessException(400, "单次预约时长为1-4小时");
        }

        // 座位时段冲突检测 (区间重叠)
        LambdaQueryWrapper<Reservation> seatConflict = new LambdaQueryWrapper<>();
        seatConflict.eq(Reservation::getSeatId, seat.getId())
                .eq(Reservation::getDate, date)
                .in(Reservation::getStatus, List.of("PENDING", "CHECKED_IN"))
                .lt(Reservation::getStartTime, endTime)
                .gt(Reservation::getEndTime, startTime);
        if (reservationMapper.selectCount(seatConflict) > 0) {
            throw new BusinessException(400, "该座位在所选时段已被预约");
        }

        // 学生同时段冲突检测
        LambdaQueryWrapper<Reservation> userConflict = new LambdaQueryWrapper<>();
        userConflict.eq(Reservation::getUserId, userId)
                .eq(Reservation::getDate, date)
                .in(Reservation::getStatus, List.of("PENDING", "CHECKED_IN"))
                .lt(Reservation::getStartTime, endTime)
                .gt(Reservation::getEndTime, startTime);
        if (reservationMapper.selectCount(userConflict) > 0) {
            throw new BusinessException(400, "您在该时段已有预约");
        }

        // 创建预约
        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setSeatId(seat.getId());
        reservation.setRoomId(room.getId());
        reservation.setDate(date);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setStatus("PENDING");
        reservation.setRemindedBefore(0);
        reservation.setWarnedLate(0);
        reservationMapper.insert(reservation);

        return toReservationResponse(reservation, seat, room);
    }

    /**
     * M3-B03: 取消预约
     */
    @Transactional
    public ReservationResponse cancel(Long reservationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "未登录");

        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) throw new BusinessException(404, "预约不存在");
        if (!reservation.getUserId().equals(userId)) throw new BusinessException(403, "无权操作此预约");

        if ("CANCELLED".equals(reservation.getStatus())) {
            throw new BusinessException(400, "预约已取消");
        }
        if ("COMPLETED".equals(reservation.getStatus())) {
            throw new BusinessException(400, "预约已完成，无法取消");
        }

        // 超时15min未签到 → 系统已自动取消，不可手动取消
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reservationStart = LocalDateTime.of(reservation.getDate(), reservation.getStartTime());
        if ("PENDING".equals(reservation.getStatus())
                && now.isAfter(reservationStart.plusMinutes(15))) {
            throw new BusinessException(400, "预约已超时，系统将自动取消并记录违约");
        }

        reservation.setStatus("CANCELLED");
        reservation.setCancelledBy("STUDENT");
        reservationMapper.updateById(reservation);

        Seat seat = seatMapper.selectById(reservation.getSeatId());
        Room room = roomMapper.selectById(reservation.getRoomId());
        return toReservationResponse(reservation, seat, room);
    }

    /**
     * 管理员取消预约
     */
    @Transactional
    public ReservationResponse adminCancel(Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) throw new BusinessException(404, "预约不存在");

        reservation.setStatus("CANCELLED");
        reservation.setCancelledBy("ADMIN");
        reservationMapper.updateById(reservation);

        Seat seat = seatMapper.selectById(reservation.getSeatId());
        Room room = roomMapper.selectById(reservation.getRoomId());
        return toReservationResponse(reservation, seat, room);
    }

    /**
     * M3-B04: 我的当前预约
     */
    public List<ReservationResponse> listCurrent() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "未登录");

        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getUserId, userId)
                .in(Reservation::getStatus, List.of("PENDING", "CHECKED_IN"))
                .orderByDesc(Reservation::getDate)
                .orderByDesc(Reservation::getStartTime);

        return reservationMapper.selectList(wrapper).stream()
                .map(r -> toReservationResponse(r,
                        seatMapper.selectById(r.getSeatId()),
                        roomMapper.selectById(r.getRoomId())))
                .collect(Collectors.toList());
    }

    /**
     * M3-B04: 我的历史预约（分页）
     */
    public Page<ReservationResponse> listHistory(int page, int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "未登录");

        Page<Reservation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getUserId, userId)
                .and(w -> w.in(Reservation::getStatus, List.of("COMPLETED", "CANCELLED"))
                        .or(sub -> sub.eq(Reservation::getStatus, "CHECKED_IN")
                                .lt(Reservation::getDate, java.time.LocalDate.now())))
                .orderByDesc(Reservation::getDate)
                .orderByDesc(Reservation::getStartTime);

        Page<Reservation> result = reservationMapper.selectPage(pageParam, wrapper);
        Page<ReservationResponse> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(result.getRecords().stream()
                .map(r -> toReservationResponse(r,
                        seatMapper.selectById(r.getSeatId()),
                        roomMapper.selectById(r.getRoomId())))
                .collect(Collectors.toList()));
        return responsePage;
    }

    /**
     * M3-B04: 再次预约
     */
    @Transactional
    public ReservationResponse rebook(Long reservationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "未登录");

        Reservation old = reservationMapper.selectById(reservationId);
        if (old == null) throw new BusinessException(404, "原预约不存在");
        if (!old.getUserId().equals(userId)) throw new BusinessException(403, "无权操作");

        // 以当前日期+同时段发起预约
        ReservationCreateRequest request = new ReservationCreateRequest();
        request.setSeatId(old.getSeatId());
        request.setDate(LocalDate.now().toString());
        request.setStartTime(old.getStartTime().toString());
        request.setEndTime(old.getEndTime().toString());

        return create(request);
    }

    /**
     * 管理端：全部预约（分页+筛选）
     */
    public Page<ReservationResponse> listForAdmin(int page, int size, String status, Long userId, String date) {
        Page<Reservation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Reservation::getStatus, status);
        if (userId != null) wrapper.eq(Reservation::getUserId, userId);
        if (date != null) wrapper.eq(Reservation::getDate, LocalDate.parse(date));
        wrapper.orderByDesc(Reservation::getId);

        Page<Reservation> result = reservationMapper.selectPage(pageParam, wrapper);
        Page<ReservationResponse> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(result.getRecords().stream()
                .map(r -> toReservationResponse(r,
                        seatMapper.selectById(r.getSeatId()),
                        roomMapper.selectById(r.getRoomId())))
                .collect(Collectors.toList()));
        return responsePage;
    }

    /**
     * 管理端：代客预约
     */
    @Transactional
    public ReservationResponse adminCreate(Long forUserId, ReservationCreateRequest request) {
        // 暂时用原方法，后续在M5完善权限校验
        // TODO: set forUserId instead of current user
        return create(request);
    }

    private ReservationResponse toReservationResponse(Reservation r, Seat seat, Room room) {
        return ReservationResponse.builder()
                .id(r.getId())
                .seatId(r.getSeatId())
                .roomName(room != null ? room.getName() : null)
                .seatNumber(seat != null ? seat.getSeatNumber() : null)
                .date(r.getDate() != null ? r.getDate().toString() : null)
                .startTime(r.getStartTime() != null ? r.getStartTime().toString() : null)
                .endTime(r.getEndTime() != null ? r.getEndTime().toString() : null)
                .status(r.getStatus())
                .cancelledBy(r.getCancelledBy())
                .build();
    }
}

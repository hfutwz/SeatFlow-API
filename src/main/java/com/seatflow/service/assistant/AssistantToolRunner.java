package com.seatflow.service.assistant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatflow.dto.request.ReservationCreateRequest;
import com.seatflow.entity.*;
import com.seatflow.mapper.*;
import com.seatflow.security.SecurityUtils;
import com.seatflow.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 实际执行系统操作的工具类
 * 被 FunctionExecutor 调用，操作数据库
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssistantToolRunner {

    private final RoomMapper roomMapper;
    private final SeatMapper seatMapper;
    private final ReservationMapper reservationMapper;
    private final UserMapper userMapper;
    private final ViolationMapper violationMapper;
    private final ReservationService reservationService;

    public String queryRooms() {
        List<Room> rooms = roomMapper.selectList(
                new LambdaQueryWrapper<Room>().eq(Room::getStatus, "OPEN").orderByAsc(Room::getId));
        if (rooms.isEmpty()) return "目前没有开放的自习室。";

        StringBuilder sb = new StringBuilder();
        for (Room room : rooms) {
            long total = seatMapper.selectCount(new LambdaQueryWrapper<Seat>().eq(Seat::getRoomId, room.getId()));
            long available = seatMapper.selectCount(
                    new LambdaQueryWrapper<Seat>().eq(Seat::getRoomId, room.getId()).eq(Seat::getStatus, "AVAILABLE"));
            sb.append(String.format("- ID:%d %s（%s）开放时间:%s-%s 空座:%d/%d\n",
                    room.getId(), room.getName(), room.getLocation() != null ? room.getLocation() : "",
                    room.getOpenTime().toString().substring(0, 5),
                    room.getCloseTime().toString().substring(0, 5),
                    available, total));
        }
        return sb.toString();
    }

    public String searchAvailableSeats(Map<String, Object> args) {
        String dateStr = (String) args.get("date");
        String startTime = (String) args.get("start_time");
        String endTime = (String) args.get("end_time");
        Long roomId = args.get("room_id") != null ? ((Number) args.get("room_id")).longValue() : null;
        String socketType = (String) args.get("socket_type");
        String position = (String) args.get("position");

        LocalDate date = LocalDate.parse(dateStr);
        LocalTime st = LocalTime.parse(startTime);
        LocalTime et = LocalTime.parse(endTime);

        // Filter rooms
        LambdaQueryWrapper<Room> roomWrapper = new LambdaQueryWrapper<>();
        roomWrapper.eq(Room::getStatus, "OPEN");
        if (roomId != null) roomWrapper.eq(Room::getId, roomId);
        List<Room> rooms = roomMapper.selectList(roomWrapper);

        StringBuilder sb = new StringBuilder();
        for (Room room : rooms) {
            LambdaQueryWrapper<Seat> seatWrapper = new LambdaQueryWrapper<>();
            seatWrapper.eq(Seat::getRoomId, room.getId()).eq(Seat::getStatus, "AVAILABLE");
            if (socketType != null && !socketType.isEmpty()) seatWrapper.eq(Seat::getSocketType, socketType);
            if (position != null && !position.isEmpty()) seatWrapper.eq(Seat::getPosition, position);
            List<Seat> seats = seatMapper.selectList(seatWrapper);

            // Filter out reserved seats
            LambdaQueryWrapper<Reservation> resWrapper = new LambdaQueryWrapper<>();
            resWrapper.eq(Reservation::getDate, date)
                    .in(Reservation::getStatus, List.of("PENDING", "CHECKED_IN"))
                    .lt(Reservation::getStartTime, et)
                    .gt(Reservation::getEndTime, st);
            List<Long> reservedSeatIds = reservationMapper.selectList(resWrapper).stream()
                    .map(Reservation::getSeatId).toList();

            List<Seat> available = seats.stream().filter(s -> !reservedSeatIds.contains(s.getId())).toList();
            if (!available.isEmpty()) {
                sb.append(String.format("\n%s：\n", room.getName()));
                for (Seat s : available) {
                    String posLabel = switch (s.getPosition()) {
                        case "WINDOW" -> "靠窗";
                        case "CORRIDOR" -> "靠走廊";
                        default -> "中间";
                    };
                    String socketLabel = switch (s.getSocketType()) {
                        case "FIXED" -> "有固定插座";
                        case "MOVABLE" -> "有移动导轨插座";
                        default -> "无插座";
                    };
                    sb.append(String.format("  座位ID:%d %s [%s,%s]\n", s.getId(), s.getSeatNumber(), posLabel, socketLabel));
                }
            }
        }

        if (sb.isEmpty()) return date + " " + startTime + "-" + endTime + " 没有找到可用座位。";
        return "可用座位：" + sb;
    }

    public String queryMyReservations(Map<String, Object> args) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return "未登录，无法查询预约。";

        String status = (String) args.getOrDefault("status", "ALL");
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getUserId, userId).orderByDesc(Reservation::getDate);
        if (!"ALL".equals(status)) {
            wrapper.eq(Reservation::getStatus, status);
        } else {
            wrapper.in(Reservation::getStatus, List.of("PENDING", "CHECKED_IN"));
        }

        List<Reservation> reservations = reservationMapper.selectList(wrapper);
        if (reservations.isEmpty()) return "当前没有进行中的预约。";

        StringBuilder sb = new StringBuilder();
        for (Reservation r : reservations) {
            Seat seat = seatMapper.selectById(r.getSeatId());
            Room room = seat != null ? roomMapper.selectById(seat.getRoomId()) : null;
            String statusLabel = "PENDING".equals(r.getStatus()) ? "待签到" : "CHECKED_IN".equals(r.getStatus()) ? "已签到" : r.getStatus();
            sb.append(String.format("- 预约ID:%d %s 座位%s %s %s-%s [%s]\n",
                    r.getId(),
                    room != null ? room.getName() : "未知",
                    seat != null ? seat.getSeatNumber() : "未知",
                    r.getDate(),
                    r.getStartTime().toString().substring(0, 5),
                    r.getEndTime().toString().substring(0, 5),
                    statusLabel));
        }
        return sb.toString();
    }

    public String makeReservation(Map<String, Object> args) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return "未登录，无法预约。";

        Long seatId = ((Number) args.get("seat_id")).longValue();
        String date = (String) args.get("date");
        String startTime = (String) args.get("start_time");
        String endTime = (String) args.get("end_time");

        try {
            ReservationCreateRequest req = new ReservationCreateRequest();
            req.setSeatId(seatId);
            req.setDate(date);
            req.setStartTime(startTime);
            req.setEndTime(endTime);
            var result = reservationService.create(req);

            Seat seat = seatMapper.selectById(seatId);
            Room room = seat != null ? roomMapper.selectById(seat.getRoomId()) : null;
            return String.format("预约成功！预约ID:%d %s 座位%s %s %s-%s",
                    result.getId(),
                    room != null ? room.getName() : "",
                    seat != null ? seat.getSeatNumber() : "",
                    date, startTime, endTime);
        } catch (Exception e) {
            return "预约失败：" + e.getMessage();
        }
    }

    public String cancelReservation(Map<String, Object> args) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return "未登录，无法取消。";

        Long reservationId = args.get("reservation_id") != null ? ((Number) args.get("reservation_id")).longValue() : null;

        if (reservationId == null) {
            // 取消最近一个待签到预约
            List<Reservation> pending = reservationMapper.selectList(
                    new LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getUserId, userId)
                            .eq(Reservation::getStatus, "PENDING")
                            .orderByDesc(Reservation::getDate)
                            .last("LIMIT 1"));
            if (pending.isEmpty()) return "没有待签到的预约可以取消。";
            reservationId = pending.get(0).getId();
        }

        try {
            reservationService.cancel(reservationId);
            return "预约已取消（ID:" + reservationId + "）";
        } catch (Exception e) {
            return "取消失败：" + e.getMessage();
        }
    }

    public String queryMyViolations() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return "未登录，无法查询。";

        List<Violation> violations = violationMapper.selectList(
                new LambdaQueryWrapper<Violation>()
                        .eq(Violation::getUserId, userId)
                        .orderByDesc(Violation::getCreatedAt));

        if (violations.isEmpty()) return "你没有违约记录。";

        StringBuilder sb = new StringBuilder();
        for (Violation v : violations) {
            sb.append(String.format("- 违约ID:%d 预约ID:%d 类型:%s 时间:%s\n",
                    v.getId(), v.getReservationId(), v.getType(), v.getCreatedAt()));
        }
        return sb.toString();
    }
}

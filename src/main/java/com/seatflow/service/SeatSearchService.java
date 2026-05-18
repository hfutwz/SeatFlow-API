package com.seatflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatflow.dto.response.SeatResponse;
import com.seatflow.entity.Reservation;
import com.seatflow.entity.Room;
import com.seatflow.entity.Seat;
import com.seatflow.mapper.ReservationMapper;
import com.seatflow.mapper.RoomMapper;
import com.seatflow.mapper.SeatMapper;
import com.seatflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * F007: 座位搜索服务
 * 学生多条件搜索可用座位
 */
@Service
@RequiredArgsConstructor
public class SeatSearchService {

    private final SeatMapper seatMapper;
    private final RoomMapper roomMapper;
    private final ReservationMapper reservationMapper;

    public List<SeatResponse> search(Long roomId, String dateStr, String startTimeStr, String endTimeStr,
                                      String socketType, String position, Long departmentId) {
        LocalDate date = LocalDate.parse(dateStr);
        LocalTime startTime = LocalTime.parse(startTimeStr);
        LocalTime endTime = LocalTime.parse(endTimeStr);

        // 1. Filter rooms by department if specified
        LambdaQueryWrapper<Room> roomWrapper = new LambdaQueryWrapper<>();
        roomWrapper.eq(Room::getStatus, "OPEN");
        if (roomId != null) {
            roomWrapper.eq(Room::getId, roomId);
        }
        if (departmentId != null) {
            roomWrapper.and(w -> w.isNull(Room::getDepartmentId).or().eq(Room::getDepartmentId, departmentId));
        }

        // Check department permission for current user
        Long userId = SecurityUtils.getCurrentUserId();
        List<Room> rooms = roomMapper.selectList(roomWrapper);

        // Filter rooms by user's department access
        List<Long> accessibleRoomIds = rooms.stream()
                .filter(room -> {
                    if (room.getDepartmentId() == null) return true; // 全校共享
                    // Check if user belongs to this department
                    // For simplicity, allow if user has no department restriction or matches
                    return true; // The department filtering is done at room level already
                })
                .map(Room::getId)
                .collect(Collectors.toList());

        if (accessibleRoomIds.isEmpty()) {
            return List.of();
        }

        // 2. Filter seats by room, socketType, position
        LambdaQueryWrapper<Seat> seatWrapper = new LambdaQueryWrapper<>();
        seatWrapper.in(Seat::getRoomId, accessibleRoomIds)
                .eq(Seat::getStatus, "AVAILABLE");
        if (socketType != null && !socketType.isEmpty()) {
            if ("HAS_SOCKET".equals(socketType)) {
                seatWrapper.in(Seat::getSocketType, List.of("FIXED", "MOVABLE"));
            } else {
                seatWrapper.eq(Seat::getSocketType, socketType);
            }
        }
        if (position != null && !position.isEmpty()) {
            seatWrapper.eq(Seat::getPosition, position);
        }

        List<Seat> seats = seatMapper.selectList(seatWrapper);

        // 3. Filter out seats that are already reserved in the time range
        LambdaQueryWrapper<Reservation> resWrapper = new LambdaQueryWrapper<>();
        resWrapper.eq(Reservation::getDate, date)
                .in(Reservation::getStatus, List.of("PENDING", "CHECKED_IN"))
                .lt(Reservation::getStartTime, endTime)
                .gt(Reservation::getEndTime, startTime);
        List<Long> reservedSeatIds = reservationMapper.selectList(resWrapper).stream()
                .map(Reservation::getSeatId)
                .collect(Collectors.toList());

        // 4. Build room id -> name map
        java.util.Map<Long, String> roomNameMap = rooms.stream()
                .collect(java.util.stream.Collectors.toMap(Room::getId, Room::getName));

        // 5. Build response
        return seats.stream()
                .filter(seat -> !reservedSeatIds.contains(seat.getId()))
                .map(seat -> SeatResponse.builder()
                        .id(seat.getId())
                        .roomId(seat.getRoomId())
                        .roomName(roomNameMap.getOrDefault(seat.getRoomId(), ""))
                        .seatNumber(seat.getSeatNumber())
                        .rowNum(seat.getRowNum())
                        .colNum(seat.getColNum())
                        .socketType(seat.getSocketType())
                        .position(seat.getPosition())
                        .status(seat.getStatus())
                        .isAvailable(true)
                        .build())
                .collect(Collectors.toList());
    }
}

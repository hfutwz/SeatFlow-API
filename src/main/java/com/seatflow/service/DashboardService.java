package com.seatflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatflow.dto.response.DashboardResponse;
import com.seatflow.entity.*;
import com.seatflow.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RoomMapper roomMapper;
    private final SeatMapper seatMapper;
    private final ReservationMapper reservationMapper;
    private final ViolationMapper violationMapper;
    private final UserMapper userMapper;

    public DashboardResponse getStats() {
        long totalRooms = roomMapper.selectCount(new LambdaQueryWrapper<>());
        long totalSeats = seatMapper.selectCount(new LambdaQueryWrapper<>());
        long availableSeats = seatMapper.selectCount(
                new LambdaQueryWrapper<Seat>().eq(Seat::getStatus, "AVAILABLE"));
        LocalDate today = LocalDate.now();
        long todayReservations = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>().eq(Reservation::getDate, today));
        long pendingReservations = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>().eq(Reservation::getStatus, "PENDING"));
        long todayCheckIns = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>().eq(Reservation::getDate, today).eq(Reservation::getStatus, "CHECKED_IN"));
        long todayViolations = violationMapper.selectCount(
                new LambdaQueryWrapper<Violation>().ge(Violation::getCreatedAt, today.atStartOfDay()));
        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<>());

        return DashboardResponse.builder()
                .totalRooms(totalRooms)
                .totalSeats(totalSeats)
                .availableSeats(availableSeats)
                .todayReservations(todayReservations)
                .pendingReservations(pendingReservations)
                .todayCheckIns(todayCheckIns)
                .todayViolations(todayViolations)
                .totalUsers(totalUsers)
                .build();
    }
}

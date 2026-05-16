package com.seatflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponse {
    private long totalRooms;
    private long totalSeats;
    private long availableSeats;
    private long todayReservations;
    private long pendingReservations;
    private long todayCheckIns;
    private long todayViolations;
    private long totalUsers;
}

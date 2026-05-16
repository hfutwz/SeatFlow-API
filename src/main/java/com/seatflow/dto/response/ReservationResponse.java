package com.seatflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReservationResponse {
    private Long id;
    private Long seatId;
    private String roomName;
    private String seatNumber;
    private String date;
    private String startTime;
    private String endTime;
    private String status;
    private String cancelledBy;
}

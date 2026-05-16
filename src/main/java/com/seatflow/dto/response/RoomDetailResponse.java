package com.seatflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomDetailResponse {
    private Long id;
    private String name;
    private String location;
    private Long departmentId;
    private String departmentName;
    private LocalTime openTime;
    private LocalTime closeTime;
    private String status;
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer maxRow;
    private Integer maxCol;
    private List<SeatResponse> seats;
}

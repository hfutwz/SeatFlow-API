package com.seatflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomResponse {
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
}

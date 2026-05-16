package com.seatflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeatResponse {
    private Long id;
    private Long roomId;
    private String seatNumber;
    private Integer rowNum;
    private Integer colNum;
    private String socketType;
    private String position;
    private String status;
    private Boolean isAvailable;
}

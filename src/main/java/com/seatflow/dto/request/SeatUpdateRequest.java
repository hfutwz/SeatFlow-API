package com.seatflow.dto.request;

import lombok.Data;

@Data
public class SeatUpdateRequest {
    private String seatNumber;
    private Integer rowNum;
    private Integer colNum;
    private String socketType;
    private String position;
    private String status;
}

package com.seatflow.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservationCreateRequest {
    @NotNull(message = "座位ID不能为空")
    private Long seatId;

    @NotNull(message = "预约日期不能为空")
    private String date;        // yyyy-MM-dd

    @NotNull(message = "开始时间不能为空")
    private String startTime;   // HH:mm

    @NotNull(message = "结束时间不能为空")
    private String endTime;     // HH:mm
}

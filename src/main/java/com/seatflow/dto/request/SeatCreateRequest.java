package com.seatflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SeatCreateRequest {
    @NotBlank(message = "座位编号不能为空")
    @Size(max = 20, message = "编号最多20字符")
    private String seatNumber;

    @NotNull(message = "行号不能为空")
    private Integer rowNum;

    @NotNull(message = "列号不能为空")
    private Integer colNum;

    private String socketType = "NONE";

    private String position = "MIDDLE";
}

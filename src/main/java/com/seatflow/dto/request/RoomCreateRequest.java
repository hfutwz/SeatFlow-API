package com.seatflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalTime;

@Data
public class RoomCreateRequest {
    @NotBlank(message = "自习室名称不能为空")
    @Size(max = 100, message = "名称最多100字符")
    private String name;

    @Size(max = 200, message = "位置最多200字符")
    private String location;

    private Long departmentId;

    private LocalTime openTime;

    private LocalTime closeTime;
}

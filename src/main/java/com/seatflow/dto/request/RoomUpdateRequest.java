package com.seatflow.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalTime;

@Data
public class RoomUpdateRequest {
    @Size(max = 100, message = "名称最多100字符")
    private String name;

    @Size(max = 200, message = "位置最多200字符")
    private String location;

    private Long departmentId;

    private LocalTime openTime;

    private LocalTime closeTime;

    private String status;
}

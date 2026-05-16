package com.seatflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("t_room")
public class Room {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String location;
    private Long departmentId;
    private LocalTime openTime;
    private LocalTime closeTime;
    private String status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

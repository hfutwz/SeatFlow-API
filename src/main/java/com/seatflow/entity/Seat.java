package com.seatflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_seat")
public class Seat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private String seatNumber;
    private Integer rowNum;
    private Integer colNum;
    private String socketType;
    private String position;
    private String status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

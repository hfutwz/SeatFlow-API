package com.seatflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_check_in_code")
public class CheckInCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private LocalDate codeDate;
    private String code;
    private LocalDateTime createdAt;
}

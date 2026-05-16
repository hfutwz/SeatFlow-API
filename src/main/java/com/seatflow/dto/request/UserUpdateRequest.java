package com.seatflow.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class UserUpdateRequest {
    private String realName;
    private String email;
    private Long departmentId;
    private String userType;
    private List<Long> roleIds;
}

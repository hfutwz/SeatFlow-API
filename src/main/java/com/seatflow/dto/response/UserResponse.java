package com.seatflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private Long departmentId;
    private String departmentName;
    private String userType;
    private List<String> roles;
    private List<Long> roleIds;
    private List<String> permissions;
    private LocalDateTime createdAt;
}

package com.seatflow.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class RoleUpdateRequest {
    private String name;
    private String description;
    private List<Long> permissionIds;
}

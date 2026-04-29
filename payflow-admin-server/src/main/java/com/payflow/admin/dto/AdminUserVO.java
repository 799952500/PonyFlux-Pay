package com.payflow.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * @author Lucas
 */
public class AdminUserVO {

    private Long id;
    private String username;
    private String role;
    private String nickname;
    private String status;
}

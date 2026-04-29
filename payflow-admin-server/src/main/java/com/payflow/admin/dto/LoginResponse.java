package com.payflow.admin.dto;

import com.payflow.admin.entity.SysMenu;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * @author Lucas
 */
public class LoginResponse {

    private String token;
    private String username;
    private String role;
    private LocalDateTime expireTime;
    private List<SysMenu> menus;
}

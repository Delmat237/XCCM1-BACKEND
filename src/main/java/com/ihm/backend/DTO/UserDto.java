package com.ihm.backend.DTO;

import com.ihm.backend.enums.UserRole;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserDto {
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private UserRole role;
    private String photoUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
}

package com.example.booking.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    /** Роль приходит и после перезагрузки страницы: без неё фронт
     *  после F5 не понимает, показывать ли админские ссылки. */
    private String role;
}

package com.example.myfirst.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank(message = "username cannot be blank")
    @Size(max = 50, message = "username must not exceed 50 characters")
    private String username;

    @NotBlank(message = "email cannot be blank")
    @Email(message = "email must be valid")
    private String email;
}

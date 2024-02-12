package com.accionmfb.omnix.notification.payload;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class SimpleSmsRequestPayload
{
    @NotNull(message = "username cannot be null")
    @NotBlank(message = "username cannot be blank")
    @NotEmpty(message = "username cannot be empty")
    private String username;

    @NotNull(message = "password cannot be null")
    @NotBlank(message = "password cannot be blank")
    @NotEmpty(message = "password cannot be empty")
    private String password;

    @NotNull(message = "mobileNumber cannot be null")
    @NotBlank(message = "mobileNumber cannot be blank")
    @NotEmpty(message = "mobileNumber cannot be empty")
    private String mobileNumber;

    @NotNull(message = "message cannot be null")
    @NotBlank(message = "message cannot be blank")
    @NotEmpty(message = "message cannot be empty")
    private String message;
}

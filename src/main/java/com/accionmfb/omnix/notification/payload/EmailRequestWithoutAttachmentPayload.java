package com.accionmfb.omnix.notification.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.*;

@Data
public class EmailRequestWithoutAttachmentPayload
{
    @NotNull(message = "Mobile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;

    @NotNull(message = "Recipient name cannot be null")
    @NotEmpty(message = "Recipient name cannot be empty")
    @NotBlank(message = "Recipient name cannot be blank")
    private String recipientName;

    @NotNull(message = "Recipient email cannot be null")
    @NotEmpty(message = "Recipient email cannot be empty")
    @NotBlank(message = "Recipient email cannot be blank")
    @Email
    private String recipientEmail;

    @NotNull(message = "Email subject cannot be null")
    @NotEmpty(message = "Email subject cannot be empty")
    @NotBlank(message = "Email subject cannot be blank")
    private String subject;

    @NotNull(message = "Email body cannot be null")
    @NotEmpty(message = "Email body cannot be empty")
    @NotBlank(message = "Email body cannot be blank")
    private String emailBody;

}

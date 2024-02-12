package com.accionmfb.omnix.notification.payload;

import lombok.Data;

@Data
public class SmsResponse {

    private String ticketId;
    private String status;
    private String errorCode;
    private String errorMessage;
    private String destination;
}

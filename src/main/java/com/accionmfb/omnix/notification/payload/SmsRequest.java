package com.accionmfb.omnix.notification.payload;

import lombok.Data;

@Data
public class SmsRequest
{
    private Sms sms;
    private Account account;
}

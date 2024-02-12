package com.accionmfb.omnix.notification.payload;

import lombok.Data;

@Data
public class Sms
{
    private String dest;
    private String src;
    private String text;
    private String unicode;
}

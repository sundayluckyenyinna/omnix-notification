package com.accionmfb.omnix.notification.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "email")
public class EmailConfigurationProperties {

    private String hostName;
    private String port;
    private String username;
    private String password;
    private String from;
    private String subject;
}

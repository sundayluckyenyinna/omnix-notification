package com.accionmfb.omnix.notification.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    private String link;
    private String description;
    private String name;
    private Map<String, Object> attachmentContextData;
}

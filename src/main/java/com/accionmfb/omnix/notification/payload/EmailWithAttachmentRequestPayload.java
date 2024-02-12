package com.accionmfb.omnix.notification.payload;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class EmailWithAttachmentRequestPayload {
    private List<Attachment> attachmentLinks;
    private List<String> recipients;
    private String message;
    private String subject;
    private String from;
}

package com.accionmfb.omnix.notification.payload;

import lombok.Data;

import java.util.List;

@Data
public class EmailWithAttachmentResponsePayload {

    private String responseCode;
    private String responseMessage;
    private List<String> filePaths;
}

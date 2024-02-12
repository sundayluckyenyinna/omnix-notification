package com.accionmfb.omnix.notification.service;

import com.accionmfb.omnix.notification.payload.EmailWithAttachmentRequestPayload;
import com.accionmfb.omnix.notification.payload.EmailWithAttachmentResponsePayload;

public interface OmnixEmailService {
    EmailWithAttachmentResponsePayload processEmailWithAttachmentLinks(EmailWithAttachmentRequestPayload requestPayload);
}

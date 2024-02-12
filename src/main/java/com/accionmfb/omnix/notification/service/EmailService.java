/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.notification.service;

import com.accionmfb.omnix.notification.payload.EmailRequestPayload;
import com.accionmfb.omnix.notification.payload.EmailRequestWithoutAttachmentPayload;

/**
 *
 * @author bokon
 */
public interface EmailService {

    boolean validateEmailPayload(String token, EmailRequestPayload requestPayload);

    String processEmail(String token, EmailRequestPayload requestPayload);

    String processEmailWithoutAttachment(EmailRequestWithoutAttachmentPayload requestPayload);

    String processEmailCount(String token, EmailRequestPayload requestPayload);
}

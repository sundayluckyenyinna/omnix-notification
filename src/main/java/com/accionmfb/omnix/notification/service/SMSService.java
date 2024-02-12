/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.notification.service;

import com.accionmfb.omnix.notification.payload.SMSRequestPayload;
import com.accionmfb.omnix.notification.payload.SimpleSmsRequestPayload;

/**
 *
 * @author bokon
 */
public interface SMSService {

    boolean validateSMSPayload(String token, SMSRequestPayload requestPayload);

    String processSMS(String token, SMSRequestPayload requestPayload);

    String processSMSRequest(SimpleSmsRequestPayload requestPayload);
}

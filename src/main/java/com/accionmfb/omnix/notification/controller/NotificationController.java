/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.notification.controller;

import com.accionmfb.omnix.notification.constant.ResponseCodes;
import com.accionmfb.omnix.notification.exception.ExceptionResponse;
import com.accionmfb.omnix.notification.jwt.JwtTokenUtil;
import com.accionmfb.omnix.notification.payload.*;
import com.accionmfb.omnix.notification.service.EmailService;
import com.accionmfb.omnix.notification.service.SMSService;
import com.google.gson.Gson;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Collections;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.accionmfb.omnix.notification.constant.ApiPaths.*;

/**
 *
 * @author bokon
 */
@RestController
@Tag(name = "Notification Service", description = "Notification REST API")
@RefreshScope
public class NotificationController {

    @Autowired
    SMSService smsService;
    @Autowired
    EmailService emailService;
    @Autowired
    MessageSource messageSource;
    @Autowired
    Gson gson;
    @Autowired
    JwtTokenUtil jwtToken;

    @PostMapping(value = SMS_NOTIFICATION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "SMS Notification")
    public ResponseEntity<Object> smsNotification(@Valid @RequestBody SMSRequestPayload requestPayload, HttpServletRequest httpRequest) throws UnirestException {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "SMS_NOTIFICATION");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = smsService.validateSMSPayload(token, requestPayload);
        if (payloadValid) {
            String response = smsService.processSMS(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = SEND_SMS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "SMS Notification")
    public ResponseEntity<Object> smsDirectSMSNotification(@Valid @RequestBody SimpleSmsRequestPayload requestPayload, HttpServletRequest httpRequest){
        String response = smsService.processSMSRequest(requestPayload);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(value = EMAIL_NOTIFICATION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Email Notification")
    public ResponseEntity<Object> emailNotification(@Valid @RequestBody EmailRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRANSACTION_EMAIL_ALERT");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = emailService.validateEmailPayload(token, requestPayload);
        if (payloadValid) {
            String response = emailService.processEmail(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @CrossOrigin({"http://10.10.0.32:8080"})
    @PostMapping(
            value = EMAIL_NOTIFICATION_NO_ATTACHMENT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Email Notification Without Attachment")
    public ResponseEntity<Object> emailNotificationWithoutAttachment(
            @Valid @RequestBody EmailRequestWithoutAttachmentPayload requestPayload,
            HttpServletRequest httpRequest
    ) {

        String response = emailService.processEmailWithoutAttachment(requestPayload);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping(value = "/mail-count", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Email Count")
    public ResponseEntity<Object> emailCount(@Valid @RequestBody EmailRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        String response = emailService.processEmailCount(token, requestPayload);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = STATISTICS_MEMORY, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fetch the JVM statistics")
    public MemoryStats getMemoryStatistics(HttpServletRequest httpRequest) {
        MemoryStats stats = new MemoryStats();
        stats.setHeapSize(Runtime.getRuntime().totalMemory());
        stats.setHeapMaxSize(Runtime.getRuntime().maxMemory());
        stats.setHeapFreeSize(Runtime.getRuntime().freeMemory());
        return stats;
    }

}

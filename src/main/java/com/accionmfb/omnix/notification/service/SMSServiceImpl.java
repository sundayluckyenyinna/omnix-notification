/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.notification.service;

import com.accionmfb.omnix.notification.constant.ResponseCodes;
import com.accionmfb.omnix.notification.jwt.JwtTokenUtil;
import com.accionmfb.omnix.notification.model.Account;
import com.accionmfb.omnix.notification.model.AppUser;
import com.accionmfb.omnix.notification.model.Customer;
import com.accionmfb.omnix.notification.model.SMS;
import com.accionmfb.omnix.notification.payload.*;
import com.accionmfb.omnix.notification.repository.NotificationRepository;
import com.google.gson.Gson;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.StringJoiner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/**
 *
 * @author bokon
 */

@Slf4j
@Service
public class SMSServiceImpl implements SMSService {

    @Autowired
    GenericService genericService;
    @Autowired
    NotificationRepository notificationRepository;
    @Autowired
    MessageSource messageSource;
    @Autowired
    Gson gson;
    @Autowired
    JwtTokenUtil jwtToken;

    @Override
    public boolean validateSMSPayload(String token, SMSRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getAccountNumber().trim());
        rawString.add(requestPayload.getMessage().trim());
        rawString.add(requestPayload.getSmsFor().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        System.out.println("Raw String: " + rawString.toString());
        System.out.println("Decrypted: " + decryptedString);
        System.out.println(rawString.toString().equalsIgnoreCase(decryptedString));
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String processSMS(String token, SMSRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("SMS", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            Customer customer = notificationRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            if (customer == null) {
                //Log the error
                genericService.generateLog("SMS", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "SMS", "", channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            Account account = null;
            //Check if the account number is supplied
            if (!requestPayload.getAccountNumber().equalsIgnoreCase("0123456789")) {
                //Check for customer account
                account = notificationRepository.getCustomerAccount(customer, requestPayload.getAccountNumber());
                if (account == null) {
                    //Log the error
                    genericService.generateLog("SMS", token, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                    //Create User Activity log
                    genericService.createUserActivity(customer.getCustomerNumber(), "SMS", "", channel, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                    errorResponse.setResponseCode(ResponseCodes.NO_PRIMARY_ACCOUNT.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH));
                    return gson.toJson(errorResponse);
                }
            }

            //Check the channel information
            AppUser appUser = notificationRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("SMS", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "SMS", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Log the request
            SMS newSMS = new SMS();
            newSMS.setAppUser(appUser);
            newSMS.setCreatedAt(LocalDateTime.now());
            newSMS.setFailureReason("");
            newSMS.setMessage(requestPayload.getMessage());
            newSMS.setMobileNumber(requestPayload.getMobileNumber());
            newSMS.setRequestId(requestPayload.getRequestId());
            newSMS.setSmsFor(requestPayload.getSmsFor());
            newSMS.setSmsType(requestPayload.getSmsType().charAt(0));
            newSMS.setStatus("PENDING");
            newSMS.setTimePeriod(genericService.getTimePeriod());
            newSMS.setAccount(account);

            SMS createSMS = notificationRepository.createSMS(newSMS);

            //Generate the SMS payload
            SMSPayload smsPayload = new SMSPayload();
            smsPayload.setMessage(requestPayload.getMessage());
            smsPayload.setMobileNumber(requestPayload.getMobileNumber());
            String ofsRequest = gson.toJson(smsPayload);

            String middlewareResponse = genericService.postToMiddleware("/sms/send", ofsRequest);

            SMSResponsePayload responsePayload = gson.fromJson(middlewareResponse, SMSResponsePayload.class);
            if ("00".equals(responsePayload.getResponseCode())) {
                createSMS.setStatus("SUCCESS");
                notificationRepository.updateSMS(createSMS);
                //Create activity log
                genericService.generateLog("SMS", token, "Success", "API Response", "INFO", requestPayload.getRequestId());
                genericService.createUserActivity("", "SMS", "", channel, "Success", requestPayload.getMobileNumber(), 'F');
                SMSResponsePayload smsResponse = new SMSResponsePayload();
                smsResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                smsResponse.setMobileNumber(requestPayload.getMobileNumber());
                smsResponse.setMessage(requestPayload.getMessage());
                smsResponse.setSmsFor(requestPayload.getSmsFor());
                return gson.toJson(smsResponse);
            }

            createSMS.setStatus("FAILED");
            createSMS.setFailureReason(responsePayload.getResponseDescription());
            notificationRepository.updateSMS(createSMS);
            //Create activity log
            genericService.generateLog("SMS", token, middlewareResponse, "API Error", "DEBUG", requestPayload.getRequestId());
            genericService.createUserActivity("", "SMS", "", channel, middlewareResponse, requestPayload.getMobileNumber(), 'F');

            errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
            errorResponse.setResponseMessage(responsePayload.getResponseDescription());
            return gson.toJson(errorResponse);
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("SMS", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "SMS", "", channel, ex.getMessage(), requestPayload.getMobileNumber(), 'F');

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public String processSMSRequest(SimpleSmsRequestPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        SMSResponsePayload smsResponsePayload = new SMSResponsePayload();
        smsResponsePayload.setResponseCode(ResponseCodes.SERVICE_UNAVAILABLE.getResponseCode());
        smsResponsePayload.setResponseDescription("Could not process SMS request at this time, please try again.");

        // Validate the AppUser
        log.info("Validating App User credentials");
        String requestBy = requestPayload.getUsername();
        AppUser appUser = notificationRepository.getAppUserUsingUsername(requestBy);
        if (appUser == null) {
            //Log the error
            genericService.generateLog("SMS", "", messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestBy);
            //Create User Activity log
            genericService.createUserActivity("", "SMS", "", "", messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }

        log.info("Validating password credentials...");
        // Check for the validity of the password.
        String storedPassword = appUser.getPassword();
        if(!storedPassword.equals(requestPayload.getPassword())){
            //Log the error
            genericService.generateLog("SMS", "", messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestBy);
            //Create User Activity log
            genericService.createUserActivity("", "SMS", "", "", messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
            errorResponse.setResponseCode(ResponseCodes.PASSWORD_PIN_MISMATCH.getResponseCode());
            errorResponse.setResponseMessage("Invalid password passed");
            return gson.toJson(errorResponse);
        }

        log.info("Formatting mobile number for Vanso SMS...");
        // Format the mobile number
        String mobile = "";
        if(requestPayload.getMobileNumber().startsWith("234")){
            mobile = requestPayload.getMobileNumber().trim();
        }else{
            mobile = "234".concat(requestPayload.getMobileNumber().substring(1)).trim();
        }

        // Log to the database
        log.info("Writing SMS record to database logs...");
        SMS sms = new SMS();
        sms.setSmsFor("Direct sms");
        sms.setRequestId(String.valueOf(System.currentTimeMillis()));
        sms.setSmsType('N');
        sms.setAppUser(appUser);
        sms.setStatus("PENDING");
        sms.setFailureReason("");
        sms.setCreatedAt(LocalDateTime.now());
        sms.setMobileNumber("0" + mobile.substring(3));
        sms.setMessage(requestPayload.getMessage());
        sms.setTimePeriod(genericService.getTimePeriod());

        log.info("Creating SMS in the database...");
        SMS createdSMS = notificationRepository.createSMS(sms);
        log.info("SMS record created with ID: {}", createdSMS.getId());

        try {
            log.info("Calling Vanso SMS engine....");
            SmsResponse smsResponse = VansoSMS.sendMessage(mobile, requestPayload.getMessage());
            log.info("Vanso SMS response...");
            if (smsResponse != null && smsResponse.getStatus().equalsIgnoreCase("ACCEPTED") || String.valueOf(smsResponse.getErrorCode()).equalsIgnoreCase("0")) {
                smsResponsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                smsResponsePayload.setMessage("Successful");
                smsResponsePayload.setSmsFor("");
                smsResponsePayload.setMobileNumber(requestPayload.getMobileNumber());
                smsResponsePayload.setResponseDescription(smsResponse.getStatus());

                createdSMS.setStatus("SUCCESS");
                notificationRepository.updateSMS(sms);
            }
            else{
                createdSMS.setStatus("FAILED");
                createdSMS.setFailureReason(smsResponse.getErrorMessage() + " " + smsResponse.getErrorCode());
                notificationRepository.updateSMS(createdSMS);
            }
        }catch (Exception e){
            createdSMS.setFailureReason(e.getMessage());
            createdSMS.setStatus("FAILED");
            notificationRepository.updateSMS(sms);
        }

        return gson.toJson(smsResponsePayload);
    }
}

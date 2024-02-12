package com.accionmfb.omnix.notification.service;

import com.accionmfb.omnix.notification.constant.ResponseCodes;
import com.accionmfb.omnix.notification.model.AppUser;
import com.accionmfb.omnix.notification.model.SMS;
import com.accionmfb.omnix.notification.payload.SMSResponsePayload;
import com.accionmfb.omnix.notification.payload.SimpleSmsRequestPayload;
import com.accionmfb.omnix.notification.repository.NotificationRepository;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Objects;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class CentralizedNotificationCronJob {

    private final SMSService smsService;
    private final Gson gson = new Gson();
    private final NotificationRepository notificationRepository;

    @Scheduled(fixedDelay = 5000, initialDelay = 1000)
    public void finalizeSMSNotificationProcessing(){
        List<SMS> smsList = notificationRepository.getAllSMSByStatus("PENDING");
        smsList.forEach(this::processSmsNotification);
    }

    private void processSmsNotification(SMS sms){
        if(sms.getMessage().length() < 160) {
            AppUser smsAppUser = sms.getAppUser();
            smsAppUser = Objects.isNull(smsAppUser) ? getDefaultAppUSer() : smsAppUser;
            SimpleSmsRequestPayload requestPayload = new SimpleSmsRequestPayload();
            requestPayload.setUsername(smsAppUser.getUsername());
            requestPayload.setPassword(smsAppUser.getPassword());
            requestPayload.setMessage(sms.getMessage());
            requestPayload.setMobileNumber(sms.getMobileNumber());
            String responseJson = smsService.processSMSRequest(requestPayload);
            log.info("[SMS API Response] : {}", responseJson);
            try {
                SMSResponsePayload responsePayload = gson.fromJson(responseJson, SMSResponsePayload.class);
                if (responsePayload.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                    sms.setStatus("SUCCESS");
                    notificationRepository.updateSMS(sms);
                } else {
                    String failureReason = responsePayload.getResponseDescription();
                    failureReason = Objects.isNull(failureReason) || failureReason.trim().isEmpty() ? responsePayload.getMessage() : failureReason;
                    sms.setStatus("FAILED");
                    sms.setFailureReason(failureReason);
                    notificationRepository.updateSMS(sms);
                }
            } catch (Exception exception) {
                log.info("Exception occurred while deserializing sms response. Exception message is: {}", exception.getMessage());
                sms.setStatus("FAILED");
                sms.setFailureReason(exception.getMessage());
                notificationRepository.updateSMS(sms);
            }
        }
        else{
            sms.setStatus("BAD_MESSAGE");
            sms.setFailureReason("Message too long");
            notificationRepository.updateSMS(sms);
        }
    }

    private AppUser getDefaultAppUSer(){
        return notificationRepository.getAppUserUsingUsername("DEFAULT");
    }
}

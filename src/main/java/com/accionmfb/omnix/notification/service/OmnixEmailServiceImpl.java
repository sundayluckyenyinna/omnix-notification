package com.accionmfb.omnix.notification.service;

import com.accionmfb.omnix.notification.constant.ResponseCodes;
import com.accionmfb.omnix.notification.payload.Attachment;
import com.accionmfb.omnix.notification.payload.EmailWithAttachmentRequestPayload;
import com.accionmfb.omnix.notification.payload.EmailWithAttachmentResponsePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OmnixEmailServiceImpl implements OmnixEmailService{

    @Value("${email.host}")
    private String mailHost;
    @Value("${email.port}")
    private String mailPort;

    @Value("${email.username}")
    private String mailUsername;

    @Value("${email.password}")
    private String mailPassword;

    @Value("${email.from}")
    private String mailFrom;



    @Override
    public EmailWithAttachmentResponsePayload  processEmailWithAttachmentLinks(EmailWithAttachmentRequestPayload requestPayload){
        try {
            HtmlEmail email = getConfiguredHtmlMultipartEmail();
            List<Attachment> attachments = requestPayload.getAttachmentLinks();
            List<EmailAttachment> emailAttachments = attachments
                    .stream()
                    .map(Utils::convertAttachmentToEmailAttachment)
                    .collect(Collectors.toList());
            String from = Utils.returnOrDefault(requestPayload.getFrom(), mailFrom);
            requestPayload.getRecipients().forEach(recipient -> {
                try {
                    email.addTo(recipient);
                } catch (EmailException ignored) {}
            });
            email.setFrom(from);
            email.setSubject(requestPayload.getSubject());
            email.setMsg(requestPayload.getMessage());
            if(Utils.nonNullOrEmpty(requestPayload.getHtmlMessage())){
                email.setHtmlMsg(requestPayload.getHtmlMessage());
            }
            emailAttachments.forEach(emailAttachment -> {
                try {
                    email.attach(emailAttachment);
                } catch (EmailException ignored) {}
            });
            String sendId = email.send();
            return getOmniResponsePayloadForEmail(sendId, emailAttachments);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }


    private static EmailWithAttachmentResponsePayload getOmniResponsePayloadForEmail(String sendId, List<EmailAttachment> emailAttachments) {
        EmailWithAttachmentResponsePayload responsePayload = new EmailWithAttachmentResponsePayload();
        if(Objects.nonNull(sendId) && !sendId.trim().isEmpty()) {
            responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            responsePayload.setResponseMessage("Success");
            emailAttachments.forEach(emailAttachment -> {
                File file = new File(emailAttachment.getPath());
                boolean isFileDeleted  = file.delete();
                if(isFileDeleted){
                    log.info("Attachment PDF File with absolute path: {} deleted successfully", emailAttachment.getPath());
                }
            });
        }else {
            responsePayload.setResponseCode(ResponseCodes.FAILED_MODEL.getResponseCode());
            responsePayload.setResponseMessage("Failed to send email with attachment");
        }
        return responsePayload;
    }


    private SimpleEmail getConfiguredSimpleEmail() throws EmailException {
        SimpleEmail email = new SimpleEmail();
        configureEmailWithProperties(email);
        return email;
    }

    private HtmlEmail getConfiguredHtmlMultipartEmail() throws EmailException {
        HtmlEmail email = new HtmlEmail();
        configureEmailWithProperties(email);
        return email;
    }

    private void configureEmailWithProperties(Email email) throws EmailException {
        email.setHostName(mailHost);
        email.setSmtpPort(Integer.parseInt(mailPort));
        email.setAuthenticator(new DefaultAuthenticator(mailUsername, mailPassword));
        email.setSSLOnConnect(true);
        email.setFrom(mailFrom);
    }
}

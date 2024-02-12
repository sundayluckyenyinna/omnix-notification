/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.notification.service;

import com.accionmfb.omnix.notification.constant.ResponseCodes;
import com.accionmfb.omnix.notification.jwt.JwtTokenUtil;
import com.accionmfb.omnix.notification.payload.EmailRequestPayload;
import com.accionmfb.omnix.notification.payload.EmailRequestWithoutAttachmentPayload;
import com.accionmfb.omnix.notification.payload.OmniResponsePayload;
import com.accionmfb.omnix.notification.repository.NotificationRepository;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringJoiner;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 *
 * @author bokon
 */
@Service
public class EmailServiceImpl implements EmailService {

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

    @Value("${omnix.mail.protocol}")
    private String mailProtocol;
    @Value("${omnix.mail.trust}")
    private String mailTrust;

    @Override
    public boolean validateEmailPayload(String token, EmailRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getRecipientName());
        rawString.add(requestPayload.getRecipientEmail());
        rawString.add(requestPayload.getAttachmentFilePath().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String processEmail(String token, EmailRequestPayload requestPayload) {
        String channel = jwtToken.getChannelFromToken(token);
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        String response = "";
        genericService.generateLog("Email", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());

        //Check if the recipient email is supplied
        if (requestPayload.getRecipientEmail() == null || requestPayload.getRecipientEmail().equalsIgnoreCase("")) {
            //Log the error
            genericService.generateLog("Email", token, messageSource.getMessage("appMessages.email.notsupplied", new Object[0], Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getMobileNumber(), "Email", "", channel, messageSource.getMessage("appMessages.email.notsupplied", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

            errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.email.notsupplied", new Object[0], Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }

        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(mailHost);
            mailSender.setPort(Integer.parseInt(mailPort));

            mailSender.setUsername(mailUsername);
            mailSender.setPassword(mailPassword);

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", mailProtocol);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.debug", "true");
            props.put("mail.smtp.ssl.trust", mailTrust);

            MimeMessage emailDetails = mailSender.createMimeMessage();
            emailDetails.setFrom(requestPayload.getEmailLoginUsername());
            String addresses[] = new String[]{requestPayload.getRecipientEmail()};
            Address addrss[] = {};
            List<Address> addressList = new ArrayList<>();
            //Create address out of the emails
            for (String addr : addresses) {
                Address address = new InternetAddress(addr);
                addressList.add(address);
            }
            emailDetails.setRecipients(Message.RecipientType.TO, addressList.toArray(addrss));
            emailDetails.setSubject(requestPayload.getSubject());

            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(requestPayload.getEmailBody(), "text/html");

            if (requestPayload.getAttachmentFilePath() != null && !requestPayload.getAttachmentFilePath().equalsIgnoreCase("")) {
                //Add the attachment
                MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                DataSource source = new FileDataSource(requestPayload.getAttachmentFilePath());
                attachmentBodyPart.setDataHandler(new DataHandler(source));
                attachmentBodyPart.setFileName(requestPayload.getAttachmentFilePath());

                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(messageBodyPart);
                multipart.addBodyPart(attachmentBodyPart);
                emailDetails.setContent(multipart);
            }
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            emailDetails.setContent(multipart);

            mailSender.send(emailDetails);
            OmniResponsePayload responsePayload = new OmniResponsePayload();
            responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            responsePayload.setResponseMessage(messageSource.getMessage("appMessages.success.email", new Object[0], Locale.ENGLISH));
            return gson.toJson(responsePayload);
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Email", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Email", "", channel, ex.getMessage(), requestPayload.getMobileNumber(), 'F');

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public String processEmailWithoutAttachment(EmailRequestWithoutAttachmentPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();

        //Create request log
        String requestJson = gson.toJson(requestPayload);
        System.out.println("Request Json: " + requestJson);

        //Check if the recipient email is supplied
        if (requestPayload.getRecipientEmail() == null || requestPayload.getRecipientEmail().equalsIgnoreCase("")) {
            errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
            System.out.println("Recipient email not present.");
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.email.notsupplied", new Object[0], Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }

        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(mailHost);
            mailSender.setPort(Integer.parseInt(mailPort));

            mailSender.setUsername(mailUsername);
            mailSender.setPassword(mailPassword);

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", mailProtocol);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.debug", "true");
            props.put("mail.smtp.ssl.trust", mailTrust);

            MimeMessage emailDetails = mailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(emailDetails, "utf-8");
            emailDetails.setFrom(mailFrom);
            String addresses[] = new String[]{requestPayload.getRecipientEmail()};
            Address addrss[] = {};
            List<Address> addressList = new ArrayList<>();
            //Create address out of the emails
            for (String addr : addresses) {
                Address address = new InternetAddress(addr);
                addressList.add(address);
            }
            emailDetails.setRecipients(Message.RecipientType.TO, addressList.toArray(addrss));
            mimeMessageHelper.setSubject(requestPayload.getSubject());
            emailDetails.setContent(requestPayload.getEmailBody(), "text/html");

            mailSender.send(emailDetails);
            OmniResponsePayload responsePayload = new OmniResponsePayload();
            responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            responsePayload.setResponseMessage(messageSource.getMessage("appMessages.success.email", new Object[0], Locale.ENGLISH));
            return gson.toJson(responsePayload);
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }


    @Override
    public String processEmailCount(String token, EmailRequestPayload requestPayload) {
        try {
            Properties props = new Properties();
            props.put("mail.imap.starttls.enable", "true");
            props.put("mail.imap.host", "imappro.zoho.com");
            props.put("mail.imap.port", 993);
            props.put("mail.imap.ssl.enable", "true");

            Session emailSession = Session.getDefaultInstance(props, null);
            Store store = emailSession.getStore("imap");
            store.connect("imappro.zoho.com", requestPayload.getEmailLoginUsername(), requestPayload.getEmailLoginPassword());
            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            Flags seen = new Flags(Flags.Flag.SEEN);
            FlagTerm unseenFlagTerm = new FlagTerm(seen, false);

            Message[] messages = emailFolder.search(unseenFlagTerm);
            int mailCount = 0;
            for (int i = 0, n = messages.length; i < n; i++) {
                Message message = messages[i];
//                for (Address str : message.getFrom()) {
//                    if (str.getType().contains("comms@accionmfb.com")) {
//                        mailCount++;
//                    }
//                }
                if (message.getFrom()[0].toString().contains(requestPayload.getRecipientEmail())) {
                    mailCount++;
                    System.out.println("Email Count  " + mailCount);
                }
//                System.out.println("---------------------------------");
//                System.out.println("Email Number " + (i + 1));
//                System.out.println("Subject: " + message.getSubject());
//                System.out.println("From: " + message.getFrom()[0]);
//                System.out.println("Text: " + message.getContent().toString());

            }

            //close the store and folder objects
            emailFolder.close(false);
            store.close();
            return String.valueOf(mailCount);
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

}

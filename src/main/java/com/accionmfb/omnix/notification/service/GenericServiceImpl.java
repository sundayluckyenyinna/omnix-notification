/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.notification.service;

import com.accionmfb.omnix.notification.jwt.JwtTokenUtil;
import com.accionmfb.omnix.notification.model.UserActivity;
import com.accionmfb.omnix.notification.repository.NotificationRepository;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.StringJoiner;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author bokon
 */
@Service
public class GenericServiceImpl implements GenericService {

    @Autowired
    NotificationRepository walletRepository;
    @Autowired
    JwtTokenUtil jwtToken;
    @Value("${omnix.middleware.host.ip}")
    private String middlewareHostIP;
    @Value("${omnix.middleware.host.port}")
    private String middlewareHostPort;
    @Value("${omnix.middleware.authorization}")
    private String middlewareAuthorization;
    @Value("${omnix.middleware.signature.method}")
    private String middlewareSignatureMethod;
    @Value("${omnix.middleware.user.secret}")
    private String middlewareUserSecretKey;
    @Value("${omnix.middleware.username}")
    private String middlewareUsername;
    @Value("${omnix.start.morning}")
    private String startMorning;
    @Value("${omnix.end.morning}")
    private String endMorning;
    @Value("${omnix.start.afternoon}")
    private String startAfternoon;
    @Value("${omnix.end.afternoon}")
    private String endAfternoon;
    @Value("${omnix.start.evening}")
    private String startEvening;
    @Value("${omnix.end.evening}")
    private String endEvening;
    @Value("${omnix.start.night}")
    private String startNight;
    @Value("${omnix.end.night}")
    private String endNight;
    @Value("${omnix.channel.user.default}")
    private String t24Credentials;
    @Value("${omnix.version.loan.portfolio}")
    private String loanPortfolioVersion;
    @Value("${admin.consol.ip}")
    private String adminIP;
    private static SecretKeySpec secretKey;
    private static byte[] key;
    Logger logger = LoggerFactory.getLogger(GenericServiceImpl.class);

    @Override
    public void generateLog(String app, String token, String logMessage, String logType, String logLevel, String requestId) {
        try {
            String requestBy = jwtToken.getUsernameFromToken(token);
            String remoteIP = jwtToken.getIPFromToken(token);
            String channel = jwtToken.getChannelFromToken(token);

            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append(logType.toUpperCase());
            strBuilder.append(" - ");
            strBuilder.append("[").append(remoteIP).append(":").append(channel.toUpperCase()).append(":").append(requestBy.toUpperCase()).append("]");
            strBuilder.append("[").append(app.toUpperCase().toUpperCase()).append(":").append(requestId.toUpperCase()).append("]");
            strBuilder.append("[").append(logMessage).append("]");

            if ("INFO".equalsIgnoreCase(logLevel.trim())) {
                if (logger.isInfoEnabled()) {
                    logger.info(strBuilder.toString());
                }
            }

            if ("DEBUG".equalsIgnoreCase(logLevel.trim())) {
                if (logger.isDebugEnabled()) {
                    logger.error(strBuilder.toString());
                }
            }

        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                logger.debug(ex.getMessage());
            }
        }
    }

    @Override
    public void createUserActivity(String accountNumber, String activity, String amount, String channel, String message, String mobileNumber, char status) {
        UserActivity newActivity = new UserActivity();
        newActivity.setCustomerId(accountNumber);
        newActivity.setActivity(activity);
        newActivity.setAmount(amount);
        newActivity.setChannel(channel);
        newActivity.setCreatedAt(LocalDateTime.now());
        newActivity.setMessage(message);
        newActivity.setMobileNumber(mobileNumber);
        newActivity.setStatus(status);
        walletRepository.createUserActivity(newActivity);
    }

    @Override
    public String postToMiddleware(String requestEndPoint, String requestBody) {
        try {
            String middlewareEndpoint = "http://" + middlewareHostIP + ":" + middlewareHostPort + "/T24Gateway/services/generic" + requestEndPoint;
            String NONCE = String.valueOf(Math.random());
            String TIMESTAMP = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String SignaturePlain = String.format("%s:%s:%s:%s", NONCE, TIMESTAMP, middlewareUsername, middlewareUserSecretKey);
            String SIGNATURE = hash(SignaturePlain, middlewareSignatureMethod);
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> httpResponse = Unirest.post(middlewareEndpoint)
                    .header("Authorization", middlewareAuthorization)
                    .header("SignatureMethod", middlewareSignatureMethod)
                    .header("Accept", "application/json")
                    .header("Timestamp", TIMESTAMP)
                    .header("Nonce", NONCE)
                    .header("Content-Type", "application/json")
                    .header("Signature", SIGNATURE)
                    .body(requestBody)
                    .asString();
            return httpResponse.getBody();
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    private String hash(String plainText, String algorithm) {
        StringBuilder hexString = new StringBuilder();
        if (algorithm.equals("SHA512")) {
            algorithm = "SHA-512";
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(plainText.getBytes());

            byte byteData[] = md.digest();

            //convert the byte to hex format method 1
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }

            System.out.println("Hex format : " + sb.toString());

            //convert the byte to hex format method 2
            for (int i = 0; i < byteData.length; i++) {
                String hex = Integer.toHexString(0xff & byteData[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return hexString.toString().toUpperCase();
    }

    public static void setKey(String myKey) {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String decryptString(String textToDecrypt, String encryptionKey) {
        try {
            String secret = encryptionKey.trim();
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            String decryptedResponse = new String(cipher.doFinal(java.util.Base64.getDecoder().decode(textToDecrypt.trim())));
            String[] splitString = decryptedResponse.split(":");
            StringJoiner rawString = new StringJoiner(":");
            for (String str : splitString) {
                rawString.add(str.trim());
            }
            return rawString.toString();
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String validateT24Response(String responseString) {
        String responsePayload = null;
        if (responseString.contains("Authentication failed")) {
            responsePayload = responseString;
        }

        if (responseString.contains("Maximum T24 users")) {
            responsePayload = responseString;
        }

        if (responseString.contains("Failed to receive message")) {
            responsePayload = responseString;
        }

        if (responseString.contains("No records were found") || responseString.contains("No entries for the period")) {
            responsePayload = responseString;
        }

        if (responseString.contains("INVALID COMPANY SPECIFIED")) {
            responsePayload = responseString;
        }

        if (responseString.contains("java.lang.OutOfMemoryError")) {
            responsePayload = responseString;
        }

        if (responseString.contains("Failed to connect to host")) {
            responsePayload = responseString;
        }

        if (responseString.contains("No Cheques found")) {
            responsePayload = responseString;
        }

        if (responseString.contains("Unreadable")) {
            responsePayload = responseString;
        }

        if (responseString.contains("MANDATORY INPUT")) {
            responsePayload = responseString;
        }

        if (responseString.contains("Some errors while encountered")) {
            responsePayload = responseString;
        }

        if (responseString.contains("Some override conditions have not been met")) {
            responsePayload = responseString;
        }

        if (responseString.contains("don't have permissions to access this data")) {
            responsePayload = responseString;
        }

        if ("<Unreadable>".equals(responseString)) {
            responsePayload = responseString;
        }

        if (responseString.contains("User has no id")) {
            responsePayload = responseString;
        }

        if (responseString.equals("java.net.SocketException: Unexpected end of file from server")) {
            responsePayload = responseString;
        }

        if (responseString.contains("No Cash available")) {
            responsePayload = responseString;
        }

        if (responseString.contains("INVALID ACCOUNT")) {
            responsePayload = responseString;
        }

        if (responseString.contains("MISSING") && !responseString.substring(0, 4).equals("\"//1")) {
            responsePayload = responseString;
        }

        if (responseString.contains("java.net.SocketException")
                || responseString.contains("java.net.ConnectException")
                || responseString.contains("java.net.NoRouteToHostException")
                || responseString.contains("Connection timed out")
                || responseString.contains("Connection refused")) {
            responsePayload = responseString;
        }

        if (responseString.contains("SECURITY VIOLATION")) {
            responsePayload = responseString;
        }

        if (responseString.contains("NOT SUPPLIED")) {
            responsePayload = responseString;
        }

        if (responseString.contains("NO EN\\\"\\t\\\"TRIES FOR PERIOD")) {
            responsePayload = responseString;
        }

        if (responseString.contains("CANNOT ACCESS RECORD IN ANOTHER COMPANY")) {
            responsePayload = responseString;
        }

        if ("NO DATA PRESENT IN MESSAGE".equalsIgnoreCase(responseString)) {
            responsePayload = responseString;
        }

        if (responseString.contains("//-1") || responseString.contains("//-2")) {
            responsePayload = responseString;
        }

        if ("RECORD MISSING".equalsIgnoreCase(responseString)) {
            responsePayload = responseString;
        }

        if (responseString.contains("INVALID/ NO SIGN ON NAME SUPPLIED DURING SIGN ON PROCESS")) {
            responsePayload = responseString;
        }

        return responsePayload == null ? null : responsePayload;
    }

    @Override
    public String getT24TransIdFromResponse(String response) {
        String[] splitString = response.split("/");
        return splitString[0].replace("\"", "");
    }

    @Override
    public String getTextFromOFSResponse(String ofsResponse, String textToExtract) {
        try {
            String[] splitOfsResponse = ofsResponse.split(",");
            for (String str : splitOfsResponse) {
                String[] splitText = str.split("=");
                if (splitText[0].equalsIgnoreCase(textToExtract)) {
                    return splitText[1].isBlank() ? "" : splitText[1].trim();
                }
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
    }

    @Override
    public char getTimePeriod() {
        char timePeriod = 'M';
        int hour = LocalDateTime.now().getHour();
        int morningStart = Integer.valueOf(startMorning);
        int morningEnd = Integer.valueOf(endMorning);
        int afternoonStart = Integer.valueOf(startAfternoon);
        int afternoonEnd = Integer.valueOf(endAfternoon);
        int eveningStart = Integer.valueOf(startEvening);
        int eveningEnd = Integer.valueOf(endEvening);
        int nightStart = Integer.valueOf(startNight);
        int nightEnd = Integer.valueOf(endNight);
        //Check the the period of the day
        if (hour >= morningStart && hour <= morningEnd) {
            timePeriod = 'M';
        }
        if (hour >= afternoonStart && hour <= afternoonEnd) {
            timePeriod = 'A';
        }
        if (hour >= eveningStart && hour <= eveningEnd) {
            timePeriod = 'E';
        }
        if (hour >= nightStart && hour <= nightEnd) {
            timePeriod = 'N';
        }
        return timePeriod;
    }

    @Override
    public String generateTransRef(String transType) {
        long number = (long) Math.floor(Math.random() * 9_000_000_000L) + 1_000_000_000L;
        return transType + number;
    }

    @Override
    public String encryptString(String textToEncrypt, String token) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        try {
            String secret = encryptionKey.trim();
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return java.util.Base64.getEncoder().encodeToString(cipher.doFinal(textToEncrypt.trim().getBytes("UTF-8")));
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String formatAmountWithComma(String amount) {
        double value = amount == null || amount.equals("") ? new Double(0) : new Double(amount.replace(",", ""));
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(2);
        nf.setRoundingMode(RoundingMode.FLOOR);
        String formattedAmount = nf.format(value);
        return formattedAmount;
    }

    @Override
    public String formatOfsUserCredentials(String ofs, String userCredentials) {
        String[] userCredentialsSplit = userCredentials.split("/");
        String newUserCredentials = userCredentialsSplit[0] + "/#######";
        String newOfsRequest = ofs.replace(userCredentials, newUserCredentials);
        return newOfsRequest;
    }

}

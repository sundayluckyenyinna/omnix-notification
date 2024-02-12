/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.notification.service;

/**
 *
 * @author bokon
 */
public interface GenericService {

    void generateLog(String app, String token, String logMessage, String logType, String logLevel, String requestId);

    void createUserActivity(String accountNumber, String activity, String amount, String channel, String message, String mobileNumber, char status);

    String postToMiddleware(String requestEndPoint, String requestBody);

    String getT24TransIdFromResponse(String response);

    String validateT24Response(String responseString);

    String decryptString(String textToDecrypt, String encryptionKey);

    String getTextFromOFSResponse(String ofsResponse, String textToExtract);

    char getTimePeriod();

    String encryptString(String textToEncrypt, String token);

    String generateTransRef(String transType);

    String formatAmountWithComma(String amount);

    String formatOfsUserCredentials(String ofs, String userCredentials);
}

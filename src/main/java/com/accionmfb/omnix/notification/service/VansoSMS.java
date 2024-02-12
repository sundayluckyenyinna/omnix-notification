package com.accionmfb.omnix.notification.service;

import com.accionmfb.omnix.notification.payload.Account;
import com.accionmfb.omnix.notification.payload.Sms;
import com.accionmfb.omnix.notification.payload.SmsRequest;
import com.accionmfb.omnix.notification.payload.SmsResponse;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VansoSMS {

    public static String SYSTEM_ID = "NG.106.0822";

    public static String PASSWORD = "19uNqLXY";



    public static String SENDER_ID = "ACCION";



    public static SmsResponse sendMessage(String mobile, String message) {



        Gson gson = new Gson();

        HttpResponse<String> response = submitToVanso(mobile, message);



        String sResponse = response.getBody();

        System.out.println(sResponse);

        System.out.println(response.getStatus());



        SmsResponse oSmsResponse = new SmsResponse();

        if (response.getStatus() == 200) {

            oSmsResponse = gson.fromJson(sResponse, SmsResponse.class);

            System.out.println(oSmsResponse.getTicketId());

            System.out.println(oSmsResponse.getStatus());

            System.out.println(oSmsResponse.getErrorCode());

            System.out.println(oSmsResponse.getErrorMessage());

            System.out.println(oSmsResponse.getDestination());

        }

        return oSmsResponse;

    }



    public static HttpResponse<String> submitToVanso(String receipientMobileNo, String messageBody) {

        try {

            Gson gson = new Gson();

            String url = "https://sms.vanso.com/rest/sms/submit";

            SmsRequest oSmsRoot = new SmsRequest();
            Sms sms = new Sms();

            sms.setDest(receipientMobileNo);

            sms.setSrc(SENDER_ID); // read from file

            sms.setText(messageBody);

            sms.setUnicode("false");



            oSmsRoot.setSms(sms);



            Account account = new Account();

            account.setSystemId(SYSTEM_ID); // read from file

            account.setPassword(PASSWORD);  // read from file

            oSmsRoot.setAccount(account);



            String smsRequest = gson.toJson(oSmsRoot, SmsRequest.class);

            String URL = url;

//            Unirest.config().verifySsl(false);

            Properties props = System.getProperties();

            props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());



            HttpResponse<String> smsResponse = Unirest.post(URL)

                    .header("Content-Type", "application/json")

                    .body(smsRequest)

                    .asString();

            return smsResponse;

        } catch (UnirestException ex) {

            Logger.getLogger(VansoSMS.class.getName()).log(Level.SEVERE, null, ex);

        }

        return null;

    }



    private static void updateTns(long id, Connection tokenConn) {

        if (tokenConn != null) {

            String updateQry = "update [dbo].[message] set [status]  = 'Sent' where id = ?";



            try {

                try ( // store row in tns table

                      PreparedStatement pstmt = tokenConn.prepareStatement(updateQry)) {

                    pstmt.setLong(1, id);

                    pstmt.executeUpdate();



                }



            } catch (SQLException sQLException) {



            }

        }



    }

}
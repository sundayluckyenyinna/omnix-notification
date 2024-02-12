/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.notification.constant;

/**
 *
 * @author bokon
 */
public class ApiPaths {

    /**
     * This class includes the name and API end points of other microservices
     * that we need to communicate. NOTE: WRITE EVERYTHING IN ALPHABETICAL ORDER
     */
    //A
    //B
    public static final String BASE_API = "/omnix/api";
    //C
    //D
    //E
    //F

    //G
    //H
    public static final String HEADER_STRING = "Authorization";
    //I
    //L
    public static final String EMAIL_NOTIFICATION = "/email/notification";
    public static final String EMAIL_NOTIFICATION_WITH_ATTACHMENT = "/email/attachment";

    public static final String EMAIL_NOTIFICATION_NO_ATTACHMENT = "/email/notification/no-attachment";

    //M
    public static final String STATISTICS_MEMORY = "/actuator/stats";
    //N
    //O
    //P
    //S
    public static final String SMS_NOTIFICATION = "/sms/send";
    public static final String SEND_SMS = "/direct/sms/send";
    //T
    public static final String TOKEN_PREFIX = "Bearer";
    public static final String TWITTER_WEB_DATA = "/twitter";
    //W
    public static final String WEBSITE_WEB_DATA = "/website";
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.notification.repository;

import com.accionmfb.omnix.notification.model.Account;
import com.accionmfb.omnix.notification.model.AppUser;
import com.accionmfb.omnix.notification.model.Customer;
import com.accionmfb.omnix.notification.model.Emails;
import com.accionmfb.omnix.notification.model.SMS;
import com.accionmfb.omnix.notification.model.UserActivity;

import java.util.List;

/**
 *
 * @author bokon
 */
public interface NotificationRepository {

    UserActivity createUserActivity(UserActivity userActivity);

    SMS createSMS(SMS sms);

    SMS updateSMS(SMS sms);

    Emails createEmail(Emails email);

    Emails updateEmail(Emails email);

    AppUser getAppUserUsingUsername(String username);

    Customer getCustomerUsingMobileNumber(String mobileNumber);

    Account getCustomerAccount(Customer customer, String accountNumber);

    List<SMS> getAllSMSByStatus(String status);
}

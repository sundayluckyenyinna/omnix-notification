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
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author bokon
 */
@Repository
@Transactional
public class NotificationRepositoryImpl implements NotificationRepository {

    @PersistenceContext
    EntityManager em;

    @Override
    public UserActivity createUserActivity(UserActivity userActivity) {
        em.persist(userActivity);
        em.flush();
        return userActivity;
    }

    @Override
    public AppUser getAppUserUsingUsername(String username) {
        TypedQuery<AppUser> query = em.createQuery("SELECT t FROM AppUser t WHERE t.username = :username", AppUser.class)
                .setParameter("username", username);
        List<AppUser> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public SMS createSMS(SMS sms) {
        em.persist(sms);
        em.flush();
        return sms;
    }

    @Override
    public SMS updateSMS(SMS sms) {
        em.merge(sms);
        em.flush();
        return sms;
    }

    @Override
    public Emails createEmail(Emails email) {
        em.persist(email);
        em.flush();
        return email;
    }

    @Override
    public Emails updateEmail(Emails email) {
        em.merge(email);
        em.flush();
        return email;
    }

    @Override
    public Customer getCustomerUsingMobileNumber(String mobileNumber) {
        TypedQuery<Customer> query = em.createQuery("SELECT t FROM Customer t WHERE t.mobileNumber = :mobileNumber", Customer.class)
                .setParameter("mobileNumber", mobileNumber);
        List<Customer> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Account getCustomerAccount(Customer customer, String accountNumber) {
        TypedQuery<Account> query = em.createQuery("SELECT t FROM Account t WHERE t.customer = :customer AND t.accountNumber = :accountNumber OR t.oldAccountNumber = :accountNumber", Account.class)
                .setParameter("customer", customer)
                .setParameter("accountNumber", accountNumber);
        List<Account> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<SMS> getAllSMSByStatus(String status){
        return em.createQuery("select s from SMS s where s.status =: status", SMS.class)
                .setParameter("status", status)
                .getResultList();
    }
}

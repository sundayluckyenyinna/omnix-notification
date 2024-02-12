/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.notification.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author bokon
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "email")
public class Emails implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "email_address")
    private String emailAddress = "";
    @Column(name = "mobile_number")
    private String mobileNumber;
    @Column(name = "status")
    private String status;
    @Column(name = "message", length = 100000)
    private String message;
    @Column(name = "email_type")
    private String emailType;
    @ManyToOne
    private Customer customer;
    @ManyToOne
    private AppUser appUser;
    @Column(name = "failure_reason")
    private String failureReason = "";
    @Column(name = "request_id")
    private String requestId;
}

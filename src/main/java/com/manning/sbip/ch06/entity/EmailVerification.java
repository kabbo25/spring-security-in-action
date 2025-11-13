package com.manning.sbip.ch06.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

@Data
@Entity
@Table(name = "CT_EMAIL_VERIFICATIONS")
@NoArgsConstructor
public class EmailVerification {

    @Id
    @GeneratedValue(generator = "UUID_GENERATOR")
    @GenericGenerator(name = "UUID_GENERATOR", strategy = "org.hibernate.id.UUIDGenerator")
    private String verificationId;

    private String username;

    public EmailVerification(String username) {
        this.username = username;
    }
}

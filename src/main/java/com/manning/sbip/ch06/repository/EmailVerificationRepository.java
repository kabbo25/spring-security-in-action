package com.manning.sbip.ch06.repository;

import com.manning.sbip.ch06.entity.EmailVerification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailVerificationRepository extends CrudRepository<EmailVerification, String> {

    boolean existsByUsername(String username);

    EmailVerification findByUsername(String username);
}

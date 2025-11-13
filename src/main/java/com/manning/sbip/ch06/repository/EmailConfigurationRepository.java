package com.manning.sbip.ch06.repository;

import com.manning.sbip.ch06.entity.EmailConfiguration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailConfigurationRepository extends CrudRepository<EmailConfiguration, Long> {

    Optional<EmailConfiguration> findByActiveTrue();
}

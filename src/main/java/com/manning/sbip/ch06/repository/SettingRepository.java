package com.manning.sbip.ch06.repository;

import com.manning.sbip.ch06.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for accessing organization settings including JWT keys
 */
@Repository
public interface SettingRepository extends JpaRepository<Setting, Long> {

    /**
     * Find setting by organization code
     *
     * @param organizationCode the organization code
     * @return Optional containing the setting if found
     */
    Optional<Setting> findByOrganizationCode(String organizationCode);

    /**
     * Check if JWT keys are configured for an organization
     *
     * @param organizationCode the organization code
     * @return true if both private and public keys are not null
     */
    default boolean hasJwtKeys(String organizationCode) {
        return findByOrganizationCode(organizationCode)
                .map(setting -> setting.getJwtPrivateKey() != null && setting.getJwtPublicKey() != null)
                .orElse(false);
    }
}

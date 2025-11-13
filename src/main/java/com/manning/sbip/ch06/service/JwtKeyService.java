package com.manning.sbip.ch06.service;

import com.manning.sbip.ch06.entity.Setting;
import com.manning.sbip.ch06.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Service for loading and managing JWT keys from the database
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtKeyService {

    private final SettingRepository settingRepository;

    @Value("${jwt.organization.code:DSI-007}")
    private String organizationCode;

    /**
     * Load the private key for JWT signing from the database
     *
     * @return PrivateKey object for JWT signing
     * @throws IllegalStateException if keys are not configured or invalid
     */
    @Cacheable(value = "jwtPrivateKey", key = "#root.target.organizationCode")
    public PrivateKey loadPrivateKey() {
        log.info("Loading JWT private key from database for organization: {}", organizationCode);

        Setting setting = settingRepository.findByOrganizationCode(organizationCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Organization setting not found for: " + organizationCode));

        if (setting.getJwtPrivateKey() == null || setting.getJwtPrivateKey().isBlank()) {
            throw new IllegalStateException(
                    "JWT private key not configured for organization: " + organizationCode);
        }

        try {
            return convertPemToPrivateKey(setting.getJwtPrivateKey());
        } catch (Exception e) {
            log.error("Failed to load JWT private key", e);
            throw new IllegalStateException("Invalid JWT private key configuration", e);
        }
    }

    /**
     * Load the public key for JWT verification from the database
     *
     * @return PublicKey object for JWT verification
     * @throws IllegalStateException if keys are not configured or invalid
     */
    @Cacheable(value = "jwtPublicKey", key = "#root.target.organizationCode")
    public PublicKey loadPublicKey() {
        log.info("Loading JWT public key from database for organization: {}", organizationCode);

        Setting setting = settingRepository.findByOrganizationCode(organizationCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Organization setting not found for: " + organizationCode));

        if (setting.getJwtPublicKey() == null || setting.getJwtPublicKey().isBlank()) {
            throw new IllegalStateException(
                    "JWT public key not configured for organization: " + organizationCode);
        }

        try {
            return convertPemToPublicKey(setting.getJwtPublicKey());
        } catch (Exception e) {
            log.error("Failed to load JWT public key", e);
            throw new IllegalStateException("Invalid JWT public key configuration", e);
        }
    }

    /**
     * Get the JWT algorithm configured for the organization
     *
     * @return JWT algorithm (e.g., "RS256")
     */
    public String getAlgorithm() {
        Setting setting = settingRepository.findByOrganizationCode(organizationCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Organization setting not found for: " + organizationCode));

        return setting.getJwtAlgorithm() != null ? setting.getJwtAlgorithm() : "RS256";
    }

    /**
     * Get the JWT issuer configured for the organization
     *
     * @return JWT issuer URL
     */
    public String getIssuer() {
        Setting setting = settingRepository.findByOrganizationCode(organizationCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Organization setting not found for: " + organizationCode));

        return setting.getJwtIssuer() != null ? setting.getJwtIssuer() : "https://springauth.local";
    }

    /**
     * Get the JWT token expiration time in minutes
     *
     * @return expiration time in minutes
     */
    public int getExpirationMinutes() {
        Setting setting = settingRepository.findByOrganizationCode(organizationCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Organization setting not found for: " + organizationCode));

        return setting.getJwtExpirationMinutes() != null ? setting.getJwtExpirationMinutes() : 60;
    }

    /**
     * Check if JWT keys are configured for the organization
     *
     * @return true if both private and public keys are configured
     */
    public boolean areKeysConfigured() {
        return settingRepository.findByOrganizationCode(organizationCode)
                .map(setting -> setting.getJwtPrivateKey() != null
                        && !setting.getJwtPrivateKey().isBlank()
                        && setting.getJwtPublicKey() != null
                        && !setting.getJwtPublicKey().isBlank())
                .orElse(false);
    }

    /**
     * Convert PEM-formatted private key string to PrivateKey object
     *
     * @param pem PEM-formatted private key string
     * @return PrivateKey object
     * @throws Exception if conversion fails
     */
    private PrivateKey convertPemToPrivateKey(String pem) throws Exception {
        // Remove PEM headers and whitespace
        String privateKeyPEM = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        // Decode base64
        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

        // Generate private key
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);

        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Convert PEM-formatted public key string to PublicKey object
     *
     * @param pem PEM-formatted public key string
     * @return PublicKey object
     * @throws Exception if conversion fails
     */
    private PublicKey convertPemToPublicKey(String pem) throws Exception {
        // Remove PEM headers and whitespace
        String publicKeyPEM = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        // Decode base64
        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);

        // Generate public key
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);

        return keyFactory.generatePublic(keySpec);
    }
}

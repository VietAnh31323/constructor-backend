package com.backend.constructor.config.properties;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RsaKeyProperties {

    private final Environment environment;
    private final ResourceLoader resourceLoader;

    @Value("${jwt.expired:1440}")
    private long expired;

    @Value("${jwt.refresh-expired-days:15}")
    private long refreshExpiredDays;

    @Value("${jwt.issuer:constructor-backend}")
    private String issuer;

    @Value("${jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${jwt.rsa-public-key:}")
    private String publicKeySource;

    @Value("${jwt.rsa-private-key:}")
    private String privateKeySource;

    private RSAPublicKey rsaPublicKey;
    private RSAPrivateKey rsaPrivateKey;

    public RsaKeyProperties(Environment environment, ResourceLoader resourceLoader) {
        this.environment = environment;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        try {
            if (StringUtils.hasText(publicKeySource) && StringUtils.hasText(privateKeySource)) {
                this.rsaPublicKey = parsePublicKey(resolveContent(publicKeySource));
                this.rsaPrivateKey = parsePrivateKey(resolveContent(privateKeySource));
                log.info("Loaded JWT RSA keys from configured source");
                return;
            }

            Set<String> activeProfiles = Arrays.stream(environment.getActiveProfiles())
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            if (activeProfiles.contains("prod") || activeProfiles.contains("production") || activeProfiles.contains("staging")) {
                throw new IllegalStateException("JWT_PUBLIC_KEY and JWT_PRIVATE_KEY must be configured in staging/prod environments!");
            }

            loadOrGenerateDevKeys();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize JWT RSA keys", e);
        }
    }

    private void loadOrGenerateDevKeys() throws Exception {
        Path devCertDir = Path.of(".dev-certs");
        Path pubPath = devCertDir.resolve("publicKey.pem");
        Path privPath = devCertDir.resolve("privateKey.pem");

        if (Files.exists(pubPath) && Files.exists(privPath)) {
            this.rsaPublicKey = parsePublicKey(Files.readString(pubPath, StandardCharsets.UTF_8));
            this.rsaPrivateKey = parsePrivateKey(Files.readString(privPath, StandardCharsets.UTF_8));
            log.info("Loaded local DEV JWT RSA keypair from .dev-certs/");
            return;
        }

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        this.rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
        this.rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();

        try {
            Files.createDirectories(devCertDir);
            String pubPem = "-----BEGIN PUBLIC KEY-----\n"
                    + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(rsaPublicKey.getEncoded())
                    + "\n-----END PUBLIC KEY-----\n";
            String privPem = "-----BEGIN PRIVATE KEY-----\n"
                    + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(rsaPrivateKey.getEncoded())
                    + "\n-----END PRIVATE KEY-----\n";
            Files.writeString(pubPath, pubPem, StandardCharsets.UTF_8);
            Files.writeString(privPath, privPem, StandardCharsets.UTF_8);
            log.warn("Generated local DEV JWT RSA keypair in .dev-certs/ (do not use in production)");
        } catch (Exception ex) {
            log.warn("Using in-memory DEV JWT RSA keypair: {}", ex.getMessage());
        }
    }

    private String resolveContent(String source) throws Exception {
        String trimmed = source.trim();
        if (trimmed.startsWith("classpath:") || trimmed.startsWith("file:")) {
            Resource resource = resourceLoader.getResource(trimmed);
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return trimmed.replace("\\n", "\n");
    }

    private RSAPublicKey parsePublicKey(String pem) throws Exception {
        String sanitized = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(sanitized);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(decoded));
    }

    private RSAPrivateKey parsePrivateKey(String pem) throws Exception {
        String sanitized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(sanitized);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    public long expired() {
        return expired;
    }

    public long refreshExpiredDays() {
        return refreshExpiredDays;
    }

    public String issuer() {
        return issuer;
    }

    public boolean cookieSecure() {
        return cookieSecure;
    }

    public RSAPublicKey rsaPublicKey() {
        return rsaPublicKey;
    }

    public RSAPrivateKey rsaPrivateKey() {
        return rsaPrivateKey;
    }
}

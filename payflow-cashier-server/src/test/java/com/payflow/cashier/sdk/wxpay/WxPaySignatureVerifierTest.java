package com.payflow.cashier.sdk.wxpay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WxPaySignatureVerifierTest {

    @Mock
    private WxPayPlatformCertCache platformCertCache;

    @InjectMocks
    private WxPaySignatureVerifier verifier;

    @Test
    void verify_rejectsExpiredTimestamp() {
        String oldTs = String.valueOf(System.currentTimeMillis() / 1000 - 400);
        assertFalse(verifier.verify("SERIAL", "sig", oldTs, "nonce", "{}"));
    }

    @Test
    void verify_rejectsWhenNoPlatformCert() {
        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        when(platformCertCache.findBySerial("SERIAL")).thenReturn(null);
        assertFalse(verifier.verify("SERIAL", "sig", ts, "nonce", "{}"));
    }

    @Test
    void verify_acceptsValidSignature() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        PublicKey publicKey = kp.getPublic();

        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = "nonce123";
        String body = "{\"event_type\":\"TRANSACTION.SUCCESS\"}";
        String message = timestamp + "\n" + nonce + "\n" + body + "\n";

        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initSign(kp.getPrivate());
        sign.update(message.getBytes());
        String signature = Base64.getEncoder().encodeToString(sign.sign());

        when(platformCertCache.findBySerial("SERIAL-1")).thenReturn(publicKey);

        assertTrue(verifier.verify("SERIAL-1", signature, timestamp, nonce, body));
    }
}

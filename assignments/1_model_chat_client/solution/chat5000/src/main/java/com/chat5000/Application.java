package com.chat5000;

import io.micronaut.runtime.Micronaut;

/**
 * Entry point for the Chat5000 Micronaut application.
 * Configures PQC-compliant TLS 1.3 named groups (X25519MLKEM768) when not overridden via JVM args.
 */
public final class Application {

    private static final String PQC_NAMED_GROUPS = "X25519MLKEM768,x25519,secp384r1";

    private Application() {
    }

    public static void main(String[] args) {
        if (System.getProperty("jdk.tls.namedGroups") == null) {
            System.setProperty("jdk.tls.namedGroups", PQC_NAMED_GROUPS);
        }
        Micronaut.run(Application.class, args);
    }
}

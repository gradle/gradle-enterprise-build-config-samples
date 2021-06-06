package com.acme;

import software.amazon.awssdk.services.kms.KmsClient;

public class KeyManagementService {

    public static String getBuildCachePassword() {
        // TODO replace with real code
        return KmsClient.create().listKeys().keys().get(0).keyId();
    }
}

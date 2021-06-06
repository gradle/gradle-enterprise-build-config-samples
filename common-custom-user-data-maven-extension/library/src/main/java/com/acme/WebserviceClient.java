package com.acme;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

public class WebserviceClient {

    public static int queryStatus() {
        // TODO replace with real code
        Client client = ClientBuilder.newClient();
        try {
            return client.target("https://example.com")
                    .request(TEXT_HTML)
                    .get()
                    .getStatus();
        } finally {
            client.close();
        }
    }
}

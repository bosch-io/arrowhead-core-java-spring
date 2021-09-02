/********************************************************************************
* Copyright (c) 2021 Bosch.IO GmbH[ and others]
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
********************************************************************************/

package eu.arrowhead.core.confmgr.arrowhead;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class ArrowheadAuthorizationSystemClientTest {

    @Test
    public void givenPublicKeyIsAvailable_whenGetPublicKey_thenPublicKeyReceived() throws Exception {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/plain")
                .setBody("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvmaFymz4cSB+LP3pixhhgrczq8G2rWTMyyajMQNSXOuvGDBhB7S32rl/+B8VUvTrYBx7ab8AM/TxrDwWAzsQ3p3oo0+5NNibHpZ8QucWIXqbpQqFobiNWx+ogoBLV4UfJekxXYH54QrTdwkvlOFXxjkL11uwBKEsZ0TzKgGaRs+iR80sjDeicnpPlMm0RbpXjx10OzmjqfC1Wrusyl2gjsW9ySfwJ1461n6cPndn8csvC37IHdHpkKTRlcDDPG6/GN3+vfUeznA7zGFWv2IOTzsuruehXaOuDNgekvREK3Xh4BpvGTZ6oEFVmr5853U3PS6ExKlvwR8NwFiPXI9LAQIDAQAB"));
        mockWebServer.start();

        String baseUrl = mockWebServer.url("").toString();
        ArrowheadAuthorizationSystemClient client = new ArrowheadAuthorizationSystemClient(baseUrl);

        String publicKey = client.getPublicKey();

        RecordedRequest recordedRequest = mockWebServer.takeRequest(10, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/authorization/publickey");

        assertThat(publicKey).isEqualTo("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvmaFymz4cSB+LP3pixhhgrczq8G2rWTMyyajMQNSXOuvGDBhB7S32rl/+B8VUvTrYBx7ab8AM/TxrDwWAzsQ3p3oo0+5NNibHpZ8QucWIXqbpQqFobiNWx+ogoBLV4UfJekxXYH54QrTdwkvlOFXxjkL11uwBKEsZ0TzKgGaRs+iR80sjDeicnpPlMm0RbpXjx10OzmjqfC1Wrusyl2gjsW9ySfwJ1461n6cPndn8csvC37IHdHpkKTRlcDDPG6/GN3+vfUeznA7zGFWv2IOTzsuruehXaOuDNgekvREK3Xh4BpvGTZ6oEFVmr5853U3PS6ExKlvwR8NwFiPXI9LAQIDAQAB");

        mockWebServer.close();
    }

}

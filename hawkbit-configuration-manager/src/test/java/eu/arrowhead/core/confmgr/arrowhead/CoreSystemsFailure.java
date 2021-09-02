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

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.ConnectException;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import eu.arrowhead.core.confmgr.arrowhead.model.request.ServiceRegistryRequestDTO;
import eu.arrowhead.core.confmgr.arrowhead.model.request.SystemRequestDTO;


public class CoreSystemsFailure {
    @Test
    public void testServiceRegistryFailure() {
        ArrowheadServiceRegistryClient srClient = new ArrowheadServiceRegistryClient("https://localhost:8443");

        ServiceRegistryRequestDTO requestDTO = ServiceRegistryRequestDTO.builder()
            .serviceDefinition("definition3")
            .providerSystem(SystemRequestDTO.builder()
                .systemName("conf-system")
                .address("192.168.1.1")
                .port(1234)
                .authenticationInfo("test")
                .build())
            .serviceUri("/")
            .secure(ServiceRegistryRequestDTO.SecurityLevel.TOKEN)
            .version(1)
            .interfaces(Collections.singletonList("HTTPS-SECURE-JSON"))
            .build();

        Exception exception = assertThrows(Exception.class, () -> {
            srClient.registerService(requestDTO);
        });

        assertEquals(ConnectException.class, exception.getCause().getCause().getClass());
    }

    @Test
    public void testAuthorizationSystemFailure() {
        ArrowheadAuthorizationSystemClient authClient = new ArrowheadAuthorizationSystemClient("https://localhost:8443");

        Exception exception = assertThrows(Exception.class, () -> {
            authClient.getPublicKey();
        });

        assertEquals(ConnectException.class, exception.getCause().getCause().getClass());
    }
}

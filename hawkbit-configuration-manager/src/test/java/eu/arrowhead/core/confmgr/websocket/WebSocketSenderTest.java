/********************************************************************************
* Copyright (c) 2021 Bosch.IO GmbH[ and others]
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
********************************************************************************/

package eu.arrowhead.core.confmgr.websocket;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import eu.arrowhead.core.confmgr.hawkbit.model.inbound.MessageTypeInbound;
import eu.arrowhead.core.confmgr.hawkbit.model.inbound.ThingDeletedInboundMessage;
import eu.arrowhead.core.confmgr.websocket.model.DeviceMessage;

public class WebSocketSenderTest {
    WebsocketSender wsSender;

    WebSocketSession wsSession;

    @BeforeEach
    public void init() {
        ConcurrentHashMap<String, WebSocketSession> webSocketSessionMap = new ConcurrentHashMap<>();
        
        wsSession = mock(WebSocketSession.class);
        webSocketSessionMap.put("testIdX", wsSession);

        wsSender = new WebsocketSender(webSocketSessionMap);
    }

    @Test
    public void testSendMessageWithMissingSessionWebSocketSessionMap() throws IOException {
        DeviceMessage deviceMessage = DeviceMessage.builder()
            .type(MessageTypeInbound.EVENT.toString())
            .message(
                ThingDeletedInboundMessage.builder()
                    .headers(
                        ThingDeletedInboundMessage.Headers.builder()
                            .thingId("testIdY")
                            .build()
                    )
                    .build()
            )
            .build();

        assertThrows(DeviceNotConnectedException.class, () -> {
            wsSender.sendMessage("testIdY", deviceMessage);
        });
    }

    @Test
    public void testSendMessageWithExistingSession() throws IOException,
            DeviceNotConnectedException {
        DeviceMessage deviceMessage = DeviceMessage.builder()
            .type(MessageTypeInbound.EVENT.toString())
            .message(
                ThingDeletedInboundMessage.builder()
                    .headers(
                        ThingDeletedInboundMessage.Headers.builder()
                            .thingId("testIdX")
                            .build()
                    )
                    .build()
            )
            .build();
        
        String expectedBody = "{"
            + "\"type\":\"EVENT\","
            + "\"message\":{"
            +     "\"headers\":{"
            +         "\"thingId\":\"testIdX\""
            +     "}"
            + "}"
        + "}";

        TextMessage expectedWSMessage = new TextMessage(expectedBody);

        wsSender.sendMessage("testIdX", deviceMessage);

        verify(wsSession).sendMessage(expectedWSMessage);
    }    
}

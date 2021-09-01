/********************************************************************************
 * Copyright (c) 2021 {Lulea University of Technology}
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 
 *
 * Contributors: 
 *   {Lulea University of Technology} - implementation
 *   Arrowhead Consortia - conceptualization 
 ********************************************************************************/
package eu.arrowhead.core.datamanager;
 
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.core.datamanager.service.HistorianService;
import eu.arrowhead.core.datamanager.security.DatamanagerACLFilter;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.core.datamanager.service.DataManagerDriver;
import eu.arrowhead.core.datamanager.security.DatamanagerACLFilter;
import eu.arrowhead.common.dto.shared.SenML;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

@Component
public class HistorianWSHandler extends TextWebSocketHandler {
 
    private final Logger logger = LogManager.getLogger(HistorianWSHandler.class);
    private Gson gson = new Gson();
    
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
 
    @Value("${server.ssl.enabled}")
    private boolean sslEnabled;

    @Autowired
    private HistorianService historianService;

    @Autowired
    private DataManagerDriver dataManagerDriver;

    @Autowired
    DatamanagerACLFilter dataManagerACLFilter;

    //=================================================================================================
    // methods

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        super.afterConnectionEstablished(session);
    }
 
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        super.afterConnectionClosed(session, status);
    }
 
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        
        Map<String,Object> attributes = session.getAttributes();
	    String systemName, serviceName, payload;

	    try {
		    systemName = (String)session.getAttributes().get("systemId");
		    serviceName = (String)session.getAttributes().get("serviceId");
            String CN = systemName;
            if (sslEnabled ) {
                CN = (String)attributes.get("CN");
            }
            
            payload = message.getPayload();

		    //logger.debug("Got message for {}/{}", systemName, serviceName);
            boolean authorized = dataManagerACLFilter.checkRequest(CN, "PUT", CommonConstants.DATAMANAGER_URI + CommonConstants.OP_DATAMANAGER_HISTORIAN + "/ws/" + attributes.get("systemId") + "/" + attributes.get("serviceId"));
            if(authorized) {
                Vector<SenML> sml = gson.fromJson(payload, new TypeToken<Vector<SenML>>(){}.getType());
		        dataManagerDriver.validateSenMLMessage(systemName, serviceName, sml);

		        SenML head = sml.firstElement();
		        if(head.getBt() == null) {
			        head.setBt((double)System.currentTimeMillis() / 1000);
		        } else {
                    double deltaTime = ((double)System.currentTimeMillis() / 1000) - head.getBt();
                    deltaTime *= 1000.0;
                    //logger.info("Message took {} ms ", String.format("%.3f", deltaTime));
                }

		        dataManagerDriver.validateSenMLContent(sml);
                historianService.createEndpoint(systemName, serviceName);
    		    historianService.updateEndpoint(systemName, serviceName, sml);

            } else {
		        session.close();
            }
	    } catch(Exception e) {
		    session.close();
		    return;

	    }

	    //logger.debug("Incoming msg: \n" + payload + "\n from " + systemName + "/" + serviceName); //TODO: decide if EventHandler like forwaridng should be added!
        /*sessions.forEach(webSocketSession -> {
            try {
                webSocketSession.sendMessage(message); //XXX: only send to sessions that are connected to the system+service combo!!
            } catch (IOException e) {
                logger.error("Error occurred.", e);
            }
        });*/
    }
}


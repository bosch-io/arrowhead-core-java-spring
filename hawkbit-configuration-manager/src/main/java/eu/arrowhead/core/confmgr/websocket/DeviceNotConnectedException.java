/********************************************************************************
* Copyright (c) 2021 Bosch.IO GmbH[ and others]
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
********************************************************************************/

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

public class DeviceNotConnectedException extends Exception {
    private static final long serialVersionUID = 7329483234L;

    public DeviceNotConnectedException() { super(); }
    public DeviceNotConnectedException(String message) { super(message); }
    public DeviceNotConnectedException(String message, Throwable cause) { super(message, cause); }
    public DeviceNotConnectedException(Throwable cause) { super(cause); }
}

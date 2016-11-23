/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.mqtt;

import com.whizzosoftware.hobson.api.variable.DeviceVariableState;

import java.util.Collection;
import java.util.Map;

public interface MQTTEventDelegate {
    void activateDevice(String deviceId, Map<String,Object> variables);
    String getDeviceSecret(String deviceId);
    boolean hasDevice(String deviceId);
    boolean isDeviceActivated(String deviceId);
    void onDeviceData(String id, Collection<DeviceVariableState> objects);
}

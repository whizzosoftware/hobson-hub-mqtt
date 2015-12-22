/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.mqtt;

import com.whizzosoftware.hobson.api.device.DevicePassport;
import com.whizzosoftware.hobson.api.variable.VariableUpdate;

import java.util.Collection;

public interface MQTTEventDelegate {
    DevicePassport activateDevicePassport(String deviceId);
    DevicePassport getDevicePassport(String bootstrapId);
    void onPassportRegistration(String id, String name, Collection<VariableUpdate> initialData);
    void onDeviceData(String id, Collection<VariableUpdate> objects);
}

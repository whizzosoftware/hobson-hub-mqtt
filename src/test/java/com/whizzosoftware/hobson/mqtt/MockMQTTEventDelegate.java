/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.mqtt;

import com.whizzosoftware.hobson.api.variable.DeviceVariableState;

import java.util.*;

public class MockMQTTEventDelegate implements MQTTEventDelegate {
    private Map<String,String> registrations = new HashMap<>();
    private Map<String,Collection<DeviceVariableState>> data = new HashMap<>();

    public int getEventCount() {
        return registrations.size() + data.size();
    }

    public void registerDevice(String deviceId) {
        registrations.put(deviceId, UUID.randomUUID().toString());
    }

    public Collection<String> getDeviceRegistrationIds() {
        return registrations.keySet();
    }

    public Collection<DeviceVariableState> getData(String id) {
        return data.get(id);
    }

    @Override
    public boolean isDeviceActivated(String deviceId) {
        return registrations.containsKey(deviceId);
    }

    @Override
    public String getDeviceSecret(String deviceId) {
        return registrations.get(deviceId);
    }

    @Override
    public boolean hasDevice(String deviceId) {
        return true;
    }

    @Override
    public void onDeviceData(String id, Collection<DeviceVariableState> objects) {
        data.put(id, objects);
    }

    @Override
    public void activateDevice(String deviceId, Map<String,Object> variables) {
        registrations.put(deviceId, UUID.randomUUID().toString());
    }
}

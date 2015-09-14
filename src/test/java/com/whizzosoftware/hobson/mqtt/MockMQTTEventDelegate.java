/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.mqtt;

import com.whizzosoftware.hobson.api.device.DeviceBootstrap;
import com.whizzosoftware.smartobjects.SmartObject;

import java.util.*;

public class MockMQTTEventDelegate implements MQTTEventDelegate {
    private Map<String,String> registrations = new HashMap<>();
    private Map<String,DeviceBootstrap> bootstrapToDeviceMap = new HashMap<>();
    private Map<String,Collection<SmartObject>> data = new HashMap<>();

    public int getEventCount() {
        return registrations.size() + data.size();
    }

    public Collection<String> getDeviceRegistrationIds() {
        return registrations.keySet();
    }

    public String getDeviceRegistrationName(String id) {
        return registrations.get(id);
    }

    public Collection<SmartObject> getData(String id) {
        return data.get(id);
    }

    @Override
    public DeviceBootstrap registerDeviceBootstrap(String deviceId) {
        DeviceBootstrap db = new DeviceBootstrap(UUID.randomUUID().toString(), deviceId, System.currentTimeMillis());
        db.setSecret(deviceId);
        bootstrapToDeviceMap.put(db.getId(), db);
        return db;
    }

    @Override
    public DeviceBootstrap getDeviceBootstrap(String bootstrapId) {
        return bootstrapToDeviceMap.get(bootstrapId);
    }

    @Override
    public void onBootstrapRegistration(String id, String name, Collection<SmartObject> initialData) {
        registrations.put(id, name);
    }

    @Override
    public void onDeviceData(String id, Collection<SmartObject> objects) {
        data.put(id, objects);
    }
}

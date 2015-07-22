package com.whizzosoftware.hobson.mqtt;

import com.whizzosoftware.hobson.ipsoso.object.SmartObject;

import java.util.*;

public class MockMQTTEventListener implements MQTTEventListener {
    private Map<String,String> registrations = new HashMap<>();
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
    public void onDeviceRegistration(String id, String name, Collection<SmartObject> initialData) {
        registrations.put(id, name);
    }

    @Override
    public void onDeviceData(String id, Collection<SmartObject> objects) {
        data.put(id, objects);
    }
}
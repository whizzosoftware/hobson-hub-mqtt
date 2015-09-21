/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.mqtt.device;

import com.whizzosoftware.hobson.api.device.AbstractHobsonDevice;
import com.whizzosoftware.hobson.api.device.DeviceType;
import com.whizzosoftware.hobson.api.plugin.HobsonPlugin;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.api.variable.HobsonVariable;
import com.whizzosoftware.hobson.api.variable.VariableUpdate;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class MQTTDevice extends AbstractHobsonDevice {
    private DeviceType type;
    private Map<String,MQTTDeviceVariable> variableMap = new HashMap<>();

    /**
     * Constructor.
     *
     * @param plugin the HobsonPlugin that created this device
     * @param id     the device ID
     */
    public MQTTDevice(HobsonPlugin plugin, String id, String name, DeviceType type, Collection<VariableUpdate> initialData) {
        super(plugin, id);

        setDefaultName(name);

        this.type = type;

        if (initialData != null) {
            for (VariableUpdate vu : initialData) {
                variableMap.put(vu.getName(), new MQTTDeviceVariable(vu.getName(), HobsonVariable.Mask.READ_ONLY, vu.getValue()));
            }
        }
    }

    /**
     * Constructor.
     *
     * @param plugin the HobsonPlugin that created this device
     * @param json a JSON representation of the device
     */
    public MQTTDevice(HobsonPlugin plugin, JSONObject json) {
        super(plugin, json.getString("id"));

        setDefaultName(json.getString("name"));

        this.type = DeviceType.valueOf(json.getString("type"));

        JSONArray varArray = json.getJSONArray("vars");
        for (int i=0; i < varArray.length(); i++) {
            JSONObject varJson = varArray.getJSONObject(i);
            String name = varJson.getString("name");
            variableMap.put(name, new MQTTDeviceVariable(name, HobsonVariable.Mask.valueOf(varJson.getString("mask")), null));
        }
    }

    @Override
    public void onStartup(PropertyContainer config) {
        if (variableMap != null) {
            for (MQTTDeviceVariable var : variableMap.values()) {
                publishVariable(var.name, var.initialValue, var.mask);
            }
        }
    }

    @Override
    protected TypedProperty[] createSupportedProperties() {
        return null;
    }

    @Override
    public DeviceType getType() {
        return type;
    }

    @Override
    public String[] getTelemetryVariableNames() {
        return variableMap.keySet().toArray(new String[variableMap.size()]);
    }

    @Override
    public void onShutdown() {
    }

    @Override
    public void onSetVariable(String variableName, Object value) {
    }

    public void onDeviceData(Collection<VariableUpdate> data) {
        if (data.size() > 0) {
            fireVariableUpdateNotifications(new ArrayList<>(data));
        }
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", getContext().getDeviceId());
        json.put("name", getDefaultName());
        json.put("type", getType().toString());
        JSONArray vars = new JSONArray();
        for (MQTTDeviceVariable v : variableMap.values()) {
            JSONObject vjson = new JSONObject();
            vjson.put("name", v.name);
            vjson.put("mask", v.mask.toString());
            vars.put(vjson);
        }
        json.put("vars", vars);
        return json;
    }

    private class MQTTDeviceVariable {
        public String name;
        public HobsonVariable.Mask mask;
        public Object initialValue;

        public MQTTDeviceVariable(String name, HobsonVariable.Mask mask, Object initialValue) {
            this.name = name;
            this.mask = mask;
            this.initialValue = initialValue;
        }
    }
}

/*
 *******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************
*/
package com.whizzosoftware.hobson.mqtt.device;

import com.whizzosoftware.hobson.api.device.DeviceType;
import com.whizzosoftware.hobson.api.device.HobsonDeviceDescriptor;
import com.whizzosoftware.hobson.api.device.proxy.AbstractDeviceProxy;
import com.whizzosoftware.hobson.api.plugin.HobsonPlugin;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.api.variable.*;

import java.util.*;

public class MQTTDevice extends AbstractDeviceProxy {
    public static final String PROP_SECRET = "secret";
    public static final String PROP_ACTIVATED = "activated";

    private Map<String,MQTTDeviceVariable> variableMap = new HashMap<>();

    /**
     * Constructor.
     *
     * @param plugin the HobsonPlugin that created this device
     * @param id     the device ID
     */
    public MQTTDevice(HobsonPlugin plugin, String id, String name, DeviceType type) {
        super(plugin, id, name, type);
    }

    public MQTTDevice(HobsonPlugin plugin, HobsonDeviceDescriptor desc) {
        super(plugin, desc.getContext().getDeviceId(), desc.getName(), desc.getType());
        Collection<DeviceVariableDescriptor> variables = desc.getVariables();
        if (variables != null) {
            for (DeviceVariableDescriptor dvd : variables) {
                variableMap.put(dvd.getContext().getName(), new MQTTDeviceVariable(dvd.getContext().getName(), dvd.getMask(), null));
            }
        }
    }

    @Override
    protected TypedProperty[] createConfigurationPropertyTypes() {
        return new TypedProperty[] {
            new TypedProperty.Builder(PROP_SECRET, "Device Secret", "The device secret", TypedProperty.Type.SECURE_STRING).build(),
            new TypedProperty.Builder(PROP_ACTIVATED, "Activated", "Indicates if the device has been activated", TypedProperty.Type.BOOLEAN).build()
        };
    }

    @Override
    public String getManufacturerName() {
        return null;
    }

    @Override
    public String getManufacturerVersion() {
        return null;
    }

    @Override
    public String getModelName() {
        return null;
    }

    @Override
    public String getPreferredVariableName() {
        if (variableMap.containsKey(VariableConstants.ON)) {
            return VariableConstants.ON;
        }
        return null;
    }

    public boolean isActivated() {
        Boolean b = (Boolean)getConfigurationProperty(PROP_ACTIVATED);
        return (b != null && b);
    }

    public void onActivation(Map<String,Object> variables) {
        long now = System.currentTimeMillis();
        setConfigurationProperty(PROP_ACTIVATED, true);
        if (variables != null) {
            List<DeviceProxyVariable> vars = new ArrayList<>();
            for (String name : variables.keySet()) {
                vars.add(createDeviceVariable(name, VariableMask.READ_ONLY, variables.get(name), now));
            }
            if (vars.size() > 0) {
                publishVariables(vars);
            }
        }
    }

    @Override
    public void onDeviceConfigurationUpdate(PropertyContainer config) {

    }

    @Override
    public void onShutdown() {
    }

    @Override
    public void onSetVariable(String variableName, Object value) {
    }

    @Override
    public void onStartup(String name, PropertyContainer config) {
        // make sure device has a secret configured
        if (!config.hasPropertyValue(PROP_SECRET)) {
            setConfigurationProperty(PROP_SECRET, UUID.randomUUID().toString());
        }

        // publish any appropriate variables
        if (variableMap != null) {
            List<DeviceProxyVariable> vars = new ArrayList<>();
            for (MQTTDeviceVariable var : variableMap.values()) {
                vars.add(createDeviceVariable(var.name, var.mask, var.initialValue, var.initialValue != null ? System.currentTimeMillis() : null));
            }
            publishVariables(vars);
        }
    }

    public void onDeviceData(Collection<DeviceVariableState> data) {
        if (data.size() > 0) {
            Map<String,Object> values = new HashMap<>();
            for (DeviceVariableState s : data) {
                values.put(s.getContext().getName(), s.getValue());
            }
            setVariableValues(values);
        }
        setLastCheckin(System.currentTimeMillis());
    }

    private class MQTTDeviceVariable {
        public String name;
        public VariableMask mask;
        public Object initialValue;

        public MQTTDeviceVariable(String name, VariableMask mask, Object initialValue) {
            this.name = name;
            this.mask = mask;
            this.initialValue = initialValue;
        }
    }
}

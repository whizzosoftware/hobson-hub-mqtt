/*
 *******************************************************************************
 * Copyright (c) 2016 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************
*/
package com.whizzosoftware.hobson.mqtt.action;

import com.whizzosoftware.hobson.api.action.SingleAction;
import com.whizzosoftware.hobson.api.action.ActionProvider;
import com.whizzosoftware.hobson.api.property.PropertyConstraintType;
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.mqtt.MQTTPlugin;

import java.util.Map;

public class AddDeviceActionProvider extends ActionProvider {
    private MQTTPlugin plugin;

    public AddDeviceActionProvider(MQTTPlugin plugin) {
        super(
            PropertyContainerClassContext.create(plugin.getContext(), "addDevice"),
            "Add MQTT device",
            "Adds a new MQTT device",
            false,
            2000
        );
        this.plugin = plugin;
        addSupportedProperty(new TypedProperty.Builder("id", "Device ID", "The unique device identifier", TypedProperty.Type.STRING).
            constraint(PropertyConstraintType.required, true).
            build());
        addSupportedProperty(new TypedProperty.Builder("name", "Device name", "The name of the new device", TypedProperty.Type.STRING).
            constraint(PropertyConstraintType.required, true).
            build());
    }

    @Override
    public SingleAction createAction(final Map<String,Object> properties) {
        return new AddDeviceAction(plugin.getContext(), new AddDeviceActionContext() {
            @Override
            public void publishMQTTDevice(String id, String name) {
                plugin.publishMQTTDevice(id, name);
            }

            @Override
            public Map<String,Object> getProperties() {
                return properties;
            }
        }, plugin.getEventLoopExecutor());
    }
}

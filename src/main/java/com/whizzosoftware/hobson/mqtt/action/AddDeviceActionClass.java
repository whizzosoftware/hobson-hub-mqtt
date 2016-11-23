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

import com.whizzosoftware.hobson.api.action.ActionClass;
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.mqtt.MQTTPlugin;

public class AddDeviceActionClass extends ActionClass {
    public AddDeviceActionClass(MQTTPlugin plugin) {
        super(PropertyContainerClassContext.create(plugin.getContext(), "addDevice"), "Add device", "Add a new device", false, 2000);
        addSupportedProperty(new TypedProperty.Builder("id", "Device ID", "The unique device identifier", TypedProperty.Type.STRING).build());
        addSupportedProperty(new TypedProperty.Builder("name", "Device name", "The name of the new device", TypedProperty.Type.STRING).build());
    }
}

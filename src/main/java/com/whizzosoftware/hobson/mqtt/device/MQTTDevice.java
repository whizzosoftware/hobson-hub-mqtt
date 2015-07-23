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
import com.whizzosoftware.hobson.api.variable.VariableUpdate;
import com.whizzosoftware.hobson.mqtt.util.SmartObjectConverter;
import com.whizzosoftware.smartobjects.SmartObject;
import com.whizzosoftware.smartobjects.resource.Resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MQTTDevice extends AbstractHobsonDevice {
    private DeviceType type;
    private Collection<SmartObject> initialData;

    /**
     * Constructor.
     *
     * @param plugin the HobsonPlugin that created this device
     * @param id     the device ID
     */
    public MQTTDevice(HobsonPlugin plugin, String id, String name, DeviceType type, Collection<SmartObject> initialData) {
        super(plugin, id);

        setDefaultName(name);

        this.type = type;
        this.initialData = initialData;
    }

    @Override
    public void onStartup(PropertyContainer config) {
        for (SmartObject so : initialData) {
            String varName = SmartObjectConverter.getVariableNameForSmartObject(so);
            Resource res = SmartObjectConverter.getPrimaryValueForSmartObject(so);
            if (varName != null && res != null) {
                publishVariable(varName, res.getValue(), SmartObjectConverter.getMaskForResource(res));
            }
        }
    }

    @Override
    protected TypedProperty[] createSupportedProperties() {
        return new TypedProperty[0];
    }

    @Override
    public DeviceType getType() {
        return type;
    }

    @Override
    public void onShutdown() {

    }

    @Override
    public void onSetVariable(String variableName, Object value) {
    }

    public void onDeviceData(Collection<SmartObject> data) {
        List<VariableUpdate> updates = new ArrayList<>();

        for (SmartObject so : data) {
            String name = SmartObjectConverter.getVariableNameForSmartObject(so);
            Resource r = SmartObjectConverter.getPrimaryValueForSmartObject(so);
            if (name != null && r != null) {
                updates.add(new VariableUpdate(getContext(), name, r.getValue()));
            }
        }

        if (updates.size() > 0) {
            fireVariableUpdateNotifications(updates);
        }
    }
}

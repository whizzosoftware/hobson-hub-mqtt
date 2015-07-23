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
import com.whizzosoftware.hobson.api.variable.VariableConstants;
import com.whizzosoftware.hobson.api.variable.VariableUpdate;
import com.whizzosoftware.smartobjects.object.SmartObject;
import com.whizzosoftware.smartobjects.object.impl.DigitalOutput;
import com.whizzosoftware.smartobjects.object.impl.Humidity;
import com.whizzosoftware.smartobjects.object.impl.Illuminance;
import com.whizzosoftware.smartobjects.object.impl.Temperature;
import com.whizzosoftware.smartobjects.resource.Resource;
import com.whizzosoftware.smartobjects.resource.ResourceConstants;

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
            String varName = getVariableNameForSmartObject(so);
            Resource res = getPrimaryValueForSmartObject(so);
            if (varName != null && res != null) {
                publishVariable(varName, res.getValue(), getMaskForResource(res));
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
            String name = getVariableNameForSmartObject(so);
            Resource r = getPrimaryValueForSmartObject(so);
            if (name != null && r != null) {
                updates.add(new VariableUpdate(getContext(), name, r.getValue()));
            }
        }

        if (updates.size() > 0) {
            fireVariableUpdateNotifications(updates);
        }
    }

    protected String getVariableNameForSmartObject(SmartObject so) {
        switch (so.getId()) {
            case DigitalOutput.ID:
                return VariableConstants.ON;
            case Temperature.ID: {
                String vc = VariableConstants.TEMP_F;
                Resource ur = so.getResource(ResourceConstants.Units, 0);
                if (ur != null) {
                    vc = "cel".equalsIgnoreCase(ur.getValue().toString()) ? VariableConstants.TEMP_C : VariableConstants.TEMP_F;
                }
                return vc;
            }
            case Humidity.ID:
                return VariableConstants.HUMIDITY_PERCENT;
            case Illuminance.ID:
                return VariableConstants.LX_LUX;
            default:
                return null;
        }
    }

    protected Resource getPrimaryValueForSmartObject(SmartObject so) {
        switch (so.getId()) {
            case DigitalOutput.ID:
                return so.getResource(ResourceConstants.DigitalOutputState, 0);
            case Temperature.ID:
                return so.getResource(ResourceConstants.SensorValue, 0);
            case Humidity.ID:
                return so.getResource(ResourceConstants.SensorValue, 0);
            case Illuminance.ID:
                return so.getResource(ResourceConstants.SensorValue, 0);
            default:
                return null;
        }
    }

    protected HobsonVariable.Mask getMaskForResource(Resource r) {
        switch (r.getResourceClass().getAccessType()) {
            case ReadOnly:
                return HobsonVariable.Mask.READ_ONLY;
            case ReadWrite:
                return HobsonVariable.Mask.READ_WRITE;
            case Event:
                return HobsonVariable.Mask.WRITE_ONLY;
            default:
                return null;
        }
    }
}

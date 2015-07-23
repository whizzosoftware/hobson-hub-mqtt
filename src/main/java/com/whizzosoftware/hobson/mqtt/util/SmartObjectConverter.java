/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.mqtt.util;

import com.whizzosoftware.hobson.api.variable.HobsonVariable;
import com.whizzosoftware.hobson.api.variable.VariableConstants;
import com.whizzosoftware.smartobjects.SmartObject;
import com.whizzosoftware.smartobjects.UCUMCode;
import com.whizzosoftware.smartobjects.impl.DigitalOutput;
import com.whizzosoftware.smartobjects.impl.Humidity;
import com.whizzosoftware.smartobjects.impl.Illuminance;
import com.whizzosoftware.smartobjects.impl.Temperature;
import com.whizzosoftware.smartobjects.resource.Resource;
import com.whizzosoftware.smartobjects.resource.ResourceConstants;

/**
 * A class of convenience methods for converting SmartObject data to/from Hobson data.
 *
 * @author Dan Noguerol
 */
public class SmartObjectConverter {
    /**
     * Returns the Hobson variable name for a SmartObject.
     *
     * @param so the smart object
     * @return the variable name (or null if there is no correlation)
     */
    static public String getVariableNameForSmartObject(SmartObject so) {
        switch (so.getId()) {
            case DigitalOutput.ID:
                return VariableConstants.ON;
            case Temperature.ID: {
                String vc = VariableConstants.TEMP_F;
                Resource ur = so.getResource(ResourceConstants.Units, 0);
                if (ur != null) {
                    vc = UCUMCode.DegreeCelsius.equals(ur.getValue()) ? VariableConstants.TEMP_C : VariableConstants.TEMP_F;
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

    /**
     * Returns the "primary" value for a SmartObject -- this is the value that maps directly to a Hobson variable.
     *
     * @param so the smart object
     * @return the value (or null if there is no correlation)
     */
    static public Resource getPrimaryValueForSmartObject(SmartObject so) {
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

    /**
     * Converts a SmartObject resource mask to a Hobson variable mask.
     *
     * @param r the resource
     * @return the variable mask (or null if there's no correlation)
     */
    static public HobsonVariable.Mask getMaskForResource(Resource r) {
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

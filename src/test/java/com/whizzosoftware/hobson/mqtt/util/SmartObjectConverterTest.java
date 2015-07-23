/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.mqtt.util;

import com.whizzosoftware.hobson.api.variable.VariableConstants;
import com.whizzosoftware.smartobjects.impl.DigitalOutput;
import com.whizzosoftware.smartobjects.impl.Humidity;
import com.whizzosoftware.smartobjects.impl.Illuminance;
import com.whizzosoftware.smartobjects.impl.Temperature;
import com.whizzosoftware.smartobjects.resource.ResourceConstants;
import org.junit.Test;
import static org.junit.Assert.*;

public class SmartObjectConverterTest {
    @Test
    public void testGetVariableNameForSmartObject() {
        // digital output
        assertEquals(VariableConstants.ON, SmartObjectConverter.getVariableNameForSmartObject(new DigitalOutput(0)));

        // default fahrenheit
        assertEquals(VariableConstants.TEMP_F, SmartObjectConverter.getVariableNameForSmartObject(new Temperature(0)));

        // explicit fahrenheit
        Temperature t = new Temperature(0);
        t.setResourceValue(ResourceConstants.Units, 0, "Far");
        assertEquals(VariableConstants.TEMP_F, SmartObjectConverter.getVariableNameForSmartObject(t));

        // celsius temperature
        t = new Temperature(0);
        t.setResourceValue(ResourceConstants.Units, 0, "Cel");
        assertEquals(VariableConstants.TEMP_C, SmartObjectConverter.getVariableNameForSmartObject(t));

        // humidity
        assertEquals(VariableConstants.HUMIDITY_PERCENT, SmartObjectConverter.getVariableNameForSmartObject(new Humidity(0)));

        // illuminance
        assertEquals(VariableConstants.LX_LUX, SmartObjectConverter.getVariableNameForSmartObject(new Illuminance(0)));
    }
}

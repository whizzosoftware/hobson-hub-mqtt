/*
 *******************************************************************************
 * Copyright (c) 2017 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************
*/
package com.whizzosoftware.hobson.mqtt;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MQTTPluginTest {
    @Test
    public void testFormatClientBrokerUrl() {
        MQTTPlugin plugin = new MQTTPlugin("plugin", "version", "description");
        assertNull(plugin.formatClientBrokerUrl(null));
        assertEquals("tcp://test.mosquitto.org:1883", plugin.formatClientBrokerUrl("test.mosquitto.org"));
        assertEquals("tcp://test.mosquitto.org:1884", plugin.formatClientBrokerUrl("test.mosquitto.org:1884"));
        assertEquals("scp://test.mosquitto.org:1883", plugin.formatClientBrokerUrl("scp://test.mosquitto.org"));
        assertEquals("scp://test.mosquitto.org:1884", plugin.formatClientBrokerUrl("scp://test.mosquitto.org:1884"));
    }
}

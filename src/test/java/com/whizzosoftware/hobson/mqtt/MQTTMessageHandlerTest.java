/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.mqtt;

import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.variable.DeviceVariableState;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class MQTTMessageHandlerTest {
    @Test
    public void testRegisterMessage() {
        MockMQTTMessageSink sink = new MockMQTTMessageSink();
        MockMQTTEventDelegate listener = new MockMQTTEventDelegate();
        MQTTMessageHandler handler = new MQTTMessageHandler(PluginContext.createLocal("plugin1"), sink, listener);

        assertEquals(0, sink.getMessageCount());
        assertEquals(0, listener.getEventCount());

        JSONObject json = new JSONObject(new JSONTokener("{\"deviceId\":\"3b2184be-2f2c-11e5-a151-feff819cdc9f\",\"nonce\":\"foo\",\"name\":\"Sensor 1\",\"data\":{\"on\":false}}"));
        handler.onMessage("bootstrap", json);

        assertEquals(1, sink.getMessageCount());
        assertEquals("bootstrap/3b2184be-2f2c-11e5-a151-feff819cdc9f/foo", sink.getMessage(0).topic);
        assertNotNull(sink.getMessage(0).payload);
        assertTrue(sink.getMessage(0).payload.getString("secret") != null);
        assertTrue(sink.getMessage(0).payload.has("topics"));

        String dataTopic = sink.getMessage(0).payload.getJSONObject("topics").getString("data");
        assertTrue(dataTopic.startsWith("device/") && dataTopic.endsWith("/data"));
        String commandTopic = sink.getMessage(0).payload.getJSONObject("topics").getString("command");
        assertTrue(commandTopic.startsWith("device/") && commandTopic.endsWith("/command"));

        assertEquals(1, listener.getEventCount());
        assertEquals("3b2184be-2f2c-11e5-a151-feff819cdc9f", listener.getDeviceRegistrationIds().iterator().next());
    }

    @Test
    public void testDataMessage() {
        MockMQTTMessageSink sink = new MockMQTTMessageSink();
        MockMQTTEventDelegate listener = new MockMQTTEventDelegate();
        MQTTMessageHandler handler = new MQTTMessageHandler(PluginContext.createLocal("plugin1"), sink, listener);

        assertEquals(0, sink.getMessageCount());
        assertEquals(0, listener.getEventCount());

        // send registration message
        JSONObject json = new JSONObject(new JSONTokener("{\"deviceId\":\"sensor1\",\"nonce\":\"foo\",\"data\":{}}"));
        handler.onMessage("bootstrap", json);
        assertEquals(1, listener.getEventCount());

        // get the data topic from the registration response
        json = sink.getMessage(0).payload;
        String dataTopic = json.getJSONObject("topics").getString("data");
        int ix = dataTopic.indexOf('/') + 1;
        sink.clear();

        // send data message
        json = new JSONObject(new JSONTokener("{\"inTempF\":72.5,\"inRh\":30.1}"));
        handler.onMessage(dataTopic, json);

        assertEquals(2, listener.getEventCount());

        assertNotNull(listener.getData("sensor1"));
        assertEquals(2, listener.getData("sensor1").size());

        Iterator<DeviceVariableState> it = listener.getData("sensor1").iterator();
        DeviceVariableState so1 = it.next();
        DeviceVariableState so2 = it.next();
        assertTrue(so1.getContext().getName().equals("inTempF") || so1.getContext().getName().equals("inRh"));
        assertTrue(so2.getContext().getName().equals("inTempF") || so2.getContext().getName().equals("inRh"));
    }
}

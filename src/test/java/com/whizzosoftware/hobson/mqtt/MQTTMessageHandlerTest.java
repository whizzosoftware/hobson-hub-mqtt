package com.whizzosoftware.hobson.mqtt;

import com.whizzosoftware.hobson.ipsoso.object.SmartObject;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class MQTTMessageHandlerTest {
    @Test
    public void testRegisterMessage() {
        MockMQTTMessageSink sink = new MockMQTTMessageSink();
        MockMQTTEventListener listener = new MockMQTTEventListener();
        MQTTMessageHandler handler = new MQTTMessageHandler(sink, listener);

        assertEquals(0, sink.getMessageCount());
        assertEquals(0, listener.getEventCount());

        handler.onMessage("devices/register", new JSONObject(new JSONTokener("{\"id\":\"3b2184be-2f2c-11e5-a151-feff819cdc9f\",\"name\":\"Sensor 1\",\"data\":{\"3201.0\":{\"5550.0\":\"true\"}}}")));

        assertEquals(1, sink.getMessageCount());
        assertEquals("devices/3b2184be-2f2c-11e5-a151-feff819cdc9f/registrations", sink.getMessage(0).topic);
        assertNotNull(sink.getMessage(0).payload);
        assertEquals("30", sink.getMessage(0).payload.getString("interval"));
        assertTrue(sink.getMessage(0).payload.has("topics"));
        assertEquals("devices/3b2184be-2f2c-11e5-a151-feff819cdc9f/data", sink.getMessage(0).payload.getJSONObject("topics").getString("data"));

        assertEquals(1, listener.getEventCount());
        assertEquals("3b2184be-2f2c-11e5-a151-feff819cdc9f", listener.getDeviceRegistrationIds().iterator().next());
        assertEquals("Sensor 1", listener.getDeviceRegistrationName("3b2184be-2f2c-11e5-a151-feff819cdc9f"));
    }

    @Test
    public void testDataMessage() {
        MockMQTTMessageSink sink = new MockMQTTMessageSink();
        MockMQTTEventListener listener = new MockMQTTEventListener();
        MQTTMessageHandler handler = new MQTTMessageHandler(sink, listener);

        assertEquals(0, sink.getMessageCount());
        assertEquals(0, listener.getEventCount());

        handler.onMessage("devices/3b2184be-2f2c-11e5-a151-feff819cdc9f/data", new JSONObject(new JSONTokener("{\"3303.0\":{\"5700.0\":\"72.5\",\"5701.0\":\"Far\"},\"3304.0\":{\"5700.0\":\"30.1\",\"5701.0\":\"%\"}}")));

        assertEquals(1, listener.getEventCount());

        assertNotNull(listener.getData("3b2184be-2f2c-11e5-a151-feff819cdc9f"));
        assertEquals(2, listener.getData("3b2184be-2f2c-11e5-a151-feff819cdc9f").size());

        Iterator<SmartObject> it = listener.getData("3b2184be-2f2c-11e5-a151-feff819cdc9f").iterator();
        SmartObject so1 = it.next();
        SmartObject so2 = it.next();
        assertTrue(so1.getId() == 3303 || so2.getId() == 3304);
    }
}

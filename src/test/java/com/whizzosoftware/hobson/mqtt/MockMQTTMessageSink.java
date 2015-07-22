package com.whizzosoftware.hobson.mqtt;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MockMQTTMessageSink implements MQTTMessageSink {
    private List<Message> messages = new ArrayList<>();

    @Override
    public void sendMessage(String topic, JSONObject payload) {
        messages.add(new Message(topic, payload));
    }

    public int getMessageCount() {
        return messages.size();
    }

    public Message getMessage(int ix) {
        return messages.get(ix);
    }

    public class Message {
        public String topic;
        public JSONObject payload;

        public Message(String topic, JSONObject payload) {
            this.topic = topic;
            this.payload = payload;
        }
    }
}

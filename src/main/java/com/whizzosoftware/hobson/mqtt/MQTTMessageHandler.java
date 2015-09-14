/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.mqtt;

import com.whizzosoftware.hobson.api.device.DeviceBootstrap;
import com.whizzosoftware.smartobjects.json.JSONHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Class responsible for processing incoming MQTT messages. Examples are device registration and device data
 * submission messages.
 *
 * @author Dan Noguerol
 */
public class MQTTMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(MQTTMessageHandler.class);

    private MQTTMessageSink sink;
    private MQTTEventDelegate delegate;
    private Pattern deviceDataTopicPattern = Pattern.compile("^device/.*/data$");
    private JSONHelper smartObjectHelper = new JSONHelper();

    /**
     * Constructor.
     *
     * @param sink the transport to use for sending response messages
     * @param delegate the listener to notify of events generated by incoming messages
     */
    public MQTTMessageHandler(MQTTMessageSink sink, MQTTEventDelegate delegate) {
        this.sink = sink;
        this.delegate = delegate;
    }

    /**
     * Called when an MQTT message is received.
     *
     * @param topic the topic the message was received on
     * @param json the JSON payload of the message
     */
    public void onMessage(String topic, JSONObject json) {
        logger.trace("Received MQTT message on topic {}: {}", topic, json);

        if ("bootstrap".equals(topic)) {
            if (json.has("deviceId") && json.has("nonce")) {
                String deviceId = json.getString("deviceId");
                String nonce = json.getString("nonce");

                DeviceBootstrap bootstrap = delegate.registerDeviceBootstrap(deviceId);
                JSONObject res = new JSONObject();

                if (bootstrap != null) {
                    // alert listener of the device registration
                    delegate.onBootstrapRegistration(deviceId, json.has("name") ? json.getString("name") : "Unknown MQTT Device", json.has("data") ? smartObjectHelper.createObjectCollection(json.getJSONObject("data")) : null);

                    // create JSON response message
                    res.put("secret", bootstrap.getSecret());
                    JSONObject topics = new JSONObject();
                    topics.put("data", "device/" + bootstrap.getId() + "/data");
                    topics.put("command", "device/" + bootstrap.getId() + "/command");
                    res.put("topics", topics);
                } else {
                    res.put("error", "Unable to bootstrap device");
                }

                // send response message
                String responseTopic = "bootstrap/" + deviceId + "/" + nonce;
                logger.debug("Registration message detected from {}; sending response on topic {}", deviceId, responseTopic);
                sink.sendMessage(responseTopic, res);
            } else {
                logger.error("Device registration missing device ID or nonce");
            }
        } else if (deviceDataTopicPattern.matcher(topic).matches()) {
            try {
                // alert listener of received data
                int ix = topic.indexOf('/') + 1;
                DeviceBootstrap deviceBootstrap = delegate.getDeviceBootstrap(topic.substring(ix, topic.indexOf('/', ix)));
                if (deviceBootstrap != null) {
                    delegate.onDeviceData(deviceBootstrap.getDeviceId(), smartObjectHelper.createObjectCollection(json));
                } else {
                    logger.error("Received data from device with invalid bootstrap identifier");
                }
            } catch (JSONException e) {
                logger.error("Error parsing device data JSON", e);
            }
        }
    }
}

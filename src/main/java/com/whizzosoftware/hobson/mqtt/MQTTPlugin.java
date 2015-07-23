/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.mqtt;

import com.whizzosoftware.hobson.api.device.DeviceContext;
import com.whizzosoftware.hobson.api.device.DeviceType;
import com.whizzosoftware.hobson.api.device.HobsonDevice;
import com.whizzosoftware.hobson.api.plugin.AbstractHobsonPlugin;
import com.whizzosoftware.hobson.api.plugin.PluginStatus;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.mqtt.device.MQTTDevice;
import com.whizzosoftware.smartobjects.SmartObject;
import org.eclipse.moquette.server.Server;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

/**
 * The MQTT plugin. This creates an embedded MQTT broker to proxy MQTT events as Hobson ones.
 *
 * @author Dan Noguerol
 */
public class MQTTPlugin extends AbstractHobsonPlugin implements MqttCallback, MQTTMessageSink, MQTTEventListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Server server;
    private final MqttConnectOptions connOpts;
    private MqttAsyncClient mqtt;
    private boolean isConnectPending;
    private boolean connected;
    private MQTTMessageHandler handler;

    public MQTTPlugin(String pluginId) {
        super(pluginId);

        handler = new MQTTMessageHandler(this, this);

        // create MQTT connection options
        connOpts = new MqttConnectOptions();
        connOpts.setConnectionTimeout(10);
        connOpts.setCleanSession(true);
        connOpts.setKeepAliveInterval(30);
    }

    // ***
    // HobsonPlugin methods
    // ***

    @Override
    public void onStartup(PropertyContainer config) {
        try {
            server = new Server();
            server.startServer();
            logger.info("MQTT broker has started");

            connect();

            setStatus(PluginStatus.running());
        } catch (IOException e) {
            logger.error("Error starting MQTT broker", e);
            setStatus(PluginStatus.failed("Unable to start MQTT broker"));
        }
    }

    @Override
    public void onShutdown() {
        super.onShutdown();

        if (server != null) {
            server.stopServer();
        }
    }

    @Override
    protected TypedProperty[] createSupportedProperties() {
        return null;
    }

    @Override
    public String getName() {
        return "MQTT Plugin";
    }

    @Override
    public void onPluginConfigurationUpdate(PropertyContainer config) {
    }

    @Override
    public void onDeviceRegistration(String id, String name, Collection<SmartObject> initialData) {
        publishDevice(new MQTTDevice(this, id, name, DeviceType.SENSOR, initialData));
    }

    @Override
    public void onDeviceData(final String id, final Collection<SmartObject> objects) {
        logger.info("Received data from device " + id + ": " + objects);

        DeviceContext ctx = DeviceContext.create(getContext(), id);

        HobsonDevice device = getDevice(ctx);
        if (device != null) {
            if (device instanceof MQTTDevice) {
                ((MQTTDevice)getDevice(ctx)).onDeviceData(objects);
            } else {
                logger.error("Received data for non-MQTT device: " + ctx);
            }
        } else {
            logger.error("Received data for unknown device: " + ctx);
        }
    }

    protected void connect() {
        try {
            String brokerUrl = "tcp://localhost:1883";

            if (mqtt == null) {
                mqtt = new MqttAsyncClient(brokerUrl, "Hobson Hub", new MemoryPersistence());
                mqtt.setCallback(this);
            }
            logger.info("Attempting broker connection to {} with user {}", brokerUrl, connOpts.getUserName());
            isConnectPending = true;
            mqtt.connect(connOpts, "", new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken token) {
                    logger.info("Broker connection successful");
                    isConnectPending = false;
                    connected = true;

                    try {
                        mqtt.subscribe("devices/#", 0, null, new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken iMqttToken) {
                                logger.info("Devices subscription successful");
                            }

                            @Override
                            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                                logger.error("Devices subscription failed", throwable);
                            }
                        });
                    } catch (MqttException e) {
                        logger.error("Unable to subscribe to devices topic");
                    }
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable t) {
                    logger.error("Broker connection failure", t);
                    isConnectPending = false;
                }
            });
        } catch (Throwable e) {
            logger.error("Broker connection failure", e);
            isConnectPending = false;
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {
        logger.error("Lost connection to broker", throwable);
        connected = false;
    }

    @Override
    public void messageArrived(final String topic, final MqttMessage mqttMessage) throws Exception {
        try {
            logger.info("Message arrived on topic " + topic + ": " + new String(mqttMessage.getPayload()));

            final JSONObject json = new JSONObject(new JSONTokener(new String(mqttMessage.getPayload())));

            executeInEventLoop(new Runnable() {
                @Override
                public void run() {
                    handler.onMessage(topic, json);
                }
            });
        } catch (JSONException e) {
            logger.error("Received invalid JSON on topic: " + topic);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        logger.info("deliveryComplete: " + iMqttDeliveryToken);
    }

    @Override
    public void sendMessage(final String topic, final JSONObject payload) {
        executeInEventLoop(new Runnable() {
            @Override
            public void run() {
                try {
                    mqtt.publish(topic, payload.toString().getBytes(), 0, false, null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken iMqttToken) {
                            logger.debug("MQTT message sent successfully");
                        }

                        @Override
                        public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                            logger.error("Failed to send MQTT message", throwable);
                        }
                    });
                } catch (MqttException e) {
                    logger.error("Failed to send MQTT message", e);
                }
            }
        });
    }
}

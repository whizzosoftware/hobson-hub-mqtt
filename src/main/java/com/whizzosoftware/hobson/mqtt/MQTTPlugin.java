/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.mqtt;

import com.whizzosoftware.hobson.api.device.DeviceBootstrap;
import com.whizzosoftware.hobson.api.device.DeviceContext;
import com.whizzosoftware.hobson.api.device.DeviceType;
import com.whizzosoftware.hobson.api.device.HobsonDevice;
import com.whizzosoftware.hobson.api.disco.DeviceAdvertisement;
import com.whizzosoftware.hobson.api.hub.HubContext;
import com.whizzosoftware.hobson.api.plugin.AbstractHobsonPlugin;
import com.whizzosoftware.hobson.api.plugin.PluginStatus;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.mqtt.device.MQTTDevice;
import com.whizzosoftware.smartobjects.SmartObject;
import org.eclipse.moquette.commons.Constants;
import org.eclipse.moquette.server.Server;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ConcurrentNavigableMap;

/**
 * The MQTT plugin. This creates an embedded MQTT broker to proxy MQTT events to Hobson.
 *
 * @author Dan Noguerol
 */
public class MQTTPlugin extends AbstractHobsonPlugin implements MqttCallback, MQTTMessageSink, MQTTEventDelegate {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static String PROP_BROKER_URL = "brokerUrl";
    private final static String DEFAULT_BROKER = "tcp://localhost:1883";

    private DB db;
    private Server server;
    private final MqttConnectOptions connOpts;
    private String brokerUrl;
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
            // create the internal device database
            db = DBMaker.newFileDB(getDataFile("devices")).closeOnJvmShutdown().make();

            // restore any previously known devices
            restoreDevices();

            // start the embedded broker
            Properties mqttConfig = new Properties();
            mqttConfig.put(Constants.PERSISTENT_STORE_PROPERTY_NAME, getDataFile("moquette_store.mapdb").getAbsolutePath());

            server = new Server();
            server.startServer(mqttConfig);
            logger.debug("MQTT broker has started");

            // publish an SSDP device advertisement for the MQTT broker
            getDiscoManager().publishDeviceAdvertisement(getContext().getHubContext(), new DeviceAdvertisement.Builder("urn:hobson:mqtt", "ssdp").uri("tcp://localhost:1889").build(), true);

            // get the client broker URL
            brokerUrl = config.getStringPropertyValue(PROP_BROKER_URL, DEFAULT_BROKER);

            // perform client connection to embedded broker
            connect();

            setStatus(PluginStatus.running());
        } catch (IOException e) {
            logger.error("Error starting MQTT broker", e);
            setStatus(PluginStatus.failed("Unable to start MQTT broker"));
        }
    }

    @Override
    public void onShutdown() {
        disconnect();

        if (server != null) {
            server.stopServer();
        }

        if (!db.isClosed()) {
            db.close();
        }
    }

    @Override
    protected TypedProperty[] createSupportedProperties() {
        return new TypedProperty[] {
            new TypedProperty(PROP_BROKER_URL, "Broker URL", "The MQTT broker to connect to (defaults to tcp://localhost:1883).", TypedProperty.Type.STRING)
        };
    }

    @Override
    public String getName() {
        return "MQTT Plugin";
    }

    @Override
    public long getRefreshInterval() {
        return 5; // the client connection watchdog will run every 5 seconds
    }

    @Override
    public void onPluginConfigurationUpdate(PropertyContainer config) {
        String s = config.getStringPropertyValue(PROP_BROKER_URL, DEFAULT_BROKER);
        if (!s.equals(brokerUrl)) {
            logger.debug("MQTT broker URL has changed");
            brokerUrl = s;
            disconnect();
        }
    }

    @Override
    public DeviceBootstrap registerDeviceBootstrap(String deviceId) {
        return getDeviceManager().registerDeviceBootstrap(HubContext.createLocal(), deviceId);
    }

    @Override
    public DeviceBootstrap getDeviceBootstrap(String bootstrapId) {
        return getDeviceManager().getDeviceBootstrap(getContext().getHubContext(), bootstrapId);
    }

    @Override
    public void onBootstrapRegistration(String id, String name, Collection<SmartObject> initialData) {
        registerDevice(id, name, initialData);
    }

    @Override
    public void onDeviceData(final String id, final Collection<SmartObject> objects) {
        logger.trace("Received data from device " + id + ": " + objects);

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

    @Override
    public void onRefresh() {
        // if there's no connection and no pending one, attempt a new one
        if (!connected && !isConnectPending) {
            connect();
        }
    }

    protected void registerDevice(String id, String name, Collection<SmartObject> initialData) {
        DeviceContext ctx = DeviceContext.create(getContext(), id);
        if (!hasDevice(ctx)) {
            MQTTDevice device = new MQTTDevice(this, id, name, DeviceType.SENSOR, initialData);
            publishDevice(device);

            ConcurrentNavigableMap<String,String> devices = db.getTreeMap("devices");
            devices.put(id, device.toJSON().toString());
            db.commit();
        }
    }

    protected void restoreDevices() {
        ConcurrentNavigableMap<String,String> devices = db.getTreeMap("devices");
        for (String id : devices.keySet()) {
            JSONObject json = new JSONObject(new JSONTokener(devices.get(id)));
            publishDevice(new MQTTDevice(this, json));
        }
    }

    protected void connect() {
        try {
            if (mqtt == null) {
                mqtt = new MqttAsyncClient(brokerUrl, "Hobson Hub", new MemoryPersistence());
                mqtt.setCallback(this);
            }
            logger.debug("Attempting broker connection to {} with user {}", brokerUrl, connOpts.getUserName());
            isConnectPending = true;
            mqtt.connect(connOpts, "", new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken token) {
                    logger.info("Connected to MQTT broker at " + brokerUrl);
                    isConnectPending = false;
                    connected = true;

                    try {
                        mqtt.subscribe("bootstrap/#", 0, null, new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken iMqttToken) {
                                logger.debug("Bootstrap subscription successful");
                            }

                            @Override
                            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                                logger.error("Bootstrap subscription failed");
                                disconnect();
                            }
                        });
                        mqtt.subscribe("device/#", 0, null, new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken iMqttToken) {
                                logger.debug("Device subscription successful");
                            }

                            @Override
                            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                                logger.error("Devices subscription failed", throwable);
                                disconnect();
                            }
                        });
                    } catch (MqttException e) {
                        logger.error("Unable to subscribe to devices topic");
                        disconnect();
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

    protected void disconnect() {
        try {
            mqtt.disconnect();
        } catch (MqttException e) {
            logger.error("Error disconnecting from broker", e);
        } finally {
            isConnectPending = false;
            connected = false;
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
            logger.trace("Message arrived on topic " + topic + ": " + new String(mqttMessage.getPayload()));

            final JSONObject json = new JSONObject(new JSONTokener(new String(mqttMessage.getPayload())));

            executeInEventLoop(new Runnable() {
                @Override
                public void run() {
                    try {
                        handler.onMessage(topic, json);
                    } catch (Throwable t) {
                        logger.error("Error processing MQTT message from topic " + topic + ": " + json, t);
                    }
                }
            });
        } catch (JSONException e) {
            logger.error("Received invalid JSON on topic: " + topic);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        logger.trace("deliveryComplete: {}", iMqttDeliveryToken);
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

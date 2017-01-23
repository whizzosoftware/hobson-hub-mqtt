/*
 *******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************
*/
package com.whizzosoftware.hobson.mqtt;

import com.whizzosoftware.hobson.api.HobsonNotFoundException;
import com.whizzosoftware.hobson.api.device.DeviceType;
import com.whizzosoftware.hobson.api.device.HobsonDeviceDescriptor;
import com.whizzosoftware.hobson.api.device.proxy.HobsonDeviceProxy;
import com.whizzosoftware.hobson.api.disco.DeviceAdvertisement;
import com.whizzosoftware.hobson.api.hub.NetworkInfo;
import com.whizzosoftware.hobson.api.plugin.AbstractHobsonPlugin;
import com.whizzosoftware.hobson.api.plugin.PluginStatus;
import com.whizzosoftware.hobson.api.property.PropertyConstraintType;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.api.variable.DeviceVariableState;
import com.whizzosoftware.hobson.mqtt.action.AddDeviceActionProvider;
import com.whizzosoftware.hobson.mqtt.device.MQTTDevice;
import io.moquette.BrokerConstants;
import io.moquette.server.Server;
import io.moquette.server.config.MemoryConfig;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * The MQTT plugin. This creates an embedded MQTT broker to proxy MQTT events to Hobson.
 *
 * @author Dan Noguerol
 */
public class MQTTPlugin extends AbstractHobsonPlugin implements MqttCallback, MQTTMessageSink, MQTTEventDelegate, MQTTSecretProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static int DEFAULT_PORT = 1883;
    private final static String PROP_BROKER_URL = "brokerUrl";
    private final static String DEFAULT_CLIENT_BROKER = "tcp://localhost:"  + DEFAULT_PORT;

    private Server server;
    private final MqttConnectOptions connOpts;
    private boolean embeddedBroker;
    private String clientBrokerUrl;
    private String clientAdminUser;
    private String clientAdminPassword;
    private MqttAsyncClient mqtt;
    private boolean isConnectPending;
    private boolean connected;
    private MQTTMessageHandler handler;

    public MQTTPlugin(String pluginId, String version, String description) {
        super(pluginId, version, description);

        handler = new MQTTMessageHandler(getContext(), this, this);

        // create ephemeral admin credentials
        clientAdminUser = UUID.randomUUID().toString();
        clientAdminPassword = UUID.randomUUID().toString();

        // create MQTT connection options
        connOpts = new MqttConnectOptions();
        connOpts.setConnectionTimeout(10);
        connOpts.setCleanSession(true);
        connOpts.setKeepAliveInterval(30);
        connOpts.setUserName(clientAdminUser);
        connOpts.setPassword(clientAdminPassword.toCharArray());
    }

    // ***
    // HobsonPlugin methods
    // ***

    @Override
    public void onStartup(PropertyContainer config) {
        try {
            // get the client broker URL
            clientBrokerUrl = config.getStringPropertyValue(PROP_BROKER_URL, DEFAULT_CLIENT_BROKER);

            // restore any previously known devices
            restoreDevices();

            // start the embedded broker if needed
            prepareBroker(clientBrokerUrl);

            // perform client connection to embedded broker
            connect();

            // publish action provider for adding devices
            publishActionProvider(new AddDeviceActionProvider(this));

            // plugin is now running
            setStatus(PluginStatus.running());
        } catch (IOException e) {
            logger.error("Error starting MQTT broker", e);
            setStatus(PluginStatus.failed("Unable to start MQTT broker"));
        }
    }

    private void prepareBroker(String brokerUrl) throws IOException {
        embeddedBroker = brokerUrl.equals(DEFAULT_CLIENT_BROKER);
        if (embeddedBroker && server == null) {
            logger.info("Starting embedded MQTT broker");
            Properties mqttConfig = new Properties();
            mqttConfig.put(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME, getDataFile("moquette_store.mapdb").getAbsolutePath());
            mqttConfig.put(BrokerConstants.PORT, DEFAULT_PORT);
            server = new Server();
            server.startServer(new MemoryConfig(mqttConfig), null, null, new MQTTAuthenticator(this, clientAdminUser, clientAdminPassword), new MQTTAuthorizator(clientAdminUser));
            logger.debug("MQTT broker has started");

            // publish an SSDP device advertisement for the MQTT broker
            try {
                NetworkInfo ni = getHubManager().getLocalManager().getNetworkInfo();
                String url = "tcp://" + ni.getInetAddress().getHostAddress() + ":" + DEFAULT_PORT;
                getDiscoManager().publishDeviceAdvertisement(getContext().getHubContext(), new DeviceAdvertisement.Builder("urn:hobson:mqtt", "ssdp").uri(url).build(), true);
            } catch (IOException e) {
                logger.error("Unable to determine IP address; will not publish advertisements", e);
            }
        } else if (!embeddedBroker && server != null) {
            logger.info("Stopping embedded MQTT broker");
            server.stopServer();
            server = null;
        }
    }

    @Override
    public void onShutdown() {
        disconnect();

        if (server != null) {
            server.stopServer();
        }
    }

    @Override
    protected TypedProperty[] getConfigurationPropertyTypes() {
        return new TypedProperty[] {
            new TypedProperty.Builder(PROP_BROKER_URL, "Broker URL", "The MQTT broker to connect to (defaults to tcp://localhost:1884).", TypedProperty.Type.STRING).
                constraint(PropertyConstraintType.required, true).
                build()
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
        String s = config.getStringPropertyValue(PROP_BROKER_URL, DEFAULT_CLIENT_BROKER);
        if (!s.equals(clientBrokerUrl)) {
            logger.debug("MQTT broker URL has changed");
            embeddedBroker = s.equals(DEFAULT_CLIENT_BROKER);
            clientBrokerUrl = s;
            disconnect();
            try {
                prepareBroker(s);
            } catch (IOException e) {
                logger.error("Error starting MQTT broker", e);
            }
        }
    }

    @Override
    public void onDeviceData(final String id, final Collection<DeviceVariableState> objects) {
        logger.trace("Received data from device " + id + ": " + objects);

        HobsonDeviceProxy device = getDeviceProxy(id);
        if (device instanceof MQTTDevice) {
            ((MQTTDevice)device).onDeviceData(objects);
        } else {
            logger.error("Received data for non-MQTT device: " + device.getContext());
        }
    }

    @Override
    public void onRefresh() {
        // if there's no connection and no pending one, attempt a new one
        if (!connected && !isConnectPending) {
            connect();
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

    public void publishMQTTDevice(String id, String name) {
        publishDeviceProxy(new MQTTDevice(this, id, name, DeviceType.SENSOR));
    }

    private void restoreDevices() {
        Collection<HobsonDeviceDescriptor> devices = getPublishedDeviceDescriptions();
        if (devices != null) {
            for (HobsonDeviceDescriptor hdd : devices) {
                publishDeviceProxy(new MQTTDevice(this, hdd));
            }
        }
    }

    // ***
    // MQTTEventDelegate methods
    // ***

    @Override
    public void activateDevice(final String deviceId, final Map<String,Object> variables) {
        executeInEventLoop(new Runnable() {
            @Override
            public void run() {
                HobsonDeviceProxy proxy = getDeviceProxy(deviceId);
                if (proxy instanceof MQTTDevice) {
                    ((MQTTDevice)proxy).onActivation(variables);
                    if (!embeddedBroker) {
                        try {
                            JSONObject json = new JSONObject();
                            json.put("deviceId", deviceId);
                            json.put("deviceSecret", getDeviceSecret(deviceId));
                            mqtt.publish("hobson/admin/activations", json.toString().getBytes(), 0, false);
                        } catch (MqttException e) {
                            logger.error("Error publishing activation message to broker", e);
                        }
                    }
                }
            }
        });
    }

    @Override
    public String getDeviceSecret(String deviceId) {
        return (String)getDeviceConfigurationProperty(deviceId, MQTTDevice.PROP_SECRET);
    }

    @Override
    public boolean hasDevice(String deviceId) {
        try {
            return (getDeviceProxy(deviceId) != null);
        } catch (HobsonNotFoundException e) {
            return false;
        }
    }

    @Override
    public boolean isDeviceActivated(String deviceId) {
        return ((MQTTDevice)getDeviceProxy(deviceId)).isActivated();
    }

    // ***
    // Private methods
    // ***

    private void connect() {
        try {
            if (mqtt == null) {
                mqtt = new MqttAsyncClient(clientBrokerUrl, "Hobson Hub", new MemoryPersistence());
                mqtt.setCallback(this);
            }
            logger.debug("Attempting broker connection to {} with user {}", clientBrokerUrl, connOpts.getUserName());
            isConnectPending = true;
            mqtt.connect(connOpts, "", new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken token) {
                    logger.info("Connected to MQTT broker at " + clientBrokerUrl);
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

    private void disconnect() {
        try {
            MqttAsyncClient c = mqtt;
            mqtt = null;
            c.disconnect();
        } catch (MqttException e) {
            logger.error("Error disconnecting from broker", e);
        } finally {
            isConnectPending = false;
            connected = false;
        }
    }
}

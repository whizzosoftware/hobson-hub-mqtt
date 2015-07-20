/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.mqtt;

import com.whizzosoftware.hobson.api.plugin.AbstractHobsonPlugin;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MQTT plugin. This creates an embedded MQTT broker to proxy MQTT events as Hobson ones.
 *
 * @author Dan Noguerol
 */
public class MQTTPlugin extends AbstractHobsonPlugin {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public MQTTPlugin(String pluginId) {
        super(pluginId);
    }

    // ***
    // HobsonPlugin methods
    // ***

    @Override
    public void onStartup(PropertyContainer config) {
        // TODO
    }

    @Override
    protected TypedProperty[] createSupportedProperties() {
        return new TypedProperty[] {};
    }

    @Override
    public String getName() {
        return "MQTT Plugin";
    }

    @Override
    public void onPluginConfigurationUpdate(PropertyContainer config) {
    }
}

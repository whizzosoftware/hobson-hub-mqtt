/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.mqtt;

import com.whizzosoftware.hobson.api.device.DeviceBootstrap;
import com.whizzosoftware.hobson.api.device.DeviceManager;
import com.whizzosoftware.hobson.api.hub.HubContext;
import org.eclipse.moquette.spi.impl.security.IAuthenticator;

/**
 * A Hobson-specific authenticator for Moquette. It confirms username/password based on the device manager's
 * bootstrap information.
 *
 * @author Dan Noguerol
 */
public class MQTTAuthenticator implements IAuthenticator {
    private HubContext context;
    private DeviceManager deviceManager;
    private String adminUser;
    private String adminPassword;

    public MQTTAuthenticator(HubContext context, DeviceManager deviceManager, String adminUser, String adminPassword) {
        this.context = context;
        this.deviceManager = deviceManager;
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
    }

    public boolean checkValid(String username, String password) {
        DeviceBootstrap db = deviceManager.getDeviceBootstrap(context, username);
        return ((username.equals(adminUser) && password.equals(adminPassword)) || (db != null && db.getSecret() != null && db.getSecret().equals(password)));
    }
}

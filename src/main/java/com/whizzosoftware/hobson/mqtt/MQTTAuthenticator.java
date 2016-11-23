/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.mqtt;

import io.moquette.spi.security.IAuthenticator;

/**
 * A Hobson-specific authenticator for Moquette. It confirms username/password based on the device manager's
 * bootstrap information.
 *
 * @author Dan Noguerol
 */
public class MQTTAuthenticator implements IAuthenticator {
    private MQTTSecretProvider provider;
    private String adminUser;
    private String adminPassword;

    public MQTTAuthenticator(MQTTSecretProvider provider, String adminUser, String adminPassword) {
        this.provider = provider;
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
    }

    public boolean checkValid(String username, byte[] p) {
        String secret = provider.getDeviceSecret(username);
        String password = new String(p);
        return ((username.equals(adminUser) && password.equals(adminPassword)) || (secret != null && secret.equals(password)));
    }
}

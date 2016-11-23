/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.mqtt;

import io.moquette.spi.security.IAuthorizator;

/**
 * A Hobson-specific Moquette "authorizator". It enforces access control policies for admin, anonymous
 * and authenticated users.
 *
 * @author Dan Noguerol
 */
public class MQTTAuthorizator implements IAuthorizator {
    private String adminUser;

    public MQTTAuthorizator(String adminUser) {
        this.adminUser = adminUser;
    }

    @Override
    public boolean canWrite(String topic, String user, String client) {
        return (
            adminUser.equals(user) ||
            topic.startsWith("bootstrap") ||
            topic.equals("device/" + user + "/data")
        );
    }

    @Override
    public boolean canRead(String topic, String user, String client) {
        return (
            adminUser.equals(user) ||
            topic.startsWith("bootstrap") || // TODO: can we make this more specific?
            topic.startsWith("device/" + user + "/command")
        );
    }
}

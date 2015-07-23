/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.mqtt;

import com.whizzosoftware.smartobjects.object.SmartObject;

import java.util.Collection;

public interface MQTTEventListener {
    public void onDeviceRegistration(String id, String name, Collection<SmartObject> initialData);
    public void onDeviceData(String id, Collection<SmartObject> objects);
}

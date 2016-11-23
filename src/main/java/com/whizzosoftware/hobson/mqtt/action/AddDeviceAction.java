/*
 *******************************************************************************
 * Copyright (c) 2016 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************
*/
package com.whizzosoftware.hobson.mqtt.action;

import com.whizzosoftware.hobson.api.action.ActionLifecycleContext;
import com.whizzosoftware.hobson.api.action.SingleAction;
import com.whizzosoftware.hobson.api.action.ActionExecutionContext;
import com.whizzosoftware.hobson.api.plugin.EventLoopExecutor;
import com.whizzosoftware.hobson.api.plugin.PluginContext;

import java.util.Map;

public class AddDeviceAction extends SingleAction {
    public AddDeviceAction(PluginContext pctx, ActionExecutionContext actx, EventLoopExecutor executor) {
        super(pctx, actx, executor);
    }

    @Override
    public void onStart(ActionLifecycleContext ctx) {
        Map<String,Object> properties = getContext().getProperties();
        ((AddDeviceActionContext)getContext()).publishMQTTDevice(
            (String)properties.get("id"),
            (String)properties.get("name")
        );
        ctx.complete();
    }

    @Override
    public void onMessage(ActionLifecycleContext ctx, String name, Object prop) {
    }

    @Override
    public void onStop(ActionLifecycleContext ctx) {
    }
}

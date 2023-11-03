/*
 * Copyright (c) 2010-2022 DGIT Systems Pty. Ltd. All Rights Reserved.
 *
 * This program and the accompanying materials are the property of DGIT
 * Systems Pty. Ltd.
 *
 * You may obtain a copy of the Licence at http://www.dgit.biz/licence
 */

package com.telflow.secore.template;

import javax.ws.rs.core.Response;

public class TestApiClass implements TestApi {
    @Override
    public Response ping() {
        return Response.ok().entity("pong").build();
    }
}

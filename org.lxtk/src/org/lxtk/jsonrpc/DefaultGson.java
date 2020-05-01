/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.lxtk.jsonrpc;

import java.util.Collections;

import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;

import com.google.gson.Gson;

/**
 * TODO JavaDoc
 */
public class DefaultGson
{
    /**
     * TODO JavaDoc
     */
    public static final Gson INSTANCE = new MessageJsonHandler(
        Collections.emptyMap()).getGson();

    private DefaultGson()
    {
    }
}

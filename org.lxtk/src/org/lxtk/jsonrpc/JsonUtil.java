/*******************************************************************************
 * Copyright (c) 2022 1C-Soft LLC.
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

import com.google.gson.Gson;

/**
 * Provides static utility methods related to JSON.
 */
public class JsonUtil
{
    private static final Gson GSON = DefaultGson.INSTANCE;

    /**
     * Makes a deep copy of the given object by first serializing it to JSON
     * and then deserializing it.
     *
     * @param <T> object type
     * @param o an object (not <code>null</code>)
     * @return a deep copy of the given object
     */
    public static <T> T deepCopy(T o)
    {
        @SuppressWarnings("unchecked")
        T deepCopy = (T)GSON.fromJson(GSON.toJson(o), o.getClass());
        return deepCopy;
    }

    private JsonUtil()
    {
    }
}

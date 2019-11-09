/*******************************************************************************
 * Copyright (c) 2019 1C-Soft LLC.
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

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import org.eclipse.lsp4j.jsonrpc.MessageConsumer;

/**
 * TODO JavaDoc
 *
 * @param <T> remote interface type
 */
public interface JsonRpcConnectionFactory<T>
{
    /**
     * TODO JavaDoc
     *
     * @param localService the object that receives method calls from the
     *  remote service (not <code>null</code>)
     * @param remoteInterface an interface on which RPC methods are looked up
     *  (not <code>null</code>)
     * @param executorService the executor service used to start threads
     *  (may be <code>null</code>)
     * @param wrapper a function for plugging in additional message consumers
     *  (may be <code>null</code>)
     * @return a new JSON-RPC connection (never <code>null</code>)
     */
    JsonRpcConnection<T> newConnection(Object localService,
        Class<T> remoteInterface, ExecutorService executorService,
        Function<MessageConsumer, MessageConsumer> wrapper);
}

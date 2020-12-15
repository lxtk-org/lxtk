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
package org.lxtk.lx4e.ui;

import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.WorkDoneProgress;
import org.lxtk.client.AbstractLanguageClient;
import org.lxtk.client.AbstractLanguageClientController;
import org.lxtk.client.ConnectionInitializer;
import org.lxtk.jsonrpc.JsonRpcConnection;

/**
 * Provides API and partial implementation for controlling an Eclipse-based language client
 * talking to a language server according to the Language Server Protocol.
 * <p>
 * This implementation is thread-safe.
 * </p>
 *
 * @param <S> server interface type
 */
public abstract class EclipseLanguageClientController<S extends LanguageServer>
    extends AbstractLanguageClientController<S>
{
    @Override
    protected ConnectionInitializer newConnectionInitializer(AbstractLanguageClient<S> client,
        JsonRpcConnection<S> connection)
    {
        return new ConnectionInitializer(client, connection, () -> computeInitializeParams(client),
            getInitializeTimeout())
        {
            @Override
            protected WorkDoneProgress newWorkDoneProgress()
            {
                return WorkDoneProgressFactory.newWorkDoneProgressWithJob(true);
            }
        };
    }
}

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
package org.lxtk;

import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.WorkDoneProgressBegin;
import org.eclipse.lsp4j.WorkDoneProgressEnd;
import org.eclipse.lsp4j.WorkDoneProgressNotification;
import org.eclipse.lsp4j.WorkDoneProgressReport;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Default implementation of the {@link WorkDoneProgress} interface.
 */
public class DefaultWorkDoneProgress
    extends AbstractProgress
    implements WorkDoneProgress
{
    private volatile DefaultWorkDoneProgressState state;

    /**
     * Constructor.
     *
     * @param token not <code>null</code>
     */
    public DefaultWorkDoneProgress(Either<String, Number> token)
    {
        super(token);
    }

    @Override
    public final WorkDoneProgressState getState()
    {
        return state;
    }

    @Override
    protected void doAccept(ProgressParams params)
    {
        WorkDoneProgressNotification value = params.getValue();
        if (value == null)
            return;

        switch (value.getKind())
        {
        case begin:
            begin((WorkDoneProgressBegin)value);
            break;
        case report:
            report((WorkDoneProgressReport)value);
            break;
        case end:
            end((WorkDoneProgressEnd)value);
            break;
        }
    }

    /**
     * Signals the beginning of progress reporting.
     *
     * @param params never <code>null</code>
     */
    protected void begin(WorkDoneProgressBegin params)
    {
        state = new DefaultWorkDoneProgressState(params);
    }

    /**
     * Called on each progress report.
     *
     * @param params never <code>null</code>
     */
    protected void report(WorkDoneProgressReport params)
    {
        state = state.update(params);
    }

    /**
     * Signals the end of progress reporting.
     *
     * @param params never <code>null</code>
     */
    protected void end(WorkDoneProgressEnd params)
    {
        toCompletableFuture().complete(null);
    }
}

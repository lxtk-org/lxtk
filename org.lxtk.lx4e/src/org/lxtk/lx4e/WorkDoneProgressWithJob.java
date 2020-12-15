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
package org.lxtk.lx4e;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.lsp4j.WorkDoneProgressBegin;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.DefaultWorkDoneProgress;
import org.lxtk.WorkDoneProgress;

/**
 * A {@link WorkDoneProgress} that schedules a job for reporting progress.
 */
public class WorkDoneProgressWithJob
    extends DefaultWorkDoneProgress
{
    /**
     * Constructor.
     *
     * @param token not <code>null</code>
     */
    public WorkDoneProgressWithJob(Either<String, Number> token)
    {
        super(token);
    }

    @Override
    protected void begin(WorkDoneProgressBegin params)
    {
        super.begin(params);

        schedule(Job.create(params.getTitle(), monitor ->
        {
            CompletableFuture<Void> future = toCompletableFuture();
            ProgressSupport<Void> progressSupport =
                new ProgressSupport<>(future, () -> getState(), () -> getLastUpdated());
            try
            {
                progressSupport.join(monitor);
            }
            catch (OperationCanceledException e)
            {
                future.cancel(true);
                throw e;
            }
            catch (CancellationException e)
            {
                throw new OperationCanceledException();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new OperationCanceledException();
            }
            catch (ExecutionException e)
            {
                // ignore
            }
        }));
    }

    /**
     * Schedules the job.
     *
     * @param job never <code>null</code>
     */
    protected void schedule(Job job)
    {
        job.schedule();
    }
}

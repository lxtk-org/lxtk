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

import java.util.UUID;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.lxtk.DefaultWorkDoneProgress;
import org.lxtk.WorkDoneProgress;
import org.lxtk.lx4e.WorkDoneProgressWithJob;

/**
 * Provides static utility methods for creating a {@link WorkDoneProgress}.
 */
public class WorkDoneProgressFactory
{
    /**
     * Creates and returns a basic {@link WorkDoneProgress} for a generated token.
     *
     * @return the created progress object (never <code>null</code>)
     */
    public static WorkDoneProgress newWorkDoneProgress()
    {
        return newWorkDoneProgress(Either.forLeft(UUID.randomUUID().toString()));
    }

    /**
     * Creates and returns a basic {@link WorkDoneProgress} for the given token.
     *
     * @param token not <code>null</code>
     * @return the created progress object (never <code>null</code>)
     */
    public static WorkDoneProgress newWorkDoneProgress(Either<String, Number> token)
    {
        return new DefaultWorkDoneProgress(token);
    }

    /**
     * Creates and returns a {@link WorkDoneProgress} that schedules a job for reporting progress
     * for a generated token.
     *
     * @param showInDialog whether to open a progress dialog on the job when it starts to run
     * @return the created progress object (never <code>null</code>)
     */
    public static WorkDoneProgress newWorkDoneProgressWithJob(boolean showInDialog)
    {
        return newWorkDoneProgressWithJob(Either.forLeft(UUID.randomUUID().toString()),
            showInDialog);
    }

    /**
     * Creates and returns a {@link WorkDoneProgress} that schedules a job for reporting progress
     * for the given token.
     *
     * @param token not <code>null</code>
     * @param showInDialog whether to open a progress dialog on the job when it starts to run
     * @return the created progress object (never <code>null</code>)
     */
    public static WorkDoneProgress newWorkDoneProgressWithJob(Either<String, Number> token,
        boolean showInDialog)
    {
        return new WorkDoneProgressWithJob(token)
        {
            @Override
            protected void schedule(Job job)
            {
                super.schedule(job);
                if (showInDialog)
                {
                    IWorkbench workbench = PlatformUI.getWorkbench();
                    workbench.getDisplay().asyncExec(() ->
                    {
                        workbench.getProgressService().showInDialog(null, job);
                    });
                }
            }
        };
    }

    private WorkDoneProgressFactory()
    {
    }
}

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
package org.lxtk.lx4e.internal.ui;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;

public class TaskExecutor
{
    public static <P, R> Map<P, R> parallelCompute(P[] inputElements,
        BiFunction<P, IProgressMonitor, R> taskFunction, String taskName, Duration timeout,
        IProgressMonitor monitor)
    {
        if (inputElements.length == 0)
            throw new IllegalArgumentException();

        Map<P, R> results = new LinkedHashMap<>();

        for (P inputElement : inputElements)
        {
            results.put(inputElement, null); // initialize iteration order
        }

        JobGroup jobGroup = new JobGroup(taskName, 0, inputElements.length);

        for (P inputElement : inputElements)
        {
            Job job = Job.create(taskName, pm ->
            {
                R result = taskFunction.apply(inputElement, pm);

                synchronized (results)
                {
                    results.put(inputElement, result);
                }
            });
            job.setJobGroup(jobGroup);
            job.schedule();
        }

        try
        {
            jobGroup.join(timeout.toMillis(), monitor);
        }
        catch (InterruptedException e)
        {
            // ignore
        }

        return results;
    }

    private TaskExecutor()
    {
    }
}

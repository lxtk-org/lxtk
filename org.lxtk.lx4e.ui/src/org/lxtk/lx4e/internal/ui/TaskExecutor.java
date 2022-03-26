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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;

public class TaskExecutor
{
    public static <P> void parallelExecute(P[] inputElements, BiConsumer<P, IProgressMonitor> task,
        String taskName, Duration timeout, IProgressMonitor monitor)
    {
        if (inputElements.length == 0)
            return;

        JobGroup jobGroup = new JobGroup(taskName, 0, inputElements.length);

        for (P inputElement : inputElements)
        {
            Job job = Job.create(taskName, pm ->
            {
                task.accept(inputElement, pm);
            });
            job.setJobGroup(jobGroup);
            job.schedule();
        }

        try
        {
            jobGroup.join(timeout != null ? timeout.toMillis() : 0, monitor);
        }
        catch (InterruptedException e)
        {
            // ignore
        }
    }

    public static <P, R> Map<P, R> parallelCompute(P[] inputElements,
        BiFunction<P, IProgressMonitor, R> taskFunction, String taskName, Duration timeout,
        IProgressMonitor monitor)
    {
        if (inputElements.length == 0)
            return Map.of();

        Map<P, R> results = new LinkedHashMap<>();

        for (P inputElement : inputElements)
        {
            results.put(inputElement, null); // initialize iteration order
        }

        parallelExecute(inputElements, (inputElement, pm) ->
        {
            R result = taskFunction.apply(inputElement, pm);

            synchronized (results)
            {
                results.put(inputElement, result);
            }
        }, taskName, timeout, monitor);

        return results;
    }

    public static <P> void sequentialExecute(P[] inputElements,
        BiFunction<P, Duration, Boolean> task, Duration timeout)
    {
        for (P inputElement : inputElements)
        {
            long startTimeMillis = System.currentTimeMillis();

            if (Boolean.TRUE.equals(task.apply(inputElement, timeout)))
                break;

            timeout = timeout.minusMillis(System.currentTimeMillis() - startTimeMillis);
            if (timeout.isNegative() || timeout.isZero())
                break;
        }
    }

    public static <P, R> Map<P, R> sequentialCompute(P[] inputElements,
        BiFunction<P, Duration, R> taskFunction, Predicate<R> stopPredicate, Duration timeout)
    {
        if (inputElements.length == 0)
            return Map.of();

        Map<P, R> results = new LinkedHashMap<>();

        sequentialExecute(inputElements, (inputElement, taskTimeout) ->
        {
            R result = taskFunction.apply(inputElement, taskTimeout);

            results.put(inputElement, result);

            return stopPredicate.test(result);

        }, timeout);

        return results;
    }

    private TaskExecutor()
    {
    }
}

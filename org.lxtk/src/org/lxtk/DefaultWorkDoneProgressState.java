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

import java.util.Objects;

import org.eclipse.lsp4j.WorkDoneProgressBegin;
import org.eclipse.lsp4j.WorkDoneProgressReport;

/**
 * Default implementation of the {@link WorkDoneProgressState} interface.
 */
public final class DefaultWorkDoneProgressState
    implements WorkDoneProgressState
{
    private final String title;
    private final String message;
    private final Integer percentage;
    private final Boolean cancellable;

    /**
     * Constructor.
     *
     * @param params not <code>null</code>
     */
    public DefaultWorkDoneProgressState(WorkDoneProgressBegin params)
    {
        this(params.getTitle(), params.getMessage(), params.getPercentage(),
            params.getCancellable());
    }

    private DefaultWorkDoneProgressState(String title, String message, Integer percentage,
        Boolean cancellable)
    {
        this.title = Objects.requireNonNull(title);
        this.message = message;
        this.percentage = normalizePercentage(percentage);
        this.cancellable = cancellable;
    }

    /**
     * Based on this state, returns a new state updated according to the given progress report.
     *
     * @param params not <code>null</code>
     * @return the new, updated state (never <code>null</code>)
     */
    public DefaultWorkDoneProgressState update(WorkDoneProgressReport params)
    {
        String message = params.getMessage();
        if (message == null)
            message = this.message;

        Integer percentage = null;
        if (this.percentage != null)
        {
            percentage = params.getPercentage();
            if (percentage == null || percentage < this.percentage)
                percentage = this.percentage;
        }

        Boolean cancellable = params.getCancellable();
        if (cancellable == null)
            cancellable = this.cancellable;

        return new DefaultWorkDoneProgressState(title, message, percentage, cancellable);
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public String getMessage()
    {
        return message;
    }

    @Override
    public Integer getPercentage()
    {
        return percentage;
    }

    @Override
    public Boolean getCancellable()
    {
        return cancellable;
    }

    private static Integer normalizePercentage(Integer percentage)
    {
        if (percentage == null)
            return null;

        if (percentage < 0)
            return 0;

        if (percentage > 100)
            return 100;

        return percentage;
    }
}

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

/**
 * Describes the state of a work done progress. 
 *
 * @see WorkDoneProgress
 */
public interface WorkDoneProgressState
{
    /**
     * Returns the progress title.
     *
     * @return the progress title (never <code>null</code>)
     */
    String getTitle();

    /**
     * Returns the progress message.
     *
     * @return the progress message, or <code>null</code> if none
     */
    String getMessage();

    /**
     * Returns the progress percentage.
     *
     * @return the progress percentage [0-100], or <code>null</code> if progress is infinite
     */
    Integer getPercentage();

    /**
     * Returns the progress cancellability.
     *
     * @return the progress cancellability, or <code>null</code> if not specified
     */
    Boolean getCancellable();
}

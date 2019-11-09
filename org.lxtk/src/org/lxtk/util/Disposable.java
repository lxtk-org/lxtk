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
package org.lxtk.util;

import java.util.Arrays;

/**
 * TODO JavaDoc
 */
public interface Disposable
{
    /**
     * TODO JavaDoc
     *
     */
    void dispose();

    /**
     * TODO JavaDoc
     *
     * @param disposables not <code>null</code>, may contain <code>null</code>s
     */
    static void disposeAll(Disposable... disposables)
    {
        disposeAll(Arrays.asList(disposables));
    }

    /**
     * TODO JavaDoc
     *
     * @param disposables not <code>null</code>, may contain <code>null</code>s
     */
    static void disposeAll(Iterable<? extends Disposable> disposables)
    {
        Throwable thrown = null;
        for (Disposable disposable : disposables)
        {
            try
            {
                if (disposable != null)
                    disposable.dispose();
            }
            catch (RuntimeException | Error e)
            {
                if (thrown == null)
                    thrown = e;
                else
                    thrown.addSuppressed(e);
            }
        }
        if (thrown == null)
            return;
        if (thrown instanceof RuntimeException)
            throw (RuntimeException)thrown;
        if (thrown instanceof Error)
            throw (Error)thrown;
    }
}

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
package org.lxtk.lx4e.ui.highlight;

import java.util.Objects;

import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;

/**
 * Turns off occurrence highlighting until linked mode is left.
 */
public class HighlightingSynchronizer
    implements ILinkedModeListener
{
    private final Highlighter highlighter;
    private final boolean wasInstalled;

    /**
     * TODO JavaDoc
     *
     * @param highlighter not <code>null</code>
     */
    public HighlightingSynchronizer(Highlighter highlighter)
    {
        this.highlighter = Objects.requireNonNull(highlighter);
        if (wasInstalled = highlighter.isInstalled())
            highlighter.uninstall();
    }

    @Override
    public void left(LinkedModeModel model, int flags)
    {
        if (wasInstalled)
            highlighter.install();
    }

    @Override
    public void suspend(LinkedModeModel model)
    {
    }

    @Override
    public void resume(LinkedModeModel model, int flags)
    {
    }
}

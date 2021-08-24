/*******************************************************************************
 * Copyright (c) 2021 1C-Soft LLC.
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

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.text.ITextViewer;

public final class LinkedEditingPubSub
{
    public static final LinkedEditingPubSub INSTANCE = new LinkedEditingPubSub();

    private final ListenerList<ILinkedEditingListener> listeners = new ListenerList<>();

    public void addLinkedEditingListener(ILinkedEditingListener listener)
    {
        listeners.add(listener);
    }

    public void removeLinkedEditingListener(ILinkedEditingListener listener)
    {
        listeners.remove(listener);
    }

    public void fireLinkedEditingStarted(ITextViewer viewer)
    {
        for (ILinkedEditingListener listener : listeners)
        {
            SafeRunner.run(() -> listener.linkedEditingStarted(viewer));
        }
    }

    public void fireLinkedEditingStopped(ITextViewer viewer)
    {
        for (ILinkedEditingListener listener : listeners)
        {
            SafeRunner.run(() -> listener.linkedEditingStopped(viewer));
        }
    }
}

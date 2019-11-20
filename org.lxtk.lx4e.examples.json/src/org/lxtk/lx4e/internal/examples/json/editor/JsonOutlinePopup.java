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
package org.lxtk.lx4e.internal.examples.json.editor;

import org.eclipse.handly.ui.IInputElementProvider;
import org.eclipse.handly.ui.quickoutline.HandlyOutlinePopup;
import org.eclipse.handly.ui.viewer.ElementTreeContentProvider;
import org.eclipse.handly.ui.viewer.ProblemMarkerLabelDecorator;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.lxtk.lx4e.examples.json.JsonInputElementProvider;
import org.lxtk.lx4e.ui.LanguageElementLabelProvider;

/**
 * TODO JavaDoc
 */
public class JsonOutlinePopup
    extends HandlyOutlinePopup
{
    @Override
    protected IInputElementProvider getInputElementProvider()
    {
        return JsonInputElementProvider.INSTANCE;
    }

    @Override
    protected ITreeContentProvider getContentProvider()
    {
        return new ElementTreeContentProvider();
    }

    @Override
    protected IBaseLabelProvider getLabelProvider()
    {
        return new DecoratingStyledCellLabelProvider(
            new LanguageElementLabelProvider(),
            new ProblemMarkerLabelDecorator(), null);
    }
}

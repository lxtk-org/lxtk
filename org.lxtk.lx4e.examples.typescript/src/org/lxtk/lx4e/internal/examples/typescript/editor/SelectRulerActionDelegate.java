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
package org.lxtk.lx4e.internal.examples.typescript.editor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;
import org.lxtk.lx4e.internal.examples.typescript.Activator;
import org.lxtk.lx4e.ui.SelectAnnotationRulerAction;

public class SelectRulerActionDelegate
    extends AbstractRulerActionDelegate
{
    @Override
    protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo)
    {
        return new SelectAnnotationRulerAction(editor, rulerInfo,
            Activator.getDefault().getCombinedPreferenceStore());
    }
}

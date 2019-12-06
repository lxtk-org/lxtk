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
package org.lxtk.lx4e.internal.ui;

import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension4;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;

/**
 * TODO JavaDoc
 */
@SuppressWarnings("restriction")
public class FocusableInformationControlCreator
    extends AbstractReusableInformationControlCreator
{
    @Override
    public boolean canReuse(IInformationControl control)
    {
        if (!super.canReuse(control))
            return false;

        if (control instanceof IInformationControlExtension4)
            ((IInformationControlExtension4)control).setStatusText(
                getTooltipAffordanceString());

        return true;
    }

    @Override
    protected IInformationControl doCreateInformationControl(Shell parent)
    {
        if (BrowserInformationControl.isAvailable(parent))
            return newBrowserInformationControl(parent);

        return newDefaultInformationControl(parent);
    }

    protected BrowserInformationControl newBrowserInformationControl(
        Shell parent)
    {
        return new BrowserInformationControl(parent, getSymbolicFontName(),
            getTooltipAffordanceString())
        {
            @Override
            public IInformationControlCreator getInformationPresenterControlCreator()
            {
                return parent -> new BrowserInformationControl(parent,
                    getSymbolicFontName(), true);
            }
        };
    }

    protected DefaultInformationControl newDefaultInformationControl(
        Shell parent)
    {
        return new DefaultInformationControl(parent,
            getTooltipAffordanceString())
        {
            @Override
            public IInformationControlCreator getInformationPresenterControlCreator()
            {
                return parent -> new DefaultInformationControl(parent, true);
            };
        };
    }

    protected String getSymbolicFontName()
    {
        return JFaceResources.DEFAULT_FONT;
    }

    protected String getTooltipAffordanceString()
    {
        return EditorsUI.getTooltipAffordanceString();
    }
}

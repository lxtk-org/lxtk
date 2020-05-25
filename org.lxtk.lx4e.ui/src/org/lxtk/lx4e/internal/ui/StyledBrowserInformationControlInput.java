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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.internal.text.html.BrowserInformationControlInput;
import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.osgi.framework.Bundle;

@SuppressWarnings("restriction")
public abstract class StyledBrowserInformationControlInput
    extends BrowserInformationControlInput
{
    private static String defaultStyleSheet;

    public static BrowserInformationControlInput of(String html)
    {
        int max = Math.min(100, html.length());
        if (html.substring(0, max).indexOf("<html>") != -1) //$NON-NLS-1$
            // there is already a header
            return new BrowserInformationControlInput(null)
            {
                @Override
                public String getInputName()
                {
                    return ""; //$NON-NLS-1$
                }

                @Override
                public Object getInputElement()
                {
                    return html;
                }

                @Override
                public String getHtml()
                {
                    return html;
                }
            };

        return new StyledBrowserInformationControlInput(null)
        {
            @Override
            public String getInputName()
            {
                return ""; //$NON-NLS-1$
            }

            @Override
            public Object getInputElement()
            {
                return html;
            }

            @Override
            protected String getHtmlFragment()
            {
                return html;
            }
        };
    }

    /**
     * Creates the next browser input with the given input as previous one.
     *
     * @param previous the previous input or <code>null</code> if none
     */
    public StyledBrowserInformationControlInput(BrowserInformationControlInput previous)
    {
        super(previous);
    }

    @Override
    public String getHtml()
    {
        StringBuilder builder = new StringBuilder(getHtmlFragment());
        HTMLPrinter.insertPageProlog(builder, 0, getFgRgb(), getBgRgb(),
            HTMLPrinter.convertTopLevelFont(getStyleSheet(), getFontData()));
        HTMLPrinter.addPageEpilog(builder);
        return builder.toString();
    }

    @Override
    public String toString()
    {
        return getHtmlFragment();
    }

    protected abstract String getHtmlFragment();

    protected String getStyleSheet()
    {
        return getDefaultStyleSheet();
    }

    protected FontData getFontData()
    {
        return JFaceResources.getFontRegistry().getFontData(JFaceResources.DIALOG_FONT)[0];
    }

    protected RGB getFgRgb()
    {
        return JFaceResources.getColorRegistry().getRGB(
            "org.eclipse.ui.workbench.HOVER_FOREGROUND"); //$NON-NLS-1$
    }

    protected RGB getBgRgb()
    {
        return JFaceResources.getColorRegistry().getRGB(
            "org.eclipse.ui.workbench.HOVER_BACKGROUND"); //$NON-NLS-1$
    }

    private static String getDefaultStyleSheet()
    {
        if (defaultStyleSheet == null)
            defaultStyleSheet = loadDefaultStyleSheet();
        return defaultStyleSheet;
    }

    private static String loadDefaultStyleSheet()
    {
        Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
        URL styleSheetUrl = bundle.getEntry("/TextHoverStyleSheet.css"); //$NON-NLS-1$
        if (styleSheetUrl == null)
            return null;

        try (
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(styleSheetUrl.openStream())))
        {
            StringBuilder builder = new StringBuilder(1500);
            String line = reader.readLine();
            while (line != null)
            {
                builder.append(line);
                builder.append('\n');
                line = reader.readLine();
            }
            return builder.toString();
        }
        catch (IOException e)
        {
            Activator.logError(e);
            return null;
        }
    }
}

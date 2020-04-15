/*******************************************************************************
 * Copyright (c) 2019, 2020 1C-Soft LLC.
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
package org.lxtk.lx4e.ui.hyperlinks;

import java.util.Objects;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

/**
 * A partial implementation of {@link IHyperlink}.
 */
public abstract class AbstractHyperlink
    implements IHyperlink
{
    private final IRegion region;
    private final String text;

    /**
     * Constructor.
     *
     * @param region the hyperlink region (not <code>null</code>)
     * @param text optional text for this hyperlink (may be <code>null</code>)
     */
    public AbstractHyperlink(IRegion region, String text)
    {
        this.region = Objects.requireNonNull(region);
        this.text = text;
    }

    @Override
    public IRegion getHyperlinkRegion()
    {
        return region;
    }

    @Override
    public String getHyperlinkText()
    {
        return text;
    }

    @Override
    public String getTypeLabel()
    {
        return null;
    }
}

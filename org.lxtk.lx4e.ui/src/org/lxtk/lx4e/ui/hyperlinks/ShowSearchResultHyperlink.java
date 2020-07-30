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
package org.lxtk.lx4e.ui.hyperlinks;

import java.util.Objects;

import org.eclipse.jface.text.IRegion;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;

/**
 * A hyperlink that runs the given search query and shows the result in the search view.
 */
public class ShowSearchResultHyperlink
    extends AbstractHyperlink
{
    private final ISearchQuery query;

    /**
     * Constructor.
     *
     * @param region the hyperlink region (not <code>null</code>)
     * @param text optional text for this hyperlink (may be <code>null</code>)
     * @param query the search query to run by this hyperlink (not <code>null</code>,
     *  must be able to {@link ISearchQuery#canRunInBackground() run in the background})
     */
    public ShowSearchResultHyperlink(IRegion region, String text, ISearchQuery query)
    {
        super(region, text);
        this.query = Objects.requireNonNull(query);
    }

    /**
     * Returns the associated search query.
     *
     * @return the associated search query (never <code>null</code>)
     */
    public final ISearchQuery getQuery()
    {
        return query;
    }

    @Override
    public void open()
    {
        NewSearchUI.runQueryInBackground(query);
    }
}

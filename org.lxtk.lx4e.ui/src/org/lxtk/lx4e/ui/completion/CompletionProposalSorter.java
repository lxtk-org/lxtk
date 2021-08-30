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
package org.lxtk.lx4e.ui.completion;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalSorter;

/**
 * A sorter for completion proposals.
 */
public final class CompletionProposalSorter
    implements ICompletionProposalSorter
{
    @Override
    public int compare(ICompletionProposal p1, ICompletionProposal p2)
    {
        int scoreDiff = getScore(p2) - getScore(p1);
        if (scoreDiff != 0)
            return scoreDiff;

        return getSortString(p1).compareToIgnoreCase(getSortString(p2));
    }

    private int getScore(ICompletionProposal p)
    {
        if (p instanceof IScoredCompletionProposal)
            return ((IScoredCompletionProposal)p).getScore();

        return 0;
    }

    private String getSortString(ICompletionProposal p)
    {
        if (p instanceof IScoredCompletionProposal)
            return ((IScoredCompletionProposal)p).getSortString();

        return p.getDisplayString();
    }
}

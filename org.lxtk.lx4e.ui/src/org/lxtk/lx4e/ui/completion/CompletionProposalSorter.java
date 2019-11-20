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
package org.lxtk.lx4e.ui.completion;

import java.util.Comparator;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalSorter;

/**
 * TODO JavaDoc
 */
public class CompletionProposalSorter
    implements ICompletionProposalSorter, Comparator<ICompletionProposal>
{
    private static final Comparator<LSCompletionProposal> COMPARATOR =
        new LSCompletionProposalComparator();

    @Override
    public int compare(ICompletionProposal p1, ICompletionProposal p2)
    {
        if (p1 instanceof LSCompletionProposal
            && p2 instanceof LSCompletionProposal)
            return COMPARATOR.compare((LSCompletionProposal)p1,
                (LSCompletionProposal)p2);
        if (p1 instanceof LSCompletionProposal)
            return -1;
        if (p2 instanceof LSCompletionProposal)
            return 1;
        return 0;
    }
}

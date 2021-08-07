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

/**
 * A completion proposal with a score. The score is used to sort the completion proposals.
 * Proposals with higher score should be listed before proposals with lower score.
 * Proposals with equal score are sorted based on their sort string.
 */
public interface IScoredCompletionProposal
    extends ICompletionProposal
{
    /**
     * Returns the score of this completion proposal.
     *
     * @return the completion proposal score
     */
    int getScore();

    /**
     * Returns the sort string for this completion proposal.
     *
     * @return the sort string (never <code>null</code>)
     */
    String getSortString();
}

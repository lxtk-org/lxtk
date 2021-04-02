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
package org.lxtk.lx4e;

import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.lsp4j.PartialResultParams;
import org.eclipse.lsp4j.WorkDoneProgressOptions;
import org.eclipse.lsp4j.WorkDoneProgressParams;
import org.lxtk.LanguageFeatureProvider;
import org.lxtk.PartialResultProgress;
import org.lxtk.ProgressService;

abstract class LanguageFeatureRequestWithWorkDoneAndPartialResultProgress<
    R extends LanguageFeatureProvider<? extends WorkDoneProgressOptions>,
    S extends WorkDoneProgressParams & PartialResultParams, T>
    extends LanguageFeatureRequestWithWorkDoneProgress<R, S, T>
{
    private Supplier<? extends PartialResultProgress> partialResultProgressSupplier;

    /**
     * Ensures that partial result progress will be initiated using the given supplier
     * before the request is sent.
     *
     * @param partialResultProgressSupplier not <code>null</code>
     */
    public void setUpPartialResultProgress(
        Supplier<? extends PartialResultProgress> partialResultProgressSupplier)
    {
        this.partialResultProgressSupplier = Objects.requireNonNull(partialResultProgressSupplier);
    }

    @Override
    void initiateProgress(ProgressService progressService)
    {
        super.initiateProgress(progressService);
        if (partialResultProgressSupplier == null)
            return;
        PartialResultProgress partialResultProgress = partialResultProgressSupplier.get();
        if (partialResultProgress == null)
            return;
        progressService.attachProgress(partialResultProgress);
        setPartialResultProgress(partialResultProgress);
        getParams().setPartialResultToken(partialResultProgress.getToken());
    }
}

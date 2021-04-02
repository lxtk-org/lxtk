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

import org.eclipse.lsp4j.WorkDoneProgressOptions;
import org.eclipse.lsp4j.WorkDoneProgressParams;
import org.lxtk.LanguageFeatureProvider;
import org.lxtk.ProgressService;
import org.lxtk.WorkDoneProgress;

abstract class LanguageFeatureRequestWithWorkDoneProgress<
    R extends LanguageFeatureProvider<? extends WorkDoneProgressOptions>,
    S extends WorkDoneProgressParams, T>
    extends LanguageFeatureRequest<R, S, T>
{
    private Supplier<? extends WorkDoneProgress> workDoneProgressSupplier;

    /**
     * Ensures that work done progress will be initiated using the given supplier
     * before the request is sent.
     *
     * @param workDoneProgressSupplier not <code>null</code>
     */
    public void setUpWorkDoneProgress(Supplier<? extends WorkDoneProgress> workDoneProgressSupplier)
    {
        this.workDoneProgressSupplier = Objects.requireNonNull(workDoneProgressSupplier);
    }

    @Override
    void initiateProgress(ProgressService progressService)
    {
        if (workDoneProgressSupplier == null
            || !Boolean.TRUE.equals(getProvider().getRegistrationOptions().getWorkDoneProgress()))
            return;
        WorkDoneProgress workDoneProgress = workDoneProgressSupplier.get();
        if (workDoneProgress == null)
            return;
        progressService.attachProgress(workDoneProgress);
        setWorkDoneProgress(workDoneProgress);
        getParams().setWorkDoneToken(workDoneProgress.getToken());
    }
}

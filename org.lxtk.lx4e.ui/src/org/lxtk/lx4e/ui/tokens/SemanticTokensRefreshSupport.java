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
package org.lxtk.lx4e.ui.tokens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.progress.UIJob;
import org.lxtk.DocumentSemanticTokensProvider;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.util.Disposable;
import org.lxtk.util.Registry;
import org.lxtk.util.SafeRun;

/**
 * Invalidates text presentation of a given text viewer when semantic tokens need to be refreshed.
 */
public final class SemanticTokensRefreshSupport
    implements Disposable
{
    private final ITextViewer textViewer;
    private final Map<Object, Disposable> subscriptions = new HashMap<>();
    private final UIJob refreshJob = new UIJob("Refresh Semantic Tokens") //$NON-NLS-1$
    {
        @Override
        public IStatus runInUIThread(IProgressMonitor monitor)
        {
            textViewer.invalidateTextPresentation();
            return Status.OK_STATUS;
        }
    };
    private LanguageOperationTarget target;

    /**
     * Constructor.
     *
     * @param textViewer not <code>null</code>
     */
    public SemanticTokensRefreshSupport(ITextViewer textViewer)
    {
        this.textViewer = Objects.requireNonNull(textViewer);
        refreshJob.setSystem(true);
    }

    /**
     * Sets the current {@link LanguageOperationTarget}.
     *
     * @param target may be <code>null</code>
     */
    public synchronized void setTarget(LanguageOperationTarget target)
    {
        unsubscribe();
        if ((this.target = target) != null)
            subscribe();
        refreshJob.schedule();
    }

    @Override
    public synchronized void dispose()
    {
        setTarget(null);
    }

    private synchronized void subscribe()
    {
        SafeRun.run(rollback ->
        {
            rollback.add(this::unsubscribe);

            LanguageService languageService = target.getLanguageService();

            Registry<DocumentSemanticTokensProvider> registry =
                languageService.getDocumentSemanticTokensProviders();
            subscriptions.put(new Object(), registry.onDidAdd().subscribe(this::onDidAddProvider));
            subscriptions.put(new Object(),
                registry.onDidRemove().subscribe(this::onDidRemoveProvider));

            List<DocumentSemanticTokensProvider> providers =
                languageService.getDocumentMatcher().getMatches(registry,
                    DocumentSemanticTokensProvider::getDocumentSelector, target.getDocumentUri(),
                    target.getLanguageId());
            for (DocumentSemanticTokensProvider provider : providers)
            {
                subscriptions.put(provider, provider.onDidChangeSemanticTokens().subscribe(
                    this::onDidChangeSemanticTokens));
            }
        });
    }

    private synchronized void unsubscribe()
    {
        List<Disposable> subs = new ArrayList<>(subscriptions.values());
        subscriptions.clear();
        Disposable.disposeAll(subs);
    }

    private synchronized void onDidAddProvider(DocumentSemanticTokensProvider provider)
    {
        if (subscriptions.containsKey(provider))
            return;
        LanguageService languageService = target.getLanguageService();
        if (languageService.getDocumentSemanticTokensProviders().contains(provider)
            && languageService.getDocumentMatcher().isMatch(provider.getDocumentSelector(),
                target.getDocumentUri(), target.getLanguageId()))
        {
            subscriptions.put(provider,
                provider.onDidChangeSemanticTokens().subscribe(this::onDidChangeSemanticTokens));
            refreshJob.schedule();
        }
    }

    private synchronized void onDidRemoveProvider(DocumentSemanticTokensProvider provider)
    {
        Disposable subscription = subscriptions.remove(provider);
        if (subscription != null)
        {
            refreshJob.schedule();
            subscription.dispose();
        }
    }

    private synchronized void onDidChangeSemanticTokens(Void x)
    {
        refreshJob.cancel();
        refreshJob.schedule(10);
    }
}

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
package org.lxtk.lx4e.ui.codeaction;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.lxtk.CommandService;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;

/**
 * TODO JavaDoc
 */
public abstract class AbstractCodeActionMenu
    extends CompoundContributionItem
    implements IWorkbenchContribution
{
    private IServiceLocator serviceLocator;

    @Override
    public void initialize(IServiceLocator serviceLocator)
    {
        this.serviceLocator = serviceLocator;
    }

    /**
     * TODO JavaDoc
     *
     * @return the service locator
     */
    protected final IServiceLocator getServiceLocator()
    {
        return serviceLocator;
    }

    @Override
    protected IContributionItem[] getContributionItems()
    {
        LanguageOperationTarget target = getLanguageOperationTarget();
        if (target == null)
            return noItems();
        InvocationContext ctx = getInvocationContext();
        if (ctx == null)
            return noItems();
        Range range;
        try
        {
            range = DocumentUtil.toRange(ctx.document, ctx.offset, ctx.length);
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
            return noItems();
        }
        CompletableFuture<List<Either<Command, CodeAction>>> future =
            CodeActions.getCodeActions(target, range, new CodeActionContext(
                Collections.emptyList(), getCodeActionKinds()));
        List<Either<Command, CodeAction>> result = null;
        try
        {
            result = future.get(getTimeout().toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (CancellationException | InterruptedException e)
        {
        }
        catch (ExecutionException e)
        {
            Activator.logError(e);
        }
        catch (TimeoutException e)
        {
            Activator.logWarning(e);
        }
        if (result == null || result.isEmpty())
            return noItems();
        List<IContributionItem> items = new ArrayList<>(result.size());
        for (Either<Command, CodeAction> item : result)
        {
            IAction action = null;
            if (item.isLeft())
                action = getAction(item.getLeft());
            else if (item.isRight())
                action = getAction(item.getRight());
            if (action != null)
                items.add(new ActionContributionItem(action));
        }
        return items.toArray(new IContributionItem[items.size()]);
    }

    /**
     * TODO JavaDoc
     *
     * @return the current {@link InvocationContext},
     *  or <code>null</code> if none
     */
    protected InvocationContext getInvocationContext()
    {
        ITextEditor editor = Adapters.adapt(getActivePart(), ITextEditor.class,
            false);
        if (editor == null)
            return null;

        IDocumentProvider documentProvider = editor.getDocumentProvider();
        if (documentProvider == null)
            return null;

        IDocument document = documentProvider.getDocument(
            editor.getEditorInput());
        if (document == null)
            return null;

        ISelectionProvider selectionProvider = editor.getSelectionProvider();
        if (selectionProvider == null)
            return null;

        ISelection selection = selectionProvider.getSelection();
        if (selection.isEmpty() || !(selection instanceof ITextSelection))
            return null;

        ITextSelection textSelection = (ITextSelection)selection;

        int offset = textSelection.getOffset();
        if (offset < 0)
            return null;

        int length = textSelection.getLength();
        if (length < 0)
            return null;

        return new InvocationContext(document, offset, length);
    }

    /**
     * TODO JavaDoc
     *
     * @return the active workbench part, or <code>null</code> if none
     */
    protected IWorkbenchPart getActivePart()
    {
        if (serviceLocator == null)
            return null;
        IPartService partService = serviceLocator.getService(
            IPartService.class);
        if (partService == null)
            return null;
        return partService.getActivePart();
    }

    /**
     * TODO JavaDoc
     *
     * @return the current {@link LanguageOperationTarget},
     *  or <code>null</code> if none
     */
    protected abstract LanguageOperationTarget getLanguageOperationTarget();

    /**
     * TODO JavaDoc
     *
     * @return the command service (not <code>null</code>)
     */
    protected abstract CommandService getCommandService();

    /**
     * TODO JavaDoc
     *
     * @return a {@link WorkspaceEditChangeFactory} (not <code>null</code>)
     */
    protected abstract WorkspaceEditChangeFactory getWorkspaceEditChangeFactory();

    /**
     * TODO JavaDoc
     *
     * @return the kinds of code actions to request,
     *  or <code>null</code> for all possible kinds
     */
    protected abstract List<String> getCodeActionKinds();

    /**
     * TODO JavaDoc
     *
     * @param command never <code>null</code>
     * @return an action based on the command, or <code>null</code> if none
     */
    protected IAction getAction(Command command)
    {
        return new CommandAction(command);
    }

    /**
     * TODO JavaDoc
     *
     * @param codeAction never <code>null</code>
     * @return an action based on the code action, or <code>null</code> if none
     */
    protected IAction getAction(CodeAction codeAction)
    {
        return new CodeActionAction(codeAction);
    }

    /**
     * TODO JavaDoc
     *
     * @return a label for the "no actions available" item (not <code>null</code>)
     */
    protected String getNoActionsText()
    {
        return Messages.AbstractCodeActionMenu_noActions;
    }

    /**
     * TODO JavaDoc
     *
     * @return a positive duration
     */
    protected Duration getTimeout()
    {
        return Duration.ofSeconds(2);
    }

    private IContributionItem[] noItems()
    {
        return new IContributionItem[] { new ActionContributionItem(
            new NoActionsAction()) };
    }

    /**
     * TODO JavaDoc
     */
    protected static final class InvocationContext
    {
        final IDocument document;
        final int offset, length;

        /**
         * TODO JavaDoc
         *
         * @param document not <code>null</code>
         * @param offset 0-based
         * @param length non-negative
         */
        public InvocationContext(IDocument document, int offset, int length)
        {
            this.document = Objects.requireNonNull(document);
            if (offset < 0)
                throw new IllegalArgumentException();
            this.offset = offset;
            if (length < 0)
                throw new IllegalArgumentException();
            this.length = length;
        }
    }

    /**
     * TODO JavaDoc
     */
    protected class CommandAction
        extends Action
    {
        protected final Command command;

        /**
         * TODO JavaDoc
         *
         * @param command not <code>null</code>
         */
        public CommandAction(Command command)
        {
            this.command = Objects.requireNonNull(command);
            setText(command.getTitle());
        }

        @Override
        public void run()
        {
            CodeActions.execute(command, getText(), getCommandService());
        }
    }

    /**
     * TODO JavaDoc
     */
    protected class CodeActionAction
        extends Action
    {
        protected final CodeAction codeAction;

        /**
         * TODO JavaDoc
         *
         * @param codeAction not <code>null</code>
         */
        public CodeActionAction(CodeAction codeAction)
        {
            this.codeAction = Objects.requireNonNull(codeAction);
            setText(codeAction.getTitle());
        }

        @Override
        public void run()
        {
            CodeActions.execute(codeAction, getText(),
                getWorkspaceEditChangeFactory(), getCommandService());
        }
    }

    private class NoActionsAction
        extends Action
    {
        NoActionsAction()
        {
            setText(getNoActionsText());
            setEnabled(false);
        }
    }
}

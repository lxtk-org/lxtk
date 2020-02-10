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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;
import org.lxtk.CommandService;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;
import org.lxtk.lx4e.ui.DefaultEditorHelper;

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

        Range range = getSelectedTextRange();
        if (range == null)
            return noItems();

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
     * @return the selected text range, or <code>null</code> if none
     */
    protected Range getSelectedTextRange()
    {
        IWorkbenchPart part = getActivePart();
        if (!(part instanceof IEditorPart))
            return null;
        return DefaultEditorHelper.INSTANCE.getSelectedTextRange(
            (IEditorPart)part);
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

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
package org.lxtk.lx4e.ui.refactoring.rename;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringUI;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.ITextEditor;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.internal.ui.RefactoringExecutor;
import org.lxtk.lx4e.refactoring.rename.RenameRefactoring;
import org.lxtk.lx4e.ui.highlight.Highlighter;
import org.lxtk.lx4e.ui.highlight.HighlightingSynchronizer;
import org.lxtk.util.Disposable;

/**
 * Starts a {@link RenameRefactoring} in various modes, such as linked editing, dialog,
 * and direct refactoring.
 */
public class RenameRefactoringStarter
{
    private static final Map<IDocument, RenameLinkedMode> renameLinkedModes = new HashMap<>();

    /**
     * Refactoring to start.
     */
    protected final RenameRefactoring refactoring;

    /**
     * Constructor.
     *
     * @param refactoring not <code>null</code>
     */
    public RenameRefactoringStarter(RenameRefactoring refactoring)
    {
        this.refactoring = Objects.requireNonNull(refactoring);
    }

    /**
     * Starts refactoring in linked editing mode. If linked editing mode is not available,
     * starts refactoring with a dialog.
     *
     * @param editor not <code>null</code>
     */
    public void startLinkedEditing(ITextEditor editor)
    {
        IDocument document = refactoring.getDocument();
        if (editor.getDocumentProvider().getDocument(editor.getEditorInput()) != document)
            throw new IllegalArgumentException();

        RenameLinkedMode renameLinkedMode = renameLinkedModes.get(document);
        if (renameLinkedMode != null)
        {
            if (renameLinkedMode.getCurrentLinkedPosition() != null)
                return;

            renameLinkedMode.exit();
            renameLinkedMode = renameLinkedModes.get(document);
        }
        if (renameLinkedMode != null)
            throw new AssertionError();

        try
        {
            renameLinkedMode = tryStartLinkedEditing(editor);
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
        }

        if (renameLinkedMode != null)
            renameLinkedModes.put(document, renameLinkedMode);
        else
            startRefactoringWithDialog(editor.getSite().getShell(), true);
    }

    /**
     * Starts refactoring with a dialog.
     *
     * @param parent the parent shell for the dialog, or <code>null</code> if the dialog
     *  is a top level dialog
     * @param fullDialog indicates whether the dialog should start with a user input page,
     *  even if refactoring needs no additional input from the user
     */
    public void startRefactoringWithDialog(Shell parent, boolean fullDialog)
    {
        RefactoringWizardOpenOperation op =
            new RefactoringWizardOpenOperation(new RenameRefactoringWizard(refactoring)
            {
                @Override
                protected void addUserInputPages()
                {
                    if (fullDialog || needsUserInput())
                        super.addUserInputPages();
                }

                private boolean needsUserInput()
                {
                    String newName = refactoring.getNewName();
                    return newName == null || newName.equals(refactoring.getCurrentName());
                }
            });
        try
        {
            op.run(parent, refactoring.getName());
        }
        catch (InterruptedException e)
        {
            // do nothing: got canceled by the user
        }
    }

    /**
     * Starts refactoring directly. Throws a runtime exception if refactoring needs
     * additional input from the user.
     *
     * @param parent the parent shell, or <code>null</code> to create a top-level shell
     */
    public void startDirectRefactoring(Shell parent)
    {
        String newName = refactoring.getNewName();
        if (newName == null)
            throw new IllegalStateException();
        if (newName.equals(refactoring.getCurrentName()))
            return;

        try
        {
            RefactoringStatus status = RefactoringExecutor.execute(refactoring, parent);
            if (status.hasFatalError())
            {
                RefactoringUI.createRefactoringStatusDialog(status, parent, refactoring.getName(),
                    false).open();
            }
        }
        catch (InvocationTargetException e)
        {
            StatusManager.getManager().handle(Activator.createErrorStatus(
                MessageFormat.format(Messages.RenameRefactoringStarter_Refactoring_execution_error,
                    refactoring.getName()),
                e.getCause()), StatusManager.LOG | StatusManager.SHOW);
        }
        catch (InterruptedException e)
        {
            // do nothing: got canceled by the user
        }
    }

    /**
     * Attempts to start refactoring in linked editing mode using {@link #getLinkedModeStarter()
     * RenameLinkedModeStarter}.
     *
     * @param editor not <code>null</code>
     * @return the started {@link RenameLinkedMode}, or <code>null</code> if linked editing mode
     *  is not available
     * @throws BadLocationException if some of the linked editing ranges were not valid
     *  in the editor's document
     */
    protected RenameLinkedMode tryStartLinkedEditing(ITextEditor editor) throws BadLocationException
    {
        ITextViewer viewer = getTextViewer(editor);
        if (viewer == null)
            return null;

        IDocument document = refactoring.getDocument();
        if (viewer.getDocument() != document)
            return null;

        if (viewer.getSelectedRange().x != refactoring.getOffset())
            return null;

        boolean[] preview = new boolean[1];

        RenameLinkedMode renameLinkedMode = getLinkedModeStarter().start(viewer,
            new RenameLinkedModeStarter.DeleteBlockingExitPolicy(document)
            {
                @Override
                public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset,
                    int length)
                {
                    preview[0] = (event.stateMask & SWT.CTRL) != 0
                        && (event.character == SWT.CR || event.character == SWT.LF);

                    return super.doExit(model, event, offset, length);
                }
            });
        if (renameLinkedMode == null)
            return null;

        renameLinkedMode.addLinkingListener(new ILinkedModeListener()
        {
            @Override
            public void left(LinkedModeModel model, int flags)
            {
                renameLinkedModes.remove(document);

                if ((flags & ILinkedModeListener.UPDATE_CARET) != 0)
                {
                    String newName;
                    try
                    {
                        newName = renameLinkedMode.getLinkedPositions()[0].getContent();
                    }
                    catch (BadLocationException e)
                    {
                        Activator.logError(e);
                        return;
                    }
                    if (newName.equals(refactoring.getCurrentName()))
                        return;
                    refactoring.setNewName(newName);

                    try (Freezer freezer = Freezer.freeze(Adapters.adapt(editor, Control.class)))
                    {
                        renameLinkedMode.undoChanges();

                        if (preview[0])
                            startRefactoringWithDialog(editor.getSite().getShell(), false);
                        else
                            startDirectRefactoring(editor.getSite().getShell());
                    }
                }
            }

            @Override
            public void suspend(LinkedModeModel model)
            {
            }

            @Override
            public void resume(LinkedModeModel model, int flags)
            {
            }
        });

        Highlighter highlighter = getHighlighter(editor);
        if (highlighter != null)
            renameLinkedMode.addLinkingListener(new HighlightingSynchronizer(highlighter));

        return renameLinkedMode;
    }

    /**
     * Returns the linked mode starter.
     *
     * @return the linked mode starter (not <code>null</code>)
     */
    protected RenameLinkedModeStarter getLinkedModeStarter()
    {
        return new RenameLinkedModeStarter(refactoring.getLanguageOperationTarget());
    }

    /**
     * Returns the text viewer of the given text editor.
     *
     * @param editor may be <code>null</code>
     * @return the text viewer, or <code>null</code> if none
     */
    protected ITextViewer getTextViewer(ITextEditor editor)
    {
        return Adapters.adapt(editor, ITextViewer.class);
    }

    /**
     * Returns the highlighter of the given text editor.
     *
     * @param editor may be <code>null</code>
     * @return the highlighter, or <code>null</code> if none
     */
    protected Highlighter getHighlighter(ITextEditor editor)
    {
        return Adapters.adapt(editor, Highlighter.class);
    }

    private static class Freezer
        implements AutoCloseable
    {
        private final Image image;
        private final Label label;

        static Freezer freeze(Control control)
        {
            if (!(control instanceof Composite))
                return null;

            Composite composite = (Composite)control;
            Display display = composite.getDisplay();

            // Flush pending redraw requests
            int safetyCounter = 0;
            while (display.readAndDispatch() && safetyCounter++ < 100)
            {
            }

            Point size;
            Image image;
            GC gc = new GC(composite);
            try
            {
                size = composite.getSize();
                image = new Image(gc.getDevice(), size.x, size.y);
                gc.copyArea(image, 0, 0);
            }
            finally
            {
                gc.dispose();
            }

            Label label = new Label(composite, SWT.NONE);
            label.setImage(image);
            label.setBounds(0, 0, size.x, size.y);
            label.moveAbove(null);

            return new Freezer(image, label);
        }

        @Override
        public void close()
        {
            Disposable.disposeAll(label::dispose, image::dispose);
        }

        private Freezer(Image image, Label label)
        {
            this.image = image;
            this.label = label;
        }
    }
}

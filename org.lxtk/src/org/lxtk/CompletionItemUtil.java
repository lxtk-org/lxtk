/*******************************************************************************
 * Copyright (c) 2022 1C-Soft LLC.
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
package org.lxtk;

import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemDefaults;
import org.eclipse.lsp4j.InsertReplaceEdit;
import org.eclipse.lsp4j.InsertReplaceRange;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.InsertTextMode;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Provides static utility methods that operate on a {@link CompletionItem}.
 */
public class CompletionItemUtil
{
    /**
     * Returns the commit characters for the given completion item.
     * If the completion item itself does not specify a value for this property,
     * the corresponding value from the given completion item defaults will be returned if present.
     *
     * @param item not <code>null</code>
     * @param defaults may be <code>null</code>
     * @return the commit characters for the given completion item, or <code>null</code> if none
     * @see CompletionItem#getCommitCharacters()
     */
    public static List<String> getCommitCharacters(CompletionItem item,
        CompletionItemDefaults defaults)
    {
        List<String> commitCharacters = item.getCommitCharacters();
        if (commitCharacters == null && defaults != null)
            commitCharacters = defaults.getCommitCharacters();
        return commitCharacters;
    }

    /**
     * Returns the text edit for the given completion item.
     * If the completion item itself does not specify a value for this property,
     * the corresponding value from the given completion item defaults will be returned if present.
     *
     * @param item not <code>null</code>
     * @param defaults may be <code>null</code>
     * @return the text edit for the given completion item, or <code>null</code> if none
     * @see CompletionItem#getTextEdit()
     */
    public static Either<TextEdit, InsertReplaceEdit> getTextEdit(CompletionItem item,
        CompletionItemDefaults defaults)
    {
        Either<TextEdit, InsertReplaceEdit> textEdit = item.getTextEdit();
        if (textEdit == null && defaults != null)
        {
            Either<Range, InsertReplaceRange> editRange = defaults.getEditRange();
            if (editRange != null)
            {
                String newText = getTextEditText(item);
                textEdit = editRange.map(range -> Either.forLeft(new TextEdit(range, newText)),
                    insertReplaceRange -> Either.forRight(new InsertReplaceEdit(newText,
                        insertReplaceRange.getInsert(), insertReplaceRange.getReplace())));
            }
        }
        return textEdit;
    }

    private static String getTextEditText(CompletionItem item)
    {
        String text = item.getTextEditText();
        if (text == null)
            text = item.getLabel();
        return text;
    }

    /**
     * Returns the insert text format for the given completion item.
     * If the completion item itself does not specify a value for this property,
     * the corresponding value from the given completion item defaults will be returned if present.
     *
     * @param item not <code>null</code>
     * @param defaults may be <code>null</code>
     * @return the insert text format for the given completion item, or <code>null</code> if none
     * @see CompletionItem#getInsertTextFormat()
     */
    public static InsertTextFormat getInsertTextFormat(CompletionItem item,
        CompletionItemDefaults defaults)
    {
        InsertTextFormat insertTextFormat = item.getInsertTextFormat();
        if (insertTextFormat == null && defaults != null)
            insertTextFormat = defaults.getInsertTextFormat();
        return insertTextFormat;
    }

    /**
     * Returns the insert text mode for the given completion item.
     * If the completion item itself does not specify a value for this property,
     * the corresponding value from the given completion item defaults will be returned if present.
     *
     * @param item not <code>null</code>
     * @param defaults may be <code>null</code>
     * @return the insert text mode for the given completion item, or <code>null</code> if none
     * @see CompletionItem#getInsertTextMode()
     */
    public static InsertTextMode getInsertTextMode(CompletionItem item,
        CompletionItemDefaults defaults)
    {
        InsertTextMode insertTextMode = item.getInsertTextMode();
        if (insertTextMode == null && defaults != null)
            insertTextMode = defaults.getInsertTextMode();
        return insertTextMode;
    }

    /**
     * Returns the data object for the given completion item.
     * If the completion item itself does not specify a value for this property,
     * the corresponding value from the given completion item defaults will be returned if present.
     *
     * @param item not <code>null</code>
     * @param defaults may be <code>null</code>
     * @return the data object for the given completion item, or <code>null</code> if none
     * @see CompletionItem#getData()
     */
    public static Object getData(CompletionItem item, CompletionItemDefaults defaults)
    {
        Object data = item.getData();
        if (data == null && defaults != null)
            data = defaults.getData();
        return data;
    }

    private CompletionItemUtil()
    {
    }
}

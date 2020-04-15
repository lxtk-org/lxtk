/*******************************************************************************
 * Copyright (c) 2019, 2020 1C-Soft LLC.
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

import java.net.URI;
import java.text.MessageFormat;
import java.util.Arrays;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.buffer.IBuffer;
import org.lxtk.lx4e.internal.Activator;

/**
 * Provides static methods for operating on {@link IUriHandler}s.
 */
public class UriHandlers
{
    /**
     * Returns an URI handler composed of the given handlers.
     *
     * @param handlers not <code>null</code>, must not contain <code>null</code>s
     * @return the composed handler (never <code>null</code>)
     */
    public static IUriHandler compose(IUriHandler... handlers)
    {
        return compose(Arrays.asList(handlers));
    }

    /**
     * Returns an URI handler composed of the given handlers.
     *
     * @param handlers not <code>null</code>, must not contain <code>null</code>s
     * @return the composed handler (never <code>null</code>)
     */
    public static IUriHandler compose(Iterable<? extends IUriHandler> handlers)
    {
        return new IUriHandler()
        {
            @Override
            public Object getCorrespondingElement(URI uri)
            {
                for (IUriHandler handler : handlers)
                {
                    Object element = handler.getCorrespondingElement(uri);
                    if (element != null)
                        return element;
                }
                return null;
            }

            @Override
            public Boolean exists(URI uri)
            {
                for (IUriHandler handler : handlers)
                {
                    Boolean exists = handler.exists(uri);
                    if (exists != null)
                        return exists;
                }
                return null;
            }

            @Override
            public IBuffer getBuffer(URI uri) throws CoreException
            {
                for (IUriHandler handler : handlers)
                {
                    IBuffer buffer = handler.getBuffer(uri);
                    if (buffer != null)
                        return buffer;
                }
                return null;
            }

            @Override
            public String toDisplayString(URI uri)
            {
                for (IUriHandler handler : handlers)
                {
                    String displayString = handler.toDisplayString(uri);
                    if (displayString != null)
                        return displayString;
                }
                return null;
            }
        };
    }

    /**
     * Checks whether the given URI corresponds to an existing resource.
     * <p>
     * Asks the given handler to provide the result by calling {@link
     * IUriHandler#exists(URI)}. If the handler returns <code>null</code>,
     * <code>false</code> is returned.
     * </p>
     *
     * @param uri not <code>null</code>
     * @param uriHandler not <code>null</code>
     * @return <code>true</code> if the corresponding resource exists, and
     *  <code>false</code> if the corresponding resource does not exist or
     *  if there is no corresponding resource
     */
    public static boolean exists(URI uri, IUriHandler uriHandler)
    {
        return Boolean.TRUE.equals(uriHandler.exists(uri));
    }

    /**
     * Returns a buffer that contains text contents of the resource denoted by
     * the given URI.
     * <p>
     * Asks the given handler to provide the result by calling {@link
     * IUriHandler#getBuffer(URI)}. If the handler returns <code>null</code>,
     * a {@link CoreException} is thrown.
     * </p>
     *
     * @param uri not <code>null</code>
     * @param uriHandler not <code>null</code>
     * @return the corresponding buffer (never <code>null</code>).
     *  It is the client responsibility to {@link IBuffer#release() release}
     *  the returned buffer after it is no longer needed
     * @throws CoreException if there is no corresponding buffer or if an
     *  exception occurs while accessing the contents of the corresponding
     *  resource
     */
    public static IBuffer getBuffer(URI uri, IUriHandler uriHandler)
        throws CoreException
    {
        IBuffer buffer = uriHandler.getBuffer(uri);
        if (buffer != null)
            return buffer;
        throw new CoreException(Activator.createErrorStatus(
            MessageFormat.format(Messages.UriHandlers_Cannot_get_buffer,
                toDisplayString(uri, uriHandler))));
    }

    /**
     * Returns a string that identifies the resource denoted by the given URI
     * in a form suitable for displaying to the user, e.g., in message dialogs.
     * <p>
     * Asks the given handler to provide the result by calling {@link
     * IUriHandler#toDisplayString(URI)}. If the handler returns <code>null</code>,
     * a generic representation of the given URI is returned.
     * </p>
     *
     * @param uri not <code>null</code>
     * @param uriHandler not <code>null</code>
     * @return the corresponding string (never <code>null</code>)
     */
    public static String toDisplayString(URI uri, IUriHandler uriHandler)
    {
        String displayString = uriHandler.toDisplayString(uri);
        if (displayString != null)
            return displayString;
        return URIUtil.toDecodedString(uri);
    }

    private UriHandlers()
    {
    }
}

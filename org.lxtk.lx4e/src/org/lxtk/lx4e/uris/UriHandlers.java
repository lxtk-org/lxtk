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
package org.lxtk.lx4e.uris;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Arrays;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.buffer.IBuffer;
import org.lxtk.lx4e.internal.Activator;

/**
 * TODO JavaDoc
 */
public class UriHandlers
{
    /**
     * TODO JavaDoc
     *
     * @param handlers not <code>null</code>, must not contain <code>null</code>
     * @return a composed {@link IUriHandler} (never <code>null</code>)
     */
    public static IUriHandler compose(IUriHandler... handlers)
    {
        return compose(Arrays.asList(handlers));
    }

    /**
     * TODO JavaDoc
     *
     * @param handlers not <code>null</code>, must not contain <code>null</code>
     * @return a composed {@link IUriHandler} (never <code>null</code>)
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
     * TODO JavaDoc
     *
     * @param uri not <code>null</code>
     * @param uriHandler not <code>null</code>
     * @return whether the given URI has contents
     */
    public static boolean exists(URI uri, IUriHandler uriHandler)
    {
        return Boolean.TRUE.equals(uriHandler.exists(uri));
    }

    /**
     * TODO JavaDoc
     *
     * @param uri not <code>null</code>
     * @param uriHandler not <code>null</code>
     * @return a buffer for the given URI (never <code>null</code>).
     *  It is the client responsibility to {@link IBuffer#release() release}
     *  the returned buffer after it is no longer needed
     * @throws CoreException
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
     * TODO JavaDoc
     *
     * @param uri not <code>null</code>
     * @param uriHandler not <code>null</code>
     * @return a string representation of the given URI in a form suitable for
     *  human consumption (never <code>null</code>)
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

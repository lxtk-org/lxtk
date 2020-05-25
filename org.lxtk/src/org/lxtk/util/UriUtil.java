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
package org.lxtk.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Provides static utility methods for URI conversion and normalization.
 */
public class UriUtil
{
    /**
     * Returns a URI corresponding to the given string that is expected
     * to conform to RFC 3986. The returned URI is {@link #normalize(URI)
     * normalized}.
     *
     * @param str a string representation of a URI (not <code>null</code>)
     * @return the created URI (never <code>null</code>)
     * @throws IllegalArgumentException if the string violates RFC 3986
     */
    public static URI fromWireString(String str)
    {
        return normalize(URI.create(str));
    }

    /**
     * Returns a string representation of the URI in a form suitable for
     * transferring over the wire. The returned string is encoded as needed
     * so that it strictly conforms to RFC 3986 and is normalized with regard to
     * {@link Normalization#CASE case} and {@link Normalization#PATH path}.
     *
     * @param uri not <code>null</code>
     * @return a string representation of the URI (never <code>null</code>)
     */
    public static String toWireString(URI uri)
    {
        return normalize(uri, EnumSet.of(Normalization.CASE, Normalization.PATH)).toASCIIString();
    }

    /**
     * Normalizes a URI by applying the whole set of syntax-based {@link
     * Normalization normalizations}. This method has the same effect as
     * calling <code>normalize(uri, Normalization.ALL)</code>.
     *
     * @param uri not <code>null</code>
     * @return a URI equivalent to the given URI, but in a normal form
     * @see #normalize(URI, Set)
     */
    public static URI normalize(URI uri)
    {
        return normalize(uri, Normalization.ALL);
    }

    /**
     * An enumeration of techniques for syntax-based normalization,
     * according to RFC 3986, 6.2.2.
     */
    public static enum Normalization
    {
        /**
         * Case normalization, according to RFC 3986, 6.2.2.1.
         */
        CASE,
        /**
         * Percent-encoding normalization, according to RFC 3986, 6.2.2.2.
         */
        ENCODING,
        /**
         * Path segment normalization, according to RFC 3986, 6.2.2.3.
         */
        PATH;

        /**
         * An unmodifiable set containing all of the {@link Normalization} values.
         */
        public static final Set<Normalization> ALL =
            Collections.unmodifiableSet(EnumSet.allOf(Normalization.class));
    }

    /**
     * Normalizes a URI by applying a set of syntax-based {@link Normalization
     * normalizations}, as described in RFC 3986, 6.2.2.
     *
     * @param uri not <code>null</code>
     * @param normalizations not <code>null</code>
     * @return a URI equivalent to the given URI, but normalized by applying
     *  the given set of normalizations
     */
    public static URI normalize(URI uri, Set<Normalization> normalizations)
    {
        if (normalizations.contains(Normalization.ENCODING))
        {
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (normalizations.contains(Normalization.CASE))
            {
                scheme = toLowerCase(scheme);
                host = toLowerCase(host);
            }
            if (scheme != uri.getScheme() || host != uri.getHost()
                || uri.toString().indexOf('%') >= 0)
            {
                try
                {
                    if (uri.isOpaque())
                        uri = new URI(scheme, uri.getSchemeSpecificPart(), uri.getFragment());
                    else if (host == null)
                        uri = new URI(scheme, uri.getAuthority(), uri.getPath(), uri.getQuery(),
                            uri.getFragment());
                    else
                        uri = new URI(scheme, uri.getUserInfo(), host, uri.getPort(), uri.getPath(),
                            uri.getQuery(), uri.getFragment());
                }
                catch (URISyntaxException e)
                {
                    throw new AssertionError(e); // should never happen
                }
            }
        }
        else if (normalizations.contains(Normalization.CASE))
            uri = normalizeCase(uri);
        if (normalizations.contains(Normalization.PATH))
            uri = uri.normalize();
        return uri;
    }

    private static URI normalizeCase(URI uri)
    {
        String scheme = toLowerCase(uri.getScheme());
        String host = toLowerCase(uri.getHost());

        if (scheme == uri.getScheme() && host == uri.getHost() && uri.toString().indexOf('%') < 0)
            return uri;

        StringBuilder sb = new StringBuilder();
        if (scheme != null)
        {
            sb.append(scheme);
            sb.append(':');
        }
        if (uri.isOpaque())
            sb.append(normalizeEncodingCase(uri.getRawSchemeSpecificPart()));
        else
        {
            if (host != null)
            {
                sb.append("//"); //$NON-NLS-1$
                String userInfo = normalizeEncodingCase(uri.getRawUserInfo());
                if (userInfo != null)
                {
                    sb.append(userInfo);
                    sb.append('@');
                }
                boolean needBrackets =
                    ((host.indexOf(':') >= 0) && !host.startsWith("[") && !host.endsWith("]")); //$NON-NLS-1$ //$NON-NLS-2$
                if (needBrackets)
                    sb.append('[');
                sb.append(host);
                if (needBrackets)
                    sb.append(']');
                int port = uri.getPort();
                if (port != -1)
                {
                    sb.append(':');
                    sb.append(port);
                }
            }
            else
            {
                String authority = normalizeEncodingCase(uri.getRawAuthority());
                if (authority != null)
                {
                    sb.append("//"); //$NON-NLS-1$
                    sb.append(authority);
                }
            }
            String path = normalizeEncodingCase(uri.getRawPath());
            if (path != null)
                sb.append(path);
            String query = normalizeEncodingCase(uri.getRawQuery());
            if (query != null)
            {
                sb.append('?');
                sb.append(query);
            }
        }
        String fragment = normalizeEncodingCase(uri.getRawFragment());
        if (fragment != null)
        {
            sb.append('#');
            sb.append(fragment);
        }
        return URI.create(sb.toString());
    }

    private static String normalizeEncodingCase(String s)
    {
        if (s == null || s.indexOf('%') < 0)
            return s;
        int len = s.length();
        StringBuilder sb = new StringBuilder(len);
        int i = 0;
        while (i < len)
        {
            char c = s.charAt(i++);
            sb.append(c);

            if (c == '%')
            {
                sb.append(toUpperCase(s.charAt(i++)));
                sb.append(toUpperCase(s.charAt(i++)));
            }
        }
        return sb.toString();
    }

    private static String toLowerCase(String s)
    {
        if (s == null)
            return null;
        int len = s.length();
        int i;
        for (i = 0; i < len; i++)
        {
            char c = s.charAt(i);
            if (c != Character.toLowerCase(c))
                break;
        }
        if (i == len)
            return s;
        StringBuilder sb = new StringBuilder(len);
        sb.append(s.substring(0, i));
        for (; i < len; i++)
        {
            sb.append(Character.toLowerCase(s.charAt(i)));
        }
        return sb.toString();
    }

    private static char toUpperCase(char c)
    {
        if ((c >= 'a') && (c <= 'z'))
            return (char)(c - ('a' - 'A'));
        return c;
    }

    private UriUtil()
    {
    }
}

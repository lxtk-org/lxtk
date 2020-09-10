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
package org.lxtk.lx4e;

import java.util.concurrent.Future;

import org.lxtk.LanguageFeatureProvider;

abstract class LanguageFeatureRequest<R extends LanguageFeatureProvider<?>, S, T>
    extends Request<T>
{
    private R provider;
    private S params;

    /**
     * Sets a language feature provider.
     *
     * @param provider a language feature provider
     */
    public void setProvider(R provider)
    {
        this.provider = provider;
    }

    /**
     * Returns the language feature provider.
     *
     * @return the language feature provider
     */
    public R getProvider()
    {
        return provider;
    }

    /**
     * Sets request parameters.
     *
     * @param params request parameters
     */
    public void setParams(S params)
    {
        this.params = params;
    }

    /**
     * Returns request parameters.
     *
     * @return request parameters
     */
    public S getParams()
    {
        return params;
    }

    @Override
    protected final Future<T> send()
    {
        if (provider == null || params == null)
            throw new IllegalStateException();

        return send(provider, params);
    }

    /**
     * Sends a request with the given parameters
     * to the given language feature provider.
     *
     * @param provider never <code>null</code>
     * @param params never <code>null</code>
     * @return the response future (not <code>null</code>)
     */
    protected abstract Future<T> send(R provider, S params);
}

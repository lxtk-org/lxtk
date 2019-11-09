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
package org.lxtk.util.connect;

import java.time.Duration;
import java.util.Objects;

/**
 * TODO JavaDoc
 */
public final class ConnectionMonitor
    implements Runnable
{
    private final Connection connection;
    private long sleepDuration = 100; // ms

    /**
     * TODO JavaDoc
     *
     * @param connection not <code>null</code>
     */
    public ConnectionMonitor(Connection connection)
    {
        this.connection = Objects.requireNonNull(connection);
    }

    /**
     * TODO JavaDoc
     *
     * @param sleepDuration a positive duration
     */
    public void setSleepDuration(Duration sleepDuration)
    {
        if (sleepDuration.isNegative() || sleepDuration.isZero())
            throw new IllegalArgumentException();
        this.sleepDuration = sleepDuration.toMillis();
    }

    @Override
    public void run()
    {
        while (!connection.isClosed())
        {
            try
            {
                Thread.sleep(sleepDuration);
            }
            catch (InterruptedException e)
            {
                // ignore
            }
        }
    }
}

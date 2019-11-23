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
package org.lxtk.lx4e.internal.examples.typescript;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.ApiLevel;
import org.eclipse.handly.context.Context;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.model.IElementDelta;
import org.eclipse.handly.model.IModel;
import org.eclipse.handly.model.impl.IModelImpl;
import org.eclipse.handly.model.impl.support.ElementChangeEvent;
import org.eclipse.handly.model.impl.support.ElementDelta;
import org.eclipse.handly.model.impl.support.ElementManager;
import org.eclipse.handly.model.impl.support.IModelManager;
import org.eclipse.handly.model.impl.support.INotificationManager;
import org.eclipse.handly.model.impl.support.NotificationManager;
import org.eclipse.handly.util.SavedStateJob;
import org.lxtk.lx4e.model.ILanguageElement;
import org.lxtk.lx4e.model.impl.LanguageElementDelta;

/**
 * The manager for the language model.
 */
public final class ModelManager
    implements IModelManager, IResourceChangeListener
{
    /**
     * The sole instance of the manager.
     */
    public static final ModelManager INSTANCE = new ModelManager();

    private IModel model;
    private ElementManager elementManager;
    private NotificationManager notificationManager;
    private Context modelContext;

    void startup()
    {
        try
        {
            model = new IModelImpl()
            {
                @Override
                public IContext getModelContext_()
                {
                    return getModelContext();
                }

                @Override
                public int getModelApiLevel_()
                {
                    return ApiLevel.CURRENT;
                }
            };
            elementManager = new ElementManager(new ModelCache());
            notificationManager = new NotificationManager();

            modelContext = new Context();
            modelContext.bind(INotificationManager.class).to(
                notificationManager);
            modelContext.bind(ElementDelta.Factory.class).to(
                element -> new LanguageElementDelta((ILanguageElement)element));

            ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
                IResourceChangeEvent.POST_CHANGE);

            new SavedStateJob(Activator.PLUGIN_ID, this).schedule();
        }
        catch (RuntimeException e)
        {
            shutdown();
            throw e;
        }
    }

    void shutdown()
    {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        modelContext = null;
        notificationManager = null;
        elementManager = null;
        model = null;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event)
    {
        DeltaProcessor deltaProcessor = new DeltaProcessor();
        try
        {
            event.getDelta().accept(deltaProcessor);
        }
        catch (CoreException e)
        {
            Activator.logError(e);
        }
        IElementDelta[] deltas = deltaProcessor.getDeltas();
        if (deltas.length > 0)
        {
            getNotificationManager().fireElementChangeEvent(
                new ElementChangeEvent(ElementChangeEvent.POST_CHANGE, deltas));
        }
    }

    @Override
    public IModel getModel()
    {
        if (model == null)
            throw new IllegalStateException();
        return model;
    }

    @Override
    public ElementManager getElementManager()
    {
        if (elementManager == null)
            throw new IllegalStateException();
        return elementManager;
    }

    public NotificationManager getNotificationManager()
    {
        if (notificationManager == null)
            throw new IllegalStateException();
        return notificationManager;
    }

    IContext getModelContext()
    {
        if (modelContext == null)
            throw new IllegalStateException();
        return modelContext;
    }

    private ModelManager()
    {
    }
}

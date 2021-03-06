/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterLayout;
import org.eclipse.hawkbit.ui.dd.criteria.DistributionsViewClientCriterion;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

/**
 * Distribution Set Type filter buttons layout.
 */
public class DSTypeFilterLayout extends AbstractFilterLayout {

    private static final long serialVersionUID = 2689002932344750781L;

    private final ManageDistUIState manageDistUIState;

    public DSTypeFilterLayout(final ManageDistUIState manageDistUIState, final I18N i18n,
            final SpPermissionChecker permChecker, final UIEventBus eventBus, final TagManagement tagManagement,
            final EntityFactory entityFactory, final UINotification uiNotification,
            final SoftwareManagement softwareManagement, final DistributionSetManagement distributionSetManagement,
            final DistributionsViewClientCriterion distributionsViewClientCriterion) {
        super(new DSTypeFilterHeader(i18n, permChecker, eventBus, manageDistUIState, tagManagement, entityFactory,
                uiNotification, softwareManagement, distributionSetManagement),
                new DSTypeFilterButtons(eventBus, manageDistUIState, distributionsViewClientCriterion,
                        distributionSetManagement));
        this.manageDistUIState = manageDistUIState;

        restoreState();
        eventBus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final DistributionsUIEvent event) {
        if (event == DistributionsUIEvent.HIDE_DIST_FILTER_BY_TYPE) {
            setVisible(false);
        }
        if (event == DistributionsUIEvent.SHOW_DIST_FILTER_BY_TYPE) {
            setVisible(true);
        }
    }

    @Override
    public Boolean onLoadIsTypeFilterIsClosed() {

        return manageDistUIState.isDistTypeFilterClosed();
    }

}

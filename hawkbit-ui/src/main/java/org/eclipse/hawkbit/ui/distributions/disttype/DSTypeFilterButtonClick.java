/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import java.io.Serializable;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterSingleButtonClick;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.management.event.DistributionTableFilterEvent;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Button;

/**
 * Single button click behaviour of filter buttons layout.
 */
public class DSTypeFilterButtonClick extends AbstractFilterSingleButtonClick implements Serializable {

    private static final long serialVersionUID = -584783755917528648L;

    private final transient EventBus.UIEventBus eventBus;

    private final ManageDistUIState manageDistUIState;

    private final transient DistributionSetManagement distributionSetManagement;

    DSTypeFilterButtonClick(final UIEventBus eventBus, final ManageDistUIState manageDistUIState,
            final DistributionSetManagement distributionSetManagement) {
        this.eventBus = eventBus;
        this.manageDistUIState = manageDistUIState;
        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    protected void filterUnClicked(final Button clickedButton) {
        manageDistUIState.getManageDistFilters().setClickedDistSetType(null);
        eventBus.publish(this, DistributionTableFilterEvent.FILTER_BY_TAG);

    }

    @Override
    protected void filterClicked(final Button clickedButton) {
        final DistributionSetType distSetType = distributionSetManagement
                .findDistributionSetTypeByName(clickedButton.getData().toString());
        manageDistUIState.getManageDistFilters().setClickedDistSetType(distSetType);
        eventBus.publish(this, DistributionTableFilterEvent.FILTER_BY_TAG);

    }

}

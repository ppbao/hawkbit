/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tagdetails;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

/**
 * Implementation of Target tag token.
 *
 *
 */
public class TargetTagToken extends AbstractTargetTagToken<Target> {

    private static final long serialVersionUID = 7124887018280196721L;

    // To Be Done : have to set this value based on view???
    private static final Boolean NOTAGS_SELECTED = Boolean.FALSE;

    private final transient TargetManagement targetManagement;

    public TargetTagToken(final SpPermissionChecker checker, final I18N i18n, final UINotification uinotification,
            final UIEventBus eventBus, final ManagementUIState managementUIState, final TagManagement tagManagement,
            final TargetManagement targetManagement) {
        super(checker, i18n, uinotification, eventBus, managementUIState, tagManagement);
        this.targetManagement = targetManagement;
    }

    @Override
    protected String getTagStyleName() {
        return "target-tag-";
    }

    @Override
    protected String getTokenInputPrompt() {
        return i18n.get("combo.type.tag.name");
    }

    @Override
    protected void assignTag(final String tagNameSelected) {
        if (tagNameSelected != null) {
            final TargetTagAssignmentResult result = toggleAssignment(tagNameSelected);
            if (result.getAssigned() >= 1 && NOTAGS_SELECTED) {
                eventBus.publish(this, ManagementUIEvent.ASSIGN_TARGET_TAG);
            }
        } else {
            uinotification.displayValidationError(i18n.get("message.error.missing.tagname"));
        }
    }

    private TargetTagAssignmentResult toggleAssignment(final String tagNameSelected) {
        final Set<String> targetList = new HashSet<>();
        targetList.add(selectedEntity.getControllerId());
        final TargetTagAssignmentResult result = targetManagement.toggleTagAssignment(targetList, tagNameSelected);
        processTargetTagAssigmentResult(result);
        uinotification.displaySuccess(HawkbitCommonUtil.createAssignmentMessage(tagNameSelected, result, i18n));
        return result;
    }

    @Override
    protected void unassignTag(final String tagName) {
        final TargetTagAssignmentResult result = toggleAssignment(tagName);
        if (result.getUnassigned() >= 1) {
            eventBus.publish(this, ManagementUIEvent.UNASSIGN_TARGET_TAG);
        }
    }

    @Override
    protected Boolean isToggleTagAssignmentAllowed() {
        return checker.hasUpdateTargetPermission();
    }

    @Override
    protected void displayAlreadyAssignedTags() {
        removePreviouslyAddedTokens();
        if (selectedEntity != null) {
            for (final TargetTag tag : selectedEntity.getTags()) {
                addNewToken(tag.getId());
            }
        }
    }

    @Override
    protected void populateContainer() {
        container.removeAllItems();
        tagDetails.clear();
        for (final TargetTag tag : tagManagement.findAllTargetTags()) {
            setContainerPropertValues(tag.getId(), tag.getName(), tag.getColour());
        }
    }

    /**
     * 
     * @param assignmentResult
     */
    public void processTargetTagAssigmentResult(final TargetTagAssignmentResult assignmentResult) {
        final TargetTag targetTag = assignmentResult.getTargetTag();
        if (isAssign(assignmentResult)) {
            addNewToken(targetTag.getId());
        } else if (isUnassign(assignmentResult)) {
            removeTokenItem(targetTag.getId(), targetTag.getName());
        }
    }

    protected boolean isAssign(final TargetTagAssignmentResult assignmentResult) {
        if (assignmentResult.getAssigned() > 0 && managementUIState.getLastSelectedTargetId() != null) {
            return assignmentResult.getAssignedEntity().stream().map(t -> t.getId())
                    .anyMatch(controllerId -> controllerId.equals(managementUIState.getLastSelectedTargetId()));
        }
        return false;
    }

    protected boolean isUnassign(final TargetTagAssignmentResult assignmentResult) {
        if (assignmentResult.getUnassigned() > 0 && managementUIState.getLastSelectedTargetId() != null) {
            return assignmentResult.getUnassignedEntity().stream().map(t -> t.getId())
                    .anyMatch(controllerId -> controllerId.equals(managementUIState.getLastSelectedTargetId()));
        }
        return false;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final TargetTableEvent targetTableEvent) {
        onBaseEntityEvent(targetTableEvent);
    }

}

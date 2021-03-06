/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.builder.AbstractSoftwareModuleUpdateCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.exception.ConstraintViolationException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Create/build implementation.
 *
 */
public class JpaSoftwareModuleCreate extends AbstractSoftwareModuleUpdateCreate<SoftwareModuleCreate>
        implements SoftwareModuleCreate {

    private final SoftwareManagement softwareManagement;

    JpaSoftwareModuleCreate(final SoftwareManagement softwareManagement) {
        this.softwareManagement = softwareManagement;
    }

    @Override
    public JpaSoftwareModule build() {
        return new JpaSoftwareModule(getSoftwareModuleTypeFromKeyString(type), name, version, description, vendor);
    }

    private SoftwareModuleType getSoftwareModuleTypeFromKeyString(final String type) {
        if (type == null) {
            throw new ConstraintViolationException("type cannot be null");
        }

        final SoftwareModuleType smType = softwareManagement.findSoftwareModuleTypeByKey(type.trim());

        if (smType == null) {
            throw new EntityNotFoundException(type.trim());
        }

        return smType;
    }
}

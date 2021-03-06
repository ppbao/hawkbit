/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static com.google.common.collect.Iterables.limit;
import static com.google.common.collect.Iterables.toArray;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolationException;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.TenantNotExistException;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.WithSpringAuthorityRule;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Target Management")
public class TargetManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Ensures that retrieving the target security is only permitted with the necessary permissions.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1) })
    public void getTargetSecurityTokenOnlyWithCorrectPermission() throws Exception {
        final Target createdTarget = targetManagement.createTarget(
                entityFactory.target().create().controllerId("targetWithSecurityToken").securityToken("token"));

        // retrieve security token only with READ_TARGET_SEC_TOKEN permission
        final String securityTokenWithReadPermission = securityRule.runAs(WithSpringAuthorityRule
                .withUser("OnlyTargetReadPermission", false, SpPermission.READ_TARGET_SEC_TOKEN.toString()), () -> {
                    return createdTarget.getSecurityToken();
                });

        // retrieve security token as system code execution
        final String securityTokenAsSystemCode = systemSecurityContext.runAsSystem(() -> {
            return createdTarget.getSecurityToken();
        });

        // retrieve security token without any permissions
        final String securityTokenWithoutPermission = securityRule
                .runAs(WithSpringAuthorityRule.withUser("NoPermission", false), () -> {
                    return createdTarget.getSecurityToken();
                });

        assertThat(createdTarget.getSecurityToken()).isEqualTo("token");
        assertThat(securityTokenWithReadPermission).isNotNull();
        assertThat(securityTokenAsSystemCode).isNotNull();

        assertThat(securityTokenWithoutPermission).isNull();
    }

    @Test
    @Description("Ensures that targets cannot be created e.g. in plug'n play scenarios when tenant does not exists.")
    @WithUser(tenantId = "tenantWhichDoesNotExists", allSpPermissions = true, autoCreateTenant = false)
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void createTargetForTenantWhichDoesNotExistThrowsTenantNotExistException() {
        try {
            targetManagement.createTarget(entityFactory.target().create().controllerId("targetId123"));
            fail("should not be possible as the tenant does not exist");
        } catch (final TenantNotExistException e) {
            // ok
        }
    }

    @Test
    @Description("Verify that a target with empty controller id cannot be created")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void createTargetWithNoControllerId() {
        try {
            targetManagement.createTarget(entityFactory.target().create().controllerId(""));
            fail("target with empty controller id should not be created");
        } catch (final ConstraintViolationException e) {
            // ok
        }

        try {
            targetManagement.createTarget(entityFactory.target().create().controllerId(null));
            fail("target with empty controller id should not be created");
        } catch (final ConstraintViolationException e) {
            // ok
        }
    }

    @Test
    @Description("Verify that a target with whitespaces in controller id cannot be created")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void createTargetWithWhitespaces() {
        try {
            targetManagement.createTarget(entityFactory.target().create().controllerId(" "));
            fail("target with whitespaces in controller id should not be created");
        } catch (final ConstraintViolationException e) {
            // ok
        }

        try {
            targetManagement.createTarget(entityFactory.target().create().controllerId(" a"));
            fail("target with whitespaces in controller id should not be created");
        } catch (final ConstraintViolationException e) {
            // ok
        }

        try {
            targetManagement.createTarget(entityFactory.target().create().controllerId("a "));
            fail("target with whitespaces in controller id should not be created");
        } catch (final ConstraintViolationException e) {
            // ok
        }

        try {
            targetManagement.createTarget(entityFactory.target().create().controllerId("a b"));
            fail("target with whitespaces in controller id should not be created");
        } catch (final ConstraintViolationException e) {
            // ok
        }

        try {
            targetManagement.createTarget(entityFactory.target().create().controllerId("     "));
            fail("target with whitespaces in controller id should not be created");
        } catch (final ConstraintViolationException e) {
            // ok
        }

        try {
            targetManagement.createTarget(entityFactory.target().create().controllerId("aaa   bbb"));
            fail("target with whitespaces in controller id should not be created");
        } catch (final ConstraintViolationException e) {
            // ok
        }

    }

    @Test
    @Description("Ensures that targets can assigned and unassigned to a target tag. Not exists target will be ignored for the assignment.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 4),
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 8) })
    public void assignAndUnassignTargetsToTag() {
        final List<String> assignTarget = new ArrayList<>();
        assignTarget.add(targetManagement.createTarget(entityFactory.target().create().controllerId("targetId123"))
                .getControllerId());
        assignTarget.add(targetManagement.createTarget(entityFactory.target().create().controllerId("targetId1234"))
                .getControllerId());
        assignTarget.add(targetManagement.createTarget(entityFactory.target().create().controllerId("targetId1235"))
                .getControllerId());
        assignTarget.add(targetManagement.createTarget(entityFactory.target().create().controllerId("targetId1236"))
                .getControllerId());
        assignTarget.add("NotExist");

        final TargetTag targetTag = tagManagement.createTargetTag(entityFactory.tag().create().name("Tag1"));

        final List<Target> assignedTargets = targetManagement.assignTag(assignTarget, targetTag.getId());
        assertThat(assignedTargets.size()).as("Assigned targets are wrong").isEqualTo(4);
        assignedTargets.forEach(target -> assertThat(target.getTags().size()).isEqualTo(1));

        TargetTag findTargetTag = tagManagement.findTargetTag("Tag1");
        assertThat(assignedTargets.size()).as("Assigned targets are wrong")
                .isEqualTo(findTargetTag.getAssignedToTargets().size());

        assertThat(targetManagement.unAssignTag("NotExist", findTargetTag.getId())).as("Unassign target does not work")
                .isNull();

        final Target unAssignTarget = targetManagement.unAssignTag("targetId123", findTargetTag.getId());
        assertThat(unAssignTarget.getControllerId()).as("Controller id is wrong").isEqualTo("targetId123");
        assertThat(unAssignTarget.getTags()).as("Tag size is wrong").isEmpty();
        findTargetTag = tagManagement.findTargetTag("Tag1");
        assertThat(findTargetTag.getAssignedToTargets()).as("Assigned targets are wrong").hasSize(3);

        final List<Target> unAssignTargets = targetManagement.unAssignAllTargetsByTag(findTargetTag.getId());
        findTargetTag = tagManagement.findTargetTag("Tag1");
        assertThat(findTargetTag.getAssignedToTargets()).as("Unassigned targets are wrong").isEmpty();
        assertThat(unAssignTargets).as("Unassigned targets are wrong").hasSize(3);
        unAssignTargets.forEach(target -> assertThat(target.getTags().size()).isEqualTo(0));
    }

    @Test
    @Description("Ensures that targets can deleted e.g. test all cascades")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 12),
            @Expect(type = TargetDeletedEvent.class, count = 12), @Expect(type = TargetUpdatedEvent.class, count = 6) })
    public void deleteAndCreateTargets() {
        Target target = targetManagement.createTarget(entityFactory.target().create().controllerId("targetId123"));
        assertThat(targetManagement.countTargetsAll()).as("target count is wrong").isEqualTo(1);
        targetManagement.deleteTargets(Lists.newArrayList(target.getId()));
        assertThat(targetManagement.countTargetsAll()).as("target count is wrong").isEqualTo(0);

        target = createTargetWithAttributes("4711");
        assertThat(targetManagement.countTargetsAll()).as("target count is wrong").isEqualTo(1);
        targetManagement.deleteTargets(Lists.newArrayList(target.getId()));
        assertThat(targetManagement.countTargetsAll()).as("target count is wrong").isEqualTo(0);

        final List<Long> targets = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            target = targetManagement.createTarget(entityFactory.target().create().controllerId("" + i));
            targets.add(target.getId());
            targets.add(createTargetWithAttributes("" + (i * i + 1000)).getId());
        }
        assertThat(targetManagement.countTargetsAll()).as("target count is wrong").isEqualTo(10);
        targetManagement.deleteTargets(targets);
        assertThat(targetManagement.countTargetsAll()).as("target count is wrong").isEqualTo(0);
    }

    private Target createTargetWithAttributes(final String controllerId) {
        final Map<String, String> testData = new HashMap<>();
        testData.put("test1", "testdata1");

        targetManagement.createTarget(entityFactory.target().create().controllerId(controllerId));
        controllerManagament.updateControllerAttributes(controllerId, testData);

        final Target target = targetManagement.findTargetByControllerIDWithDetails(controllerId);
        assertThat(target.getTargetInfo().getControllerAttributes()).as("Controller Attributes are wrong")
                .isEqualTo(testData);
        return target;
    }

    @Test
    @Description("Finds a target by given ID and checks if all data is in the reponse (including the data defined as lazy).")
    @ExpectEvents({ @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = TargetCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 5),
            @Expect(type = ActionCreatedEvent.class, count = 2), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6) })
    public void findTargetByControllerIDWithDetails() {
        final DistributionSet set = testdataFactory.createDistributionSet("test");
        final DistributionSet set2 = testdataFactory.createDistributionSet("test2");

        assertThat(targetManagement.countTargetByAssignedDistributionSet(set.getId())).as("Target count is wrong")
                .isEqualTo(0);
        assertThat(targetManagement.countTargetByInstalledDistributionSet(set.getId())).as("Target count is wrong")
                .isEqualTo(0);
        assertThat(targetManagement.countTargetByAssignedDistributionSet(set2.getId())).as("Target count is wrong")
                .isEqualTo(0);
        assertThat(targetManagement.countTargetByInstalledDistributionSet(set2.getId())).as("Target count is wrong")
                .isEqualTo(0);

        Target target = createTargetWithAttributes("4711");

        final long current = System.currentTimeMillis();
        controllerManagament.updateLastTargetQuery("4711", null);

        final DistributionSetAssignmentResult result = assignDistributionSet(set.getId(), "4711");

        controllerManagament.addUpdateActionStatus(
                entityFactory.actionStatus().create(result.getActions().get(0)).status(Status.FINISHED));
        assignDistributionSet(set2.getId(), "4711");

        target = targetManagement.findTargetByControllerIDWithDetails("4711");
        // read data

        assertThat(targetManagement.countTargetByAssignedDistributionSet(set.getId())).as("Target count is wrong")
                .isEqualTo(0);
        assertThat(targetManagement.countTargetByInstalledDistributionSet(set.getId())).as("Target count is wrong")
                .isEqualTo(1);
        assertThat(targetManagement.countTargetByAssignedDistributionSet(set2.getId())).as("Target count is wrong")
                .isEqualTo(1);
        assertThat(targetManagement.countTargetByInstalledDistributionSet(set2.getId())).as("Target count is wrong")
                .isEqualTo(0);
        assertThat(target.getTargetInfo().getLastTargetQuery()).as("Target query is not work")
                .isGreaterThanOrEqualTo(current);
        assertThat(target.getAssignedDistributionSet()).as("Assigned ds size is wrong").isEqualTo(set2);
        assertThat(target.getTargetInfo().getInstalledDistributionSet().getId()).as("Installed ds is wrong")
                .isEqualTo(set.getId());
    }

    @Test
    @Description("Ensures that repositoy returns null if given controller ID does not exist without exception.")
    public void findTargetByControllerIDWithDetailsReturnsNullForNonexisting() {
        assertThat(targetManagement.findTargetByControllerIDWithDetails("dsfsdfsdfsd")).as("Expected as").isNull();
    }

    @Test
    @Description("Checks if the EntityAlreadyExistsException is thrown if the targets with the same controller ID are created twice.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 5) })
    public void createMultipleTargetsDuplicate() {
        testdataFactory.createTargets(5, "mySimpleTargs", "my simple targets");
        try {
            testdataFactory.createTargets(5, "mySimpleTargs", "my simple targets");
            fail("Targets already exists");
        } catch (final EntityAlreadyExistsException e) {
        }

    }

    @Test
    @Description("Checks if the EntityAlreadyExistsException is thrown if a single target with the same controller ID are created twice.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1) })
    public void createTargetDuplicate() {
        targetManagement.createTarget(entityFactory.target().create().controllerId("4711"));
        try {
            targetManagement.createTarget(entityFactory.target().create().controllerId("4711"));
            fail("Target already exists");
        } catch (final EntityAlreadyExistsException e) {
        }
    }

    /**
     * verifies, that all {@link TargetTag} of parameter. NOTE: it's accepted
     * that the target have additional tags assigned to them which are not
     * contained within parameter tags.
     *
     * @param strict
     *            if true, the given targets MUST contain EXACTLY ALL given
     *            tags, AND NO OTHERS. If false, the given targets MUST contain
     *            ALL given tags, BUT MAY CONTAIN FURTHER ONE
     * @param targets
     *            targets to be verified
     * @param tags
     *            are contained within tags of all targets.
     * @param tags
     *            to be found in the tags of the targets
     */
    private void checkTargetHasTags(final boolean strict, final Iterable<Target> targets, final TargetTag... tags) {
        _target: for (final Target tl : targets) {
            final Target t = targetManagement.findTargetByControllerID(tl.getControllerId());

            for (final Tag tt : t.getTags()) {
                for (final Tag tag : tags) {
                    if (tag.getName().equals(tt.getName())) {
                        continue _target;
                    }
                }
                if (strict) {
                    fail("Target does not contain all tags");
                }
            }
            fail("Target does not contain any tags or the expected tag was not found");
        }
    }

    private void checkTargetHasNotTags(final Iterable<Target> targets, final TargetTag... tags) {
        for (final Target tl : targets) {
            final Target t = targetManagement.findTargetByControllerID(tl.getControllerId());

            for (final Tag tag : tags) {
                for (final Tag tt : t.getTags()) {
                    if (tag.getName().equals(tt.getName())) {
                        fail("Target should have no tags");
                    }
                }
            }
        }
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Creates and updates a target and verifies the changes in the repository.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1) })
    public void singleTargetIsInsertedIntoRepo() throws Exception {

        final String myCtrlID = "myCtrlID";

        Target savedTarget = testdataFactory.createTarget(myCtrlID);
        assertNotNull("The target should not be null", savedTarget);
        final Long createdAt = savedTarget.getCreatedAt();
        Long modifiedAt = savedTarget.getLastModifiedAt();

        assertThat(createdAt).as("CreatedAt compared with modifiedAt").isEqualTo(modifiedAt);
        assertNotNull("The createdAt attribut of the target should no be null", savedTarget.getCreatedAt());
        assertNotNull("The lastModifiedAt attribut of the target should no be null", savedTarget.getLastModifiedAt());

        Thread.sleep(1);
        savedTarget = targetManagement.updateTarget(
                entityFactory.target().update(savedTarget.getControllerId()).description("changed description"));
        assertNotNull("The lastModifiedAt attribute of the target should not be null", savedTarget.getLastModifiedAt());
        assertThat(createdAt).as("CreatedAt compared with saved modifiedAt")
                .isNotEqualTo(savedTarget.getLastModifiedAt());
        assertThat(modifiedAt).as("ModifiedAt compared with saved modifiedAt")
                .isNotEqualTo(savedTarget.getLastModifiedAt());
        modifiedAt = savedTarget.getLastModifiedAt();

        final Target foundTarget = targetManagement.findTargetByControllerID(savedTarget.getControllerId());
        assertNotNull("The target should not be null", foundTarget);
        assertThat(myCtrlID).as("ControllerId compared with saved controllerId")
                .isEqualTo(foundTarget.getControllerId());
        assertThat(savedTarget).as("Target compared with saved target").isEqualTo(foundTarget);
        assertThat(createdAt).as("CreatedAt compared with saved createdAt").isEqualTo(foundTarget.getCreatedAt());
        assertThat(modifiedAt).as("LastModifiedAt compared with saved lastModifiedAt")
                .isEqualTo(foundTarget.getLastModifiedAt());
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Create multiple tragets as bulk operation and delete them in bulk.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 101),
            @Expect(type = TargetUpdatedEvent.class, count = 100),
            @Expect(type = TargetDeletedEvent.class, count = 51) })
    public void bulkTargetCreationAndDelete() throws Exception {
        final String myCtrlID = "myCtrlID";
        List<Target> firstList = testdataFactory.createTargets(100, myCtrlID, "first description");

        final Target extra = testdataFactory.createTarget("myCtrlID-00081XX");

        final Iterable<JpaTarget> allFound = targetRepository.findAll();

        assertThat(Long.valueOf(firstList.size())).as("List size of targets")
                .isEqualTo(firstList.spliterator().getExactSizeIfKnown());
        assertThat(Long.valueOf(firstList.size() + 1)).as("LastModifiedAt compared with saved lastModifiedAt")
                .isEqualTo(allFound.spliterator().getExactSizeIfKnown());

        // change the objects and save to again to trigger a change on
        // lastModifiedAt
        firstList = firstList.stream()
                .map(t -> targetManagement.updateTarget(
                        entityFactory.target().update(t.getControllerId()).name(t.getName().concat("\tchanged"))))
                .collect(toList());

        // verify that all entries are found
        _founds: for (final Target foundTarget : allFound) {
            for (final Target changedTarget : firstList) {
                if (changedTarget.getControllerId().equals(foundTarget.getControllerId())) {
                    assertThat(changedTarget.getDescription())
                            .as("Description of changed target compared with description saved target")
                            .isEqualTo(foundTarget.getDescription());
                    assertThat(changedTarget.getName()).as("Name of changed target starts with name of saved target")
                            .startsWith(foundTarget.getName());
                    assertThat(changedTarget.getName()).as("Name of changed target ends with 'changed'")
                            .endsWith("changed");
                    assertThat(changedTarget.getCreatedAt()).as("CreatedAt compared with saved createdAt")
                            .isEqualTo(foundTarget.getCreatedAt());
                    assertThat(changedTarget.getLastModifiedAt()).as("LastModifiedAt compared with saved createdAt")
                            .isNotEqualTo(changedTarget.getCreatedAt());
                    continue _founds;
                }
            }

            if (!foundTarget.getControllerId().equals(extra.getControllerId())) {
                fail("The controllerId of the found target is not equal to the controllerId of the saved target");
            }
        }

        targetManagement.deleteTarget(extra.getControllerId());

        final int numberToDelete = 50;
        final Iterable<Target> targetsToDelete = limit(firstList, numberToDelete);
        final Target[] deletedTargets = toArray(targetsToDelete, Target.class);
        final List<Long> targetsIdsToDelete = newArrayList(targetsToDelete.iterator()).stream().map(Target::getId)
                .collect(toList());

        targetManagement.deleteTargets(targetsIdsToDelete);

        final List<Target> targetsLeft = targetManagement.findTargetsAll(new PageRequest(0, 200)).getContent();
        assertThat(firstList.spliterator().getExactSizeIfKnown() - numberToDelete).as("Size of splited list")
                .isEqualTo(targetsLeft.spliterator().getExactSizeIfKnown());

        assertThat(targetsLeft).as("Not all undeleted found").doesNotContain(deletedTargets);
    }

    @Test
    @Description("Tests the assigment of tags to the a single target.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = TargetTagCreatedEvent.class, count = 7),
            @Expect(type = TargetUpdatedEvent.class, count = 7) })
    public void targetTagAssignment() {
        final Target t1 = testdataFactory.createTarget("id-1");
        final int noT2Tags = 4;
        final int noT1Tags = 3;
        final List<TargetTag> t1Tags = testdataFactory.createTargetTags(noT1Tags, "tag1");

        t1Tags.forEach(tag -> targetManagement.assignTag(Lists.newArrayList(t1.getControllerId()), tag.getId()));

        final Target t2 = testdataFactory.createTarget("id-2");
        final List<TargetTag> t2Tags = testdataFactory.createTargetTags(noT2Tags, "tag2");
        t2Tags.forEach(tag -> targetManagement.assignTag(Lists.newArrayList(t2.getControllerId()), tag.getId()));

        final Target t11 = targetManagement.findTargetByControllerID(t1.getControllerId());
        assertThat(t11.getTags()).as("Tag size is wrong").hasSize(noT1Tags).containsAll(t1Tags);
        assertThat(t11.getTags()).as("Tag size is wrong").hasSize(noT1Tags)
                .doesNotContain(Iterables.toArray(t2Tags, TargetTag.class));

        final Target t21 = targetManagement.findTargetByControllerID(t2.getControllerId());
        assertThat(t21.getTags()).as("Tag size is wrong").hasSize(noT2Tags).containsAll(t2Tags);
        assertThat(t21.getTags()).as("Tag size is wrong").hasSize(noT2Tags)
                .doesNotContain(Iterables.toArray(t1Tags, TargetTag.class));
    }

    @Test
    @Description("Tests the assigment of tags to multiple targets.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 50),
            @Expect(type = TargetTagCreatedEvent.class, count = 4),
            @Expect(type = TargetUpdatedEvent.class, count = 80) })
    public void targetTagBulkAssignments() {
        final List<Target> tagATargets = testdataFactory.createTargets(10, "tagATargets", "first description");
        final List<Target> tagBTargets = testdataFactory.createTargets(10, "tagBTargets", "first description");
        final List<Target> tagCTargets = testdataFactory.createTargets(10, "tagCTargets", "first description");

        final List<Target> tagABTargets = testdataFactory.createTargets(10, "tagABTargets", "first description");

        final List<Target> tagABCTargets = testdataFactory.createTargets(10, "tagABCTargets", "first description");

        final TargetTag tagA = tagManagement.createTargetTag(entityFactory.tag().create().name("A"));
        final TargetTag tagB = tagManagement.createTargetTag(entityFactory.tag().create().name("B"));
        final TargetTag tagC = tagManagement.createTargetTag(entityFactory.tag().create().name("C"));
        tagManagement.createTargetTag(entityFactory.tag().create().name("X"));

        // doing different assignments
        toggleTagAssignment(tagATargets, tagA);
        toggleTagAssignment(tagBTargets, tagB);
        toggleTagAssignment(tagCTargets, tagC);

        toggleTagAssignment(tagABTargets, tagA);
        toggleTagAssignment(tagABTargets, tagB);

        toggleTagAssignment(tagABCTargets, tagA);
        toggleTagAssignment(tagABCTargets, tagB);
        toggleTagAssignment(tagABCTargets, tagC);

        assertThat(targetManagement.countTargetByFilters(null, null, null, null, Boolean.FALSE, "X"))
                .as("Target count is wrong").isEqualTo(0);

        // search for targets with tag tagA
        final List<Target> targetWithTagA = new ArrayList<>();
        final List<Target> targetWithTagB = new ArrayList<>();
        final List<Target> targetWithTagC = new ArrayList<>();

        // storing target lists to enable easy evaluation
        Iterables.addAll(targetWithTagA, tagATargets);
        Iterables.addAll(targetWithTagA, tagABTargets);
        Iterables.addAll(targetWithTagA, tagABCTargets);

        Iterables.addAll(targetWithTagB, tagBTargets);
        Iterables.addAll(targetWithTagB, tagABTargets);
        Iterables.addAll(targetWithTagB, tagABCTargets);

        Iterables.addAll(targetWithTagC, tagCTargets);
        Iterables.addAll(targetWithTagC, tagABCTargets);

        // check the target lists as returned by assignTag
        checkTargetHasTags(false, targetWithTagA, tagA);
        checkTargetHasTags(false, targetWithTagB, tagB);
        checkTargetHasTags(false, targetWithTagC, tagC);

        checkTargetHasNotTags(tagATargets, tagB, tagC);
        checkTargetHasNotTags(tagBTargets, tagA, tagC);
        checkTargetHasNotTags(tagCTargets, tagA, tagB);

        // check again target lists refreshed from DB
        assertThat(targetManagement.countTargetByFilters(null, null, null, null, Boolean.FALSE, "A"))
                .as("Target count is wrong").isEqualTo(targetWithTagA.size());
        assertThat(targetManagement.countTargetByFilters(null, null, null, null, Boolean.FALSE, "B"))
                .as("Target count is wrong").isEqualTo(targetWithTagB.size());
        assertThat(targetManagement.countTargetByFilters(null, null, null, null, Boolean.FALSE, "C"))
                .as("Target count is wrong").isEqualTo(targetWithTagC.size());
    }

    @Test
    @Description("Tests the unassigment of tags to multiple targets.")
    @ExpectEvents({ @Expect(type = TargetTagCreatedEvent.class, count = 3),
            @Expect(type = TargetCreatedEvent.class, count = 109),
            @Expect(type = TargetUpdatedEvent.class, count = 227) })
    public void targetTagBulkUnassignments() {
        final TargetTag targTagA = tagManagement.createTargetTag(entityFactory.tag().create().name("Targ-A-Tag"));
        final TargetTag targTagB = tagManagement.createTargetTag(entityFactory.tag().create().name("Targ-B-Tag"));
        final TargetTag targTagC = tagManagement.createTargetTag(entityFactory.tag().create().name("Targ-C-Tag"));

        final List<Target> targAs = testdataFactory.createTargets(25, "target-id-A", "first description");
        final List<Target> targBs = testdataFactory.createTargets(20, "target-id-B", "first description");
        final List<Target> targCs = testdataFactory.createTargets(15, "target-id-C", "first description");

        final List<Target> targABs = testdataFactory.createTargets(12, "target-id-AB", "first description");
        final List<Target> targACs = testdataFactory.createTargets(13, "target-id-AC", "first description");
        final List<Target> targBCs = testdataFactory.createTargets(7, "target-id-BC", "first description");
        final List<Target> targABCs = testdataFactory.createTargets(17, "target-id-ABC", "first description");

        toggleTagAssignment(targAs, targTagA);
        toggleTagAssignment(targABs, targTagA);
        toggleTagAssignment(targACs, targTagA);
        toggleTagAssignment(targABCs, targTagA);

        toggleTagAssignment(targBs, targTagB);
        toggleTagAssignment(targABs, targTagB);
        toggleTagAssignment(targBCs, targTagB);
        toggleTagAssignment(targABCs, targTagB);

        toggleTagAssignment(targCs, targTagC);
        toggleTagAssignment(targACs, targTagC);
        toggleTagAssignment(targBCs, targTagC);
        toggleTagAssignment(targABCs, targTagC);

        checkTargetHasTags(true, targAs, targTagA);
        checkTargetHasTags(true, targBs, targTagB);
        checkTargetHasTags(true, targABs, targTagA, targTagB);
        checkTargetHasTags(true, targACs, targTagA, targTagC);
        checkTargetHasTags(true, targBCs, targTagB, targTagC);
        checkTargetHasTags(true, targABCs, targTagA, targTagB, targTagC);

        toggleTagAssignment(targCs, targTagC);
        toggleTagAssignment(targACs, targTagC);
        toggleTagAssignment(targBCs, targTagC);
        toggleTagAssignment(targABCs, targTagC);

        checkTargetHasTags(true, targAs, targTagA); // 0
        checkTargetHasTags(true, targBs, targTagB);
        checkTargetHasTags(true, targABs, targTagA, targTagB);
        checkTargetHasTags(true, targBCs, targTagB);
        checkTargetHasTags(true, targACs, targTagA);

        checkTargetHasNotTags(targCs, targTagC);
        checkTargetHasNotTags(targACs, targTagC);
        checkTargetHasNotTags(targBCs, targTagC);
        checkTargetHasNotTags(targABCs, targTagC);
    }

    @Test
    @Description("Retrieves targets by ID with lazy loading of the tags. Checks the successfull load.")
    @ExpectEvents({ @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 25),
            @Expect(type = TargetUpdatedEvent.class, count = 25) })
    public void findTargetsByControllerIDsWithTags() {
        final TargetTag targTagA = tagManagement.createTargetTag(entityFactory.tag().create().name("Targ-A-Tag"));

        final List<Target> targAs = testdataFactory.createTargets(25, "target-id-A", "first description");

        toggleTagAssignment(targAs, targTagA);

        assertThat(targetManagement
                .findTargetsByControllerIDsWithTags(targAs.stream().map(Target::getControllerId).collect(toList())))
                        .as("Target count is wrong").hasSize(25);

        // no lazy loading exception and tag correctly assigned
        assertThat(targetManagement
                .findTargetsByControllerIDsWithTags(targAs.stream().map(Target::getControllerId).collect(toList()))
                .stream().map(target -> target.getTags().contains(targTagA)).collect(toList()))
                        .as("Tags not correctly assigned").containsOnly(true);
    }

    @Test
    @Description("Test that NO TAG functionality which gives all targets with no tag assigned.")
    @ExpectEvents({ @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 50),
            @Expect(type = TargetUpdatedEvent.class, count = 25) })
    public void findTargetsWithNoTag() {

        final TargetTag targTagA = tagManagement.createTargetTag(entityFactory.tag().create().name("Targ-A-Tag"));
        final List<Target> targAs = testdataFactory.createTargets(25, "target-id-A", "first description");
        toggleTagAssignment(targAs, targTagA);

        testdataFactory.createTargets(25, "target-id-B", "first description");

        final String[] tagNames = null;
        final List<Target> targetsListWithNoTag = targetManagement
                .findTargetByFilters(pageReq, null, null, null, null, Boolean.TRUE, tagNames).getContent();

        assertThat(50L).as("Total targets").isEqualTo(targetManagement.countTargetsAll());
        assertThat(25).as("Targets with no tag").isEqualTo(targetsListWithNoTag.size());

    }

    @Test
    @Description("Tests the a target can be read with only the read target permission")
    @Expect(type = TargetCreatedEvent.class, count = 0)
    public void targetCanBeReadWithOnlyReadTargetPermission() throws Exception {
        final String knownTargetControllerId = "readTarget";
        controllerManagament.findOrRegisterTargetIfItDoesNotexist(knownTargetControllerId, new URI("http://127.0.0.1"));

        securityRule.runAs(WithSpringAuthorityRule.withUser("bumlux", "READ_TARGET"), () -> {
            final Target findTargetByControllerID = targetManagement.findTargetByControllerID(knownTargetControllerId);
            assertThat(findTargetByControllerID).isNotNull();
            assertThat(findTargetByControllerID.getTargetInfo()).isNotNull();
            assertThat(findTargetByControllerID.getTargetInfo().getPollStatus()).isNotNull();
            return null;
        });

    }

    @Test
    @Description("Test that RSQL filter finds targets with tags or specific ids.")
    public void findTargetsWithTagOrId() {
        final String rsqlFilter = "tag==Targ-A-Tag,id==target-id-B-00001,id==target-id-B-00008";
        final TargetTag targTagA = tagManagement.createTargetTag(entityFactory.tag().create().name("Targ-A-Tag"));
        final List<String> targAs = testdataFactory.createTargets(25, "target-id-A", "first description").stream()
                .map(Target::getControllerId).collect(toList());
        targetManagement.toggleTagAssignment(targAs, targTagA.getName());

        testdataFactory.createTargets(25, "target-id-B", "first description");

        final Page<Target> foundTargets = targetManagement.findTargetsAll(rsqlFilter, new PageRequest(0, 100));

        assertThat(targetManagement.findTargetsAll(new PageRequest(0, 100)).getNumberOfElements()).as("Total targets")
                .isEqualTo(50);
        assertThat(foundTargets.getTotalElements()).as("Targets in RSQL filter").isEqualTo(27L);

    }

    @Test
    @Description("Verify that the find all targets by ids method contains the entities that we are looking for")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 12) })
    public void verifyFindTargetAllById() {
        final List<Long> searchIds = new ArrayList<>();
        searchIds.add(testdataFactory.createTarget("target-4").getId());
        searchIds.add(testdataFactory.createTarget("target-5").getId());
        searchIds.add(testdataFactory.createTarget("target-6").getId());
        for (int i = 0; i < 9; i++) {
            testdataFactory.createTarget("test" + i);
        }

        final List<Target> foundDs = targetManagement.findTargetAllById(searchIds);

        assertThat(foundDs).hasSize(3);

        final List<Long> collect = foundDs.stream().map(Target::getId).collect(Collectors.toList());
        assertThat(collect).containsAll(searchIds);
    }
}

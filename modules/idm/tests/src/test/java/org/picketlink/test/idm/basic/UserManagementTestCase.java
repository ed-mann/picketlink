/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.picketlink.test.idm.basic;

import org.junit.Test;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.sample.Grant;
import org.picketlink.idm.model.sample.Group;
import org.picketlink.idm.model.sample.GroupMembership;
import org.picketlink.idm.model.sample.GroupRole;
import org.picketlink.idm.model.sample.Realm;
import org.picketlink.idm.model.sample.Role;
import org.picketlink.idm.model.sample.User;
import org.picketlink.idm.query.RelationshipQuery;
import org.picketlink.test.idm.ExcludeTestSuite;
import org.picketlink.test.idm.suites.LDAPIdentityStoreWithoutAttributesTestSuite;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * <p>
 * Test case for the {@link User} basic management operations using only the default realm.
 * </p>
 * 
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * 
 */
public class UserManagementTestCase extends AbstractIdentityTypeTestCase<User> {

    @Test
    public void testCreate() throws Exception {
        User newUser = createUser("jduke");

        assertNotNull(newUser.getId());

        newUser.setEmail("jduke@jboss.org");
        newUser.setFirstName("Java");
        newUser.setLastName("Duke");

        IdentityManager identityManager = getIdentityManager();

        identityManager.update(newUser);

        User storedUser = identityManager.getUser(newUser.getLoginName());

        assertNotNull(storedUser);
        assertEquals(newUser.getId(), storedUser.getId());
        assertEquals(newUser.getLoginName(), storedUser.getLoginName());
        assertEquals(newUser.getFirstName(), storedUser.getFirstName());
        assertEquals(newUser.getLastName(), storedUser.getLastName());
        assertEquals(newUser.getEmail(), storedUser.getEmail());
        assertNotNull(storedUser.getPartition());
        assertEquals(Realm.DEFAULT_REALM, storedUser.getPartition().getId());
        assertTrue(storedUser.isEnabled());
        assertNull(storedUser.getExpirationDate());
        assertNotNull(storedUser.getCreatedDate());
        assertTrue(new Date().compareTo(storedUser.getCreatedDate()) >= 0);
    }

    @Test
    @ExcludeTestSuite (LDAPIdentityStoreWithoutAttributesTestSuite.class)
    public void testUpdate() throws Exception {
        IdentityManager identityManager = getIdentityManager();

        User storedUser = createUser("admin");

        storedUser.setEmail("admin@jboss.org");
        storedUser.setFirstName("The");
        storedUser.setLastName("Administrator");

        identityManager.update(storedUser);

        storedUser = identityManager.getUser(storedUser.getLoginName());

        assertEquals("admin", storedUser.getLoginName());
        assertEquals("The", storedUser.getFirstName());
        assertEquals("Administrator", storedUser.getLastName());
        assertEquals("admin@jboss.org", storedUser.getEmail());

        storedUser.setFirstName("Updated " + storedUser.getFirstName());
        storedUser.setLastName("Updated " + storedUser.getLastName());
        storedUser.setEmail("Updated " + storedUser.getEmail());

        Date actualDate = Calendar.getInstance().getTime();

        storedUser.setExpirationDate(actualDate);

        identityManager.update(storedUser);

        User updatedUser = identityManager.getUser(storedUser.getLoginName());

        assertEquals("Updated The", updatedUser.getFirstName());
        assertEquals("Updated Administrator", updatedUser.getLastName());
        assertEquals("Updated admin@jboss.org", updatedUser.getEmail());
        assertEquals(actualDate, updatedUser.getExpirationDate());

    }

    @Test
    public void testRemove() throws Exception {
        IdentityManager identityManager = getIdentityManager();

        User someUser = createUser("admin");
        User anotherUser = createUser("someAnotherUser");

        identityManager.remove(someUser);

        User removedUser = getIdentityManager().getUser(someUser.getLoginName());

        assertNull(removedUser);

        anotherUser = identityManager.getUser(anotherUser.getLoginName());

        assertNotNull(anotherUser);

        Role role = createRole("role");
        Group group = createGroup("group", null);

        PartitionManager partitionManager = getPartitionManager();

        partitionManager.grantRole(anotherUser, role);
        partitionManager.addToGroup(anotherUser, group);
        partitionManager.grantGroupRole(anotherUser, role, group);

        RelationshipQuery<?> relationshipQuery = partitionManager.createRelationshipQuery(Grant.class);

        relationshipQuery.setParameter(Grant.ASSIGNEE, anotherUser);

        assertFalse(relationshipQuery.getResultList().isEmpty());

        relationshipQuery = partitionManager.createRelationshipQuery(GroupMembership.class);

        relationshipQuery.setParameter(GroupMembership.MEMBER, anotherUser);

        assertFalse(relationshipQuery.getResultList().isEmpty());

        relationshipQuery = partitionManager.createRelationshipQuery(GroupRole.class);

        relationshipQuery.setParameter(GroupRole.ASSIGNEE, anotherUser);

        assertFalse(relationshipQuery.getResultList().isEmpty());

        identityManager.remove(anotherUser);

        relationshipQuery = partitionManager.createRelationshipQuery(Grant.class);

        relationshipQuery.setParameter(Grant.ASSIGNEE, anotherUser);

        assertTrue(relationshipQuery.getResultList().isEmpty());

        relationshipQuery = partitionManager.createRelationshipQuery(GroupMembership.class);

        relationshipQuery.setParameter(GroupMembership.MEMBER, anotherUser);

        assertTrue(relationshipQuery.getResultList().isEmpty());

        relationshipQuery = partitionManager.createRelationshipQuery(GroupRole.class);

        relationshipQuery.setParameter(GroupRole.ASSIGNEE, anotherUser);

        assertTrue(relationshipQuery.getResultList().isEmpty());
    }

    @Test
    public void testEqualsMethod() {
        User instanceA = createUser("userA");
        User instanceB = createUser("userB");
        
        assertFalse(instanceA.equals(instanceB));
        
        IdentityManager identityManager = getIdentityManager();
        
        assertTrue(instanceA.equals(identityManager.getUser(instanceA.getLoginName())));
    }
    
    @Test
    @ExcludeTestSuite (LDAPIdentityStoreWithoutAttributesTestSuite.class)
    public void testSetCertificateAsAttribute() {
        User mary = createUser("mary");
        
        IdentityManager identityManager = getIdentityManager();
        
        X509Certificate certificate = getTestingCertificate("servercert.txt");
        
        mary.setAttribute(new Attribute<X509Certificate>("certificate", certificate));
        
        identityManager.update(mary);
        
        mary = identityManager.getUser(mary.getLoginName());
        
        assertNotNull(mary.<X509Certificate>getAttribute("certificate"));
        assertEquals(certificate, mary.<X509Certificate>getAttribute("certificate").getValue());
    }
    
    @Override
    protected User createIdentityType() {
        return createUser("admin");
    }

    @Override
    protected User getIdentityType() {
        return getIdentityManager().getUser("admin");
    }
    
    private X509Certificate getTestingCertificate(String fromTextFile) {
        // Certificate
        InputStream bis = getClass().getClassLoader().getResourceAsStream("cert/" + fromTextFile);
        X509Certificate cert = null;

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) cf.generateCertificate(bis);
        } catch (Exception e) {
            throw new IllegalStateException("Could not load testing certificate.", e);
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                }
            }
        }
        return cert;
    }
}

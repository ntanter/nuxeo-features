/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientResponse;

/**
 * Tests the users and groups Rest endpoints
 *
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(init = RestServerInit.class, cleanup = Granularity.METHOD)
public class UserGroupTest extends BaseUserTest {

    @Inject
    UserManager um;

    @Test
    public void itCanFetchAUser() throws Exception {
        // Given the user1

        // When I call the Rest endpoint
        ClientResponse response = getResponse(RequestType.GET, "/user/user1");

        // Then it returns the Json
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());

        assertEqualsUser("user1", "John", "Lennon", node);

    }

    @Test
    public void itReturnsA404OnNonExistentUser() throws Exception {
        // Given a non existent user

        // When I call the Rest endpoint
        ClientResponse response = getResponse(RequestType.GET,
                "/user/nonexistentuser");

        // Then it returns the Json
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(),
                response.getStatus());

    }

    @Test
    public void itCanUpdateAUser() throws Exception {
        // Given a modified user
        NuxeoPrincipal user = um.getPrincipal("user1");
        user.setFirstName("Paul");
        user.setLastName("McCartney");
        String userJson = getPrincipalAsJson(user);

        // When I call a PUT on the Rest endpoint
        ClientResponse response = getResponse(RequestType.PUT, "/user/user1",
                userJson);

        // Then it changes the user
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEqualsUser("user1", "Paul", "McCartney", node);

        user = um.getPrincipal("user1");
        assertEquals("Paul", user.getFirstName());
        assertEquals("McCartney", user.getLastName());

    }

    @Test
    public void itCanDeleteAUser() throws Exception {
        // Given a modified user
        NuxeoPrincipal user = um.getPrincipal("user1");

        // When I call a DELETE on the Rest endpoint
        ClientResponse response = getResponse(RequestType.DELETE, "/user/user1");

        // Then the user is deleted
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(),
                response.getStatus());

        user = um.getPrincipal("user1");
        assertNull(user);

    }

    @Test
    public void itCanCreateAUser() throws Exception {
        // Given a new user
        NuxeoPrincipal principal = new NuxeoPrincipalImpl("newuser");
        principal.setFirstName("test");
        principal.setLastName("user");
        principal.setCompany("nuxeo");
        principal.setEmail("test@nuxeo.com");

        // When i POST it on the user endpoint
        ClientResponse response = getResponse(RequestType.POST, "/user",
                getPrincipalAsJson(principal));

        // Then a user is created
        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEqualsUser("newuser", "test", "user", node);

        principal = um.getPrincipal("newuser");
        assertEquals("test", principal.getFirstName());
        assertEquals("user", principal.getLastName());
        assertEquals("nuxeo", principal.getCompany());
        assertEquals("test@nuxeo.com", principal.getEmail());

    }

    @Test
    public void itCanGetAGroup() throws Exception {
        // Given a group
        NuxeoGroup group = um.getGroup("group1");

        // When i GET on the API
        ClientResponse response = getResponse(RequestType.GET, "/group/"
                + group.getName());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Then i GET the Group
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEqualsGroup(group.getName(), group.getLabel(), node);

    }

    @Test
    public void itCanChangeAGroup() throws Exception {
        // Given a modified group
        NuxeoGroup group = um.getGroup("group1");
        group.setLabel("modifiedGroup");
        group.setMemberUsers(Arrays.asList(new String[] { "user1", "user2" }));
        group.setMemberGroups(Arrays.asList(new String[] { "group2" }));

        // When i PUT this group
        ClientResponse response = getResponse(RequestType.PUT, "/group/"
                + group.getName(), getGroupAsJson(group));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Then the group is modified server side
        group = um.getGroup("group1");
        assertEquals("modifiedGroup", group.getLabel());
        assertEquals(2, group.getMemberUsers().size());
        assertEquals(1, group.getMemberGroups().size());
    }

    @Test
    public void itCanDeleteGroup() throws Exception {

        // When i DELETE on a group resources
        ClientResponse response = getResponse(RequestType.DELETE,
                "/group/group1");
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(),
                response.getStatus());

        // Then the group is deleted
        assertNull(um.getGroup("group1"));
    }

    @Test
    public void itCanCreateAGroup() throws Exception {
        // Given a modified group
        NuxeoGroup group = new NuxeoGroupImpl("newGroup");
        group.setLabel("a new group");
        group.setMemberUsers(Arrays.asList(new String[] { "user1", "user2" }));
        group.setMemberGroups(Arrays.asList(new String[] { "group2" }));

        // When i POST this group
        ClientResponse response = getResponse(RequestType.POST, "/group/",
                getGroupAsJson(group));
        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        // Then the group is modified server side
        group = um.getGroup("newGroup");
        assertEquals("a new group", group.getLabel());
        assertEquals(2, group.getMemberUsers().size());
        assertEquals(1, group.getMemberGroups().size());

    }

    @Test
    public void itCanAddAGroupToAUser() throws Exception {
        // Given a user and a group
        NuxeoPrincipal principal = um.getPrincipal("user1");
        NuxeoGroup group = um.getGroup("group1");
        assertFalse(principal.isMemberOf(group.getName()));

        // When i POST this group
        ClientResponse response = getResponse(RequestType.POST, "/user/"
                + principal.getName() + "/group/" + group.getName());

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        principal = um.getPrincipal(principal.getName());
        assertTrue(principal.isMemberOf(group.getName()));

    }

    @Test
    public void itCanAddAUserToAGroup() throws Exception {
        // Given a user and a group
        NuxeoPrincipal principal = um.getPrincipal("user1");
        NuxeoGroup group = um.getGroup("group2");
        assertFalse(principal.isMemberOf(group.getName()));

        // When i POST this group
        ClientResponse response = getResponse(RequestType.POST, "/group/"
                + group.getName() + "/user/" + principal.getName());

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        principal = um.getPrincipal(principal.getName());
        assertTrue(principal.isMemberOf(group.getName()));
    }

    @Test
    public void itCanRemoveAUserToAGroup() throws Exception {
        // Given a user in a group
        NuxeoPrincipal principal = um.getPrincipal("user1");
        NuxeoGroup group = um.getGroup("group1");
        principal.setGroups(Arrays.asList(new String[] { group.getName() }));
        um.updateUser(principal.getModel());
        principal = um.getPrincipal("user1");
        assertTrue(principal.isMemberOf(group.getName()));

        // When i POST this group
        ClientResponse response = getResponse(RequestType.DELETE, "/user/"
                + principal.getName() + "/group/" + group.getName());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        principal = um.getPrincipal(principal.getName());
        assertFalse(principal.isMemberOf(group.getName()));

    }

    @Test
    public void itCanSearchUsers() throws Exception {
        // Given a search string
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("q", "Steve");

        ClientResponse response = getResponse(RequestType.GET, "/user/search",
                queryParams);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        ArrayNode items = (ArrayNode) node.get("items");
        assertEquals(1, items.size());
        assertEquals("user0",
                node.get("items").get(0).get("id").getValueAsText());

    }

    @Test
    public void itCanSearchGroups() throws Exception {
        // Given a search string
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("q", "Lannister");

        ClientResponse response = getResponse(RequestType.GET, "/group/search",
                queryParams);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        ArrayNode items = (ArrayNode) node.get("items");
        assertEquals(1, items.size());
        assertEquals("Lannister",items.get(0).get("grouplabel").getValueAsText());


    }

}

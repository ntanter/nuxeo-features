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
package org.nuxeo.ecm.automation.rest.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.RestServerFeature;
import org.nuxeo.ecm.automation.test.RestServerInit;
import org.nuxeo.ecm.automation.test.helpers.JSONDocumentHelper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 *
 *
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class DocumentModelListReaderTest {

    private HttpServletRequest request = mock(HttpServletRequest.class);

    @Inject
    CoreSession session;

    @Before
    public void doBefore() {
        when(request.getUserPrincipal()).thenReturn(session.getPrincipal());
    }

    @Test
    public void iCanReadADocument() throws Exception {
        DocumentModel note1 = RestServerInit.getNote(1, session);

        String json = JSONDocumentHelper.getDocAsJson(note1);

        DocumentModel doc = JSONDocumentModelReader.readRequest(json, null,
                request);
        assertNotNull(doc);
        assertEquals(note1.getId(), doc.getId());

    }

    @Test
    public void iCanReadADocumentModelList() throws Exception {
        DocumentModel note1 = RestServerInit.getNote(1, session);
        DocumentModel note2 = RestServerInit.getNote(2, session);

        String docsAsJson = JSONDocumentHelper.getDocsListAsJson(note1, note2);

        DocumentModelList docs = JSONDocumentModelListReader.readRequest(
                docsAsJson, null, request);
        assertEquals(2, docs.size());

        assertEquals(RestServerInit.getNote(1, session).getId(),
                docs.get(0).getId());

    }

}

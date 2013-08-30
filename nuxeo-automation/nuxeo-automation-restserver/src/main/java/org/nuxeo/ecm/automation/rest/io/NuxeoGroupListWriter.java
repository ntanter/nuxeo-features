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

import java.io.IOException;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoGroup;

/**
 *
 *
 * @since 5.7.3
 */
@Provider
@Produces({ "application/json+nxentity", "application/json" })
public class NuxeoGroupListWriter extends EntityListWriter<NuxeoGroup> {

    @Override
    protected String getEntityType() {
        return "groups";
    }

    @Override
    protected void writeItem(JsonGenerator jg, NuxeoGroup item)
            throws ClientException, IOException {
        NuxeoGroupWriter.writeGroup(jg, item);
    }

}

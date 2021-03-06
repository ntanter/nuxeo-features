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
package org.nuxeo.ecm.automation.jaxrs.io.directory;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.EntityListWriter;
import org.nuxeo.ecm.core.api.ClientException;

/**
 *
 *
 * @since 5.7.3
 */
public class DirectoryEntriesWriter extends EntityListWriter<DirectoryEntry> {

    @Override
    protected String getEntityType() {
        return "directory-entries";
    }

    @Override
    protected void writeItem(JsonGenerator jg, DirectoryEntry item)
            throws ClientException, IOException {

        DirectoryEntryWriter.writeTo(jg, item);

    }

    /**
     * @param jg
     * @param entries
     * @throws IOException
     * @throws ClientException
     * @throws JsonGenerationException
     *
     */
    public void writeTo(JsonGenerator jg, List<DirectoryEntry> entries)
            throws JsonGenerationException, ClientException, IOException {
        writeList(jg, entries);
    }

}

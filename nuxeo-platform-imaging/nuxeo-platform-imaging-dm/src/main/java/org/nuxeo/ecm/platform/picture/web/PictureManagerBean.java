/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.picture.web;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.UserAction;
import org.nuxeo.ecm.platform.ui.web.rest.RestHelper;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.codec.DocumentFileCodec;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 */
@Name("pictureManager")
@Scope(CONVERSATION)
public class PictureManagerBean implements PictureManager, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PictureManagerBean.class);

    protected static Boolean imageMagickAvailable;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @RequestParameter
    protected String fileFieldFullName;

    @In(required = true, create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected ResourcesAccessor resourcesAccessor;

    protected String fileurlPicture;

    protected String filename;

    protected Blob fileContent;

    protected Integer index;

    protected String cropCoords;

    protected ArrayList<Map<String, Object>> selectItems;

    @Override
    @Create
    public void initialize() throws Exception {
        log.debug("Initializing...");
        index = 0;
    }

    protected DocumentModel getCurrentDocument() {
        return navigationContext.getCurrentDocument();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getFileurlPicture() throws ClientException {
        ArrayList<Map<String, Object>> views = (ArrayList) getCurrentDocument().getProperty(
                "picture", "views");
        return views.get(index).get("title") + ":content";
    }

    @Override
    public void setFileurlPicture(String fileurlPicture) {
        this.fileurlPicture = fileurlPicture;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void initSelectItems() throws ClientException {
        selectItems = new ArrayList<Map<String, Object>>();
        DocumentModel doc = getCurrentDocument();
        ArrayList<Map<String, Object>> views = (ArrayList) doc.getProperty(
                "picture", "views");
        for (int i = 0; i < views.size(); i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("title", views.get(i).get("title"));
            map.put("idx", i);
            selectItems.add(map);
        }
    }

    @Override
    public ArrayList getSelectItems() throws ClientException {
        if (selectItems == null) {
            initSelectItems();
            return selectItems;
        } else {
            return selectItems;
        }
    }

    @Override
    public void setSelectItems(ArrayList selectItems) {
        this.selectItems = selectItems;
    }

    @Override
    @Deprecated
    @SuppressWarnings("unchecked")
    public String addPicture() throws Exception {
        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        DocumentModel doc = navigationContext.getChangeableDocument();

        String parentPath;
        if (getCurrentDocument() == null) {
            // creating item at the root
            parentPath = documentManager.getRootDocument().getPathAsString();
        } else {
            parentPath = navigationContext.getCurrentDocument().getPathAsString();
        }

        String title = (String) doc.getProperty("dublincore", "title");
        if (title == null) {
            title = "";
        }
        // set parent path and name for document model
        doc.setPathInfo(parentPath, pss.generatePathSegment(doc));
        try {
            DocumentModel parent = getCurrentDocument();
            ArrayList<Map<String, Object>> pictureTemplates = null;
            if (parent.getType().equals("PictureBook")) {
                // Use PictureBook Properties
                pictureTemplates = (ArrayList<Map<String, Object>>) parent.getProperty(
                        "picturebook", "picturetemplates");
            }
            PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
            boolean status = picture.fillPictureViews(fileContent, filename,
                    title, pictureTemplates);
            if (!status) {
                documentManager.cancel();
                log.info("Picture type unsupported.");
                FacesMessages.instance().add(
                        StatusMessage.Severity.ERROR,
                        resourcesAccessor.getMessages().get(
                                "label.picture.upload.error"));

                return navigationContext.getActionResult(
                        navigationContext.getCurrentDocument(), UserAction.VIEW);
            } else {
                doc = documentManager.createDocument(doc);
                documentManager.saveDocument(doc);

                Events.instance().raiseEvent(
                        EventNames.DOCUMENT_CHILDREN_CHANGED, parent);

                documentManager.save();
            }
        } catch (Exception e) {
            log.error("Picture Creation failed", e);
            documentManager.cancel();
            FacesMessage message = FacesMessages.createFacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.picture.upload.error"));
            FacesMessages.instance().add(message);
            return navigationContext.getActionResult(
                    navigationContext.getCurrentDocument(), UserAction.VIEW);
        }
        return navigationContext.getActionResult(doc, UserAction.AFTER_CREATE);
    }

    @Override
    public String rotate90left() throws ClientException, IOException {
        DocumentModel doc = getCurrentDocument();
        PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
        picture.doRotate(-90);
        documentManager.saveDocument(doc);
        documentManager.save();
        navigationContext.setCurrentDocument(doc);
        return null;
    }

    @Override
    public String rotate90right() throws ClientException, IOException {
        DocumentModel doc = getCurrentDocument();
        PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
        picture.doRotate(90);
        documentManager.saveDocument(doc);
        documentManager.save();
        navigationContext.setCurrentDocument(doc);
        return null;
    }

    @Override
    public String crop() throws ClientException, IOException {
        if (cropCoords != null && !cropCoords.equals("")) {
            DocumentModel doc = getCurrentDocument();
            PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
            picture.doCrop(cropCoords);
            documentManager.saveDocument(doc);
            documentManager.save();
            navigationContext.setCurrentDocument(doc);
        }
        return null;
    }

    @Override
    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED,
            EventNames.DOCUMENT_CHANGED })
    @BypassInterceptors
    public void resetFields() {
        filename = "";
        fileContent = null;
        selectItems = null;
        index = 0;
        cropCoords = null;
    }

    @WebRemote
    public String remoteDownload(String patternName, String docID,
            String blobPropertyName, String filename) throws ClientException {
        IdRef docref = new IdRef(docID);
        DocumentModel doc = documentManager.getDocument(docref);
        return DocumentModelFunctions.fileUrl(patternName, doc,
                blobPropertyName, filename);
    }

    @WebRemote
    public static String urlPopup(String url) {
        return RestHelper.addCurrentConversationParameters(url);
    }

    @Override
    public void download(DocumentView docView) throws ClientException {
        if (docView != null) {
            DocumentLocation docLoc = docView.getDocumentLocation();
            // fix for NXP-1799
            if (documentManager == null) {
                RepositoryLocation loc = new RepositoryLocation(
                        docLoc.getServerName());
                navigationContext.setCurrentServerLocation(loc);
                documentManager = navigationContext.getOrCreateDocumentManager();
            }
            DocumentModel doc = documentManager.getDocument(docLoc.getDocRef());
            if (doc != null) {
                String[] propertyPath = docView.getParameter(
                        DocumentFileCodec.FILE_PROPERTY_PATH_KEY).split(":");
                String title = null;
                String field = null;
                Property datamodel = null;
                if (propertyPath.length == 2) {
                    title = propertyPath[0];
                    field = propertyPath[1];
                    datamodel = doc.getProperty("picture:views");
                } else if (propertyPath.length == 3) {
                    String schema = propertyPath[0];
                    title = propertyPath[1];
                    field = propertyPath[2];
                    datamodel = doc.getProperty(schema + ":" + "views");
                }
                Property view = null;
                for (Property property : datamodel) {
                    if (property.get("title").getValue().equals(title)) {
                        view = property;
                    }
                }

                if (view == null) {
                    for (Property property : datamodel) {
                        if (property.get("title").getValue().equals("Thumbnail")) {
                            view = property;
                        }
                    }
                }
                if (view == null) {
                    return;
                }
                Blob blob = (Blob) view.getValue(field);
                String filename = (String) view.getValue("filename");
                // download
                FacesContext context = FacesContext.getCurrentInstance();

                ComponentUtils.download(context, blob, filename);
            }
        }
    }

    @Override
    @Destroy
    public void destroy() {
        log.debug("Removing Seam action listener...");
        fileurlPicture = null;
        filename = null;
        fileContent = null;
        index = null;
        selectItems = null;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public Blob getFileContent() {
        return fileContent;
    }

    @Override
    public void setFileContent(Blob fileContent) {
        this.fileContent = fileContent;
    }

    @Override
    public Integer getIndex() {
        return index;
    }

    @Override
    public void setIndex(Integer index) {
        this.index = index;
    }

    @Override
    public String getCropCoords() {
        return cropCoords;
    }

    @Override
    public void setCropCoords(String cropCoords) {
        this.cropCoords = cropCoords;
    }

    public Boolean isImageMagickAvailable() throws Exception {
        if (imageMagickAvailable == null) {
            CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
            CommandAvailability ca = cles.getCommandAvailability("cropAndResize");
            imageMagickAvailable = ca.isAvailable();
        }
        return imageMagickAvailable;
    }
}

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
package org.nuxeo.ecm.automation.server.jaxrs.adapters;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.ChainTypeImpl;
import org.nuxeo.ecm.automation.core.impl.InvokableMethod;
import org.nuxeo.ecm.automation.jaxrs.io.operations.ExecutionRequest;
import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.ecm.automation.server.jaxrs.ResponseHelper;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.2 - Web adapter that expose how to run an operation on a document
 */
@WebAdapter(name = OperationAdapter.NAME, type = "OperationService")
@Produces({ "application/json+nxentity", MediaType.APPLICATION_JSON })
public class OperationAdapter extends DefaultAdapter {

    public static final String NAME = "op";

    @POST
    @Path("{operationName}")
    public Response doPost(@PathParam("operationName")
    String oid, @Context
    HttpServletRequest request, ExecutionRequest xreq) {
        try {
            AutomationServer srv = Framework.getLocalService(AutomationServer.class);
            if (!srv.accept(oid, false, request)) {
                return ResponseHelper.notFound();
            }

            AutomationService service = Framework.getLocalService(AutomationService.class);

            OperationType operationType = service.getOperation(oid);

            // If chain, taking the first operation to do the input lookup after
            if (operationType instanceof ChainTypeImpl) {
                OperationChain chain = ((ChainTypeImpl) operationType).getChain();
                if (!chain.getOperations().isEmpty()) {
                    operationType = service.getOperation(chain.getOperations().get(
                            0).id());
                } else {
                    throw new WebException("Chain '" + oid
                            + "' doesn't contain any operation");
                }
            }

            for (InvokableMethod method : operationType.getMethods()) {
                if (getTarget().getAdapter(method.getInputType()) != null) {
                    xreq.setInput(getTarget().getAdapter(method.getInputType()));
                    break;
                }
            }

            OperationContext ctx = xreq.createContext(request,
                    getContext().getCoreSession());

            return Response.ok(service.run(ctx, oid, xreq.getParams())).build();
        } catch (Exception e) {
            throw WebException.wrap("Failed to execute operation: "
                    + oid, e);
        }

    }

}

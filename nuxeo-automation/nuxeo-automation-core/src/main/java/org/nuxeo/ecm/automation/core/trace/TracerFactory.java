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
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.trace;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationCallback;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.runtime.api.Framework;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * @since 5.7.3 The Automation tracer factory service.
 */
public class TracerFactory implements TracerFactoryMBean {

    public static final String AUTOMATION_TRACE_PROPERTY = "org.nuxeo.automation.trace";

    public static final String AUTOMATION_TRACE_PRINTABLE_PROPERTY = "org.nuxeo.automation.trace.printable";

    private static final Log log = LogFactory.getLog(TracerFactory.class);

    protected String printableTraces;

    protected Cache<String, ChainTraces> tracesCache;

    protected boolean recording;

    public TracerFactory() {
        this.tracesCache = CacheBuilder.newBuilder().concurrencyLevel(10).maximumSize(
                1000).expireAfterWrite(1, TimeUnit.HOURS).build();
        this.recording = Boolean.parseBoolean(Framework.getProperty(
                AUTOMATION_TRACE_PROPERTY, "false"));
        this.printableTraces = Framework.getProperty(
                AUTOMATION_TRACE_PRINTABLE_PROPERTY, "*");
    }

    protected static class ChainTraces {

        protected OperationType chain;

        protected Map<Integer, Trace> traces = new HashMap<Integer, Trace>();

        protected ChainTraces(OperationType chain) {
            this.chain = chain;
        }

        protected String add(Trace trace) {
            final int index = Integer.valueOf(traces.size());
            traces.put(Integer.valueOf(index), trace);
            return formatKey(trace.chain, index);
        }

        protected Trace getTrace(int index) {
            return traces.get(index);
        }

        protected void removeTrace(int index) {
            traces.remove(index);
        }

        protected void clear() {
            traces.clear();
        }

    }

    /**
     * If trace mode is enabled, instantiate {@link Tracer}. If not, instantiate
     * {@link TracerLite}.
     */
    public OperationCallback newTracer(String operationTypeId) {
        if (recording) {
            return new Tracer(this, printable(operationTypeId));
        }
        return new TracerLite(this);
    }

    protected Boolean printable(String operationTypeId) {
        if (!"*".equals(printableTraces)) {
            try {
                String[] printableTraces = this.printableTraces.split(",");
                return Arrays.asList(printableTraces).contains(operationTypeId);
            } catch (PatternSyntaxException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("The property ");
                stringBuilder.append(AUTOMATION_TRACE_PRINTABLE_PROPERTY);
                stringBuilder.append(":");
                stringBuilder.append(printableTraces);
                stringBuilder.append(" is wrongly set. All automation traces are printable.");
                log.info(stringBuilder.toString(), e);
                return true;
            }
        }
        return true;
    }

    public String recordTrace(Trace trace) {
        String chainId = trace.chain.getId();
        ChainTraces chainTraces = tracesCache.getIfPresent(chainId);
        if (chainTraces == null) {
            tracesCache.put(chainId, new ChainTraces(trace.chain));
        }
        return tracesCache.getIfPresent(chainId).add(trace);
    }

    public Trace getTrace(OperationChain chain, int index) {
        return tracesCache.getIfPresent(chain.getId()).getTrace(index);
    }

    /**
     * @param key The name of the chain.
     * @return The last trace of the given chain.
     */
    public Trace getTrace(String key) {
        ChainTraces chainTrace = tracesCache.getIfPresent(key);
        if (chainTrace == null) {
            return null;
        }
        return tracesCache.getIfPresent(key).getTrace(
                chainTrace.traces.size() - 1);
    }

    public void clearTrace(OperationChain chain, int index) {
        tracesCache.getIfPresent(chain).removeTrace(Integer.valueOf(index));
    }

    public void clearTrace(OperationChain chain) {
        tracesCache.invalidate(chain);
    }

    @Override
    public void clearTraces() {
        tracesCache.invalidateAll();
    }

    protected static String formatKey(OperationType chain, int index) {
        return String.format("%s:%s", chain.getId(), index);
    }

    public void onTrace(Trace popped) {
        recordTrace(popped);
    }

    @Override
    public boolean toggleRecording() {
        return recording = !recording;
    }

    @Override
    public boolean getRecordingState() {
        return recording;
    }

    @Override
    public String getPrintableTraces() {
        return printableTraces;
    }

    @Override
    public String setPrintableTraces(String printableTraces) {
        this.printableTraces = printableTraces;
        return printableTraces;
    }
}

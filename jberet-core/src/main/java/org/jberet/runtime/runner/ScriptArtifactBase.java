/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.jberet.runtime.runner;

import java.util.HashMap;
import java.util.Map;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.jberet._private.BatchLogger;
import org.jberet.job.model.Properties;
import org.jberet.job.model.Script;
import org.jberet.runtime.context.StepContextImpl;

/**
 * Base class for batch artifacts that run a script.
 */
abstract class ScriptArtifactBase {
    final Script script;
    final Properties artifactProperties;
    final StepContextImpl stepContext;

    final ScriptEngine engine;
    CompiledScript compiledScript;
    final String scriptContent;

    Map<String, String> methodMapping;

    public ScriptArtifactBase(final Script script, final Properties artifactProperties, final StepContextImpl stepContext) throws ScriptException {
        this.script = script;
        this.artifactProperties = artifactProperties;
        this.stepContext = stepContext;

        final ClassLoader classLoader = stepContext.getClassLoader();
        scriptContent = script.getContent(classLoader);
        this.engine = script.getEngine(classLoader);
        setEngineScopeAttributes();
        if (engine instanceof Compilable) {
            compiledScript = ((Compilable) engine).compile(scriptContent);
        }

        if (artifactProperties != null) {
            final String methodMappingVal = artifactProperties.get("methodMapping");
            if (methodMappingVal != null) {
                methodMapping = new HashMap<String, String>();
                final String[] pairs = methodMappingVal.split(",");
                for (final String pair : pairs) {
                    final String[] keyValue = pair.split("=");
                    methodMapping.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        }
    }

    void setEngineScopeAttributes() {
        final ScriptContext scriptContext = this.engine.getContext();
        try {
            scriptContext.setAttribute("jobContext", stepContext.getJobContext(), ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute("stepContext", stepContext, ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute("batchProperties", Properties.toJavaUtilProperties(artifactProperties), ScriptContext.ENGINE_SCOPE);
        } catch (final Exception e) {
            BatchLogger.LOGGER.failToSetAttributesToScriptContext(e, engine.toString(), script.getType());
        }
    }

    String getFunctionName(final String batchApiMethodName) {
        if (methodMapping == null) {
            return batchApiMethodName;
        }
        final String functionName = methodMapping.get(batchApiMethodName);
        return functionName == null ? batchApiMethodName : functionName;
    }
}

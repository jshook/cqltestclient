/*
*   Copyright 2016 jshook
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
package com.metawiring.load.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ScriptExecutor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ScriptExecutor.class);

    private final List<String> scripts = new ArrayList<>();
    private ScriptEngine engine;
    private Optional<ScriptContext> scriptContext = Optional.empty();

    public static void main(String[] args) {
        ScriptExecutor env = new ScriptExecutor();
        env.addScriptFiles(args);
        env.run();
    }

    public ScriptExecutor addScriptContext(ScriptContext context) {
        scriptContext = Optional.of(context);
        return this;
    }

    public ScriptExecutor addScriptText(String scriptText) {
        scripts.add(scriptText);
        return this;
    }

    public ScriptExecutor addScriptFiles(String[] args) {
        for (String scriptFile : args) {
            Path scriptPath = Paths.get(scriptFile);
            byte[] bytes = new byte[0];
            try {
                bytes = Files.readAllBytes(scriptPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            Charset utf8 = Charset.forName("UTF8");
            String scriptData = utf8.decode(bb).toString();
            addScriptText(scriptData);
        }
        return this;
    }

    @Override
    public void run() {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine engine = sem.getEngineByName("nashorn");
        scriptContext.ifPresent(engine::setContext);

        for (String script : scripts) {
            try {
                Object result = engine.eval(script);

//                logger.debug("engine eval result:" + result);
            } catch (ScriptException e) {
                e.printStackTrace();
            }
        }
    }

}

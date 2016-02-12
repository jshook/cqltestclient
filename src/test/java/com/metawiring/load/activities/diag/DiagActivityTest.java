package com.metawiring.load.activities.diag;

import com.metawiring.load.cycler.api.ActionDispenser;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.cycler.api.ActivityAction;
import org.testng.annotations.Test;

import java.util.function.LongConsumer;

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
public class DiagActivityTest {

    @Test
    public void testDiagActivity() {
        DiagActivity da = new DiagActivity();
        da.getName();
        ActivityDef ad = ActivityDef.parseActivityDef("type=diag;");
        ActionDispenser actionDispenser = da.getActionDispenser(ad);
        ActivityAction action = actionDispenser.getAction(1,ad);
        action.accept(1L);
    }

}
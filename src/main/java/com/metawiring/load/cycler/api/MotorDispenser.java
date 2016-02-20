/*
*   Copyright 2015 jshook
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
package com.metawiring.load.cycler.api;

import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.cycler.motors.ActivityMotor;

/**
 * Allow for custom activity motors to be provided.
 */
public interface MotorDispenser {
    /**
     * Return an instance of an ActivityMotor, given the ActivityDef and the slot it will be assigned to.
     * @param activityDef - The activity definition to be supported by the motor.
     * @param motorId - The slot nuber of the motor.
     * @return the motor instance
     */
    ActivityMotor getMotor(ActivityDef activityDef, int motorId);
}

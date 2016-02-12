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
package com.metawiring.load.cycler;

import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.config.ParameterMap;
import com.metawiring.load.core.IndexedThreadFactory;
import com.metawiring.load.cycler.api.MotorDispenser;
import com.metawiring.load.cycler.api.ActivityDefObserver;
import com.metawiring.load.cycler.motors.ActivityMotor;
import com.metawiring.load.generator.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.LongSupplier;

/**
 * <p>An ActivityExecutor is a named instance of an execution harness for a single activity instance.
 * It is responsible for managing threads and activity settings which may be changed while the
 * activity is running.</p>
 * <p/>
 * <p>An ActivityExecutor may be represent an activity that is defined and active in the running
 * scenario, but which is inactive. This can occur when an activity is paused by controlling logic,
 * or when the threads are set to zero.</p>
 */
public class ActivityExecutor implements ParameterMap.Listener {
    private static final Logger logger = LoggerFactory.getLogger(ActivityExecutor.class);

    private ActivityDef activityDef;
    private LongSupplier cycleSupplier;

    private ExecutorService executorService;
    private MotorDispenser activityMotorDispenser;
    private final List<ActivityMotor> activityMotors = new ArrayList<>();

    public ActivityExecutor(ActivityDef activityDef) {
        this.activityDef = activityDef;
        executorService = new ThreadPoolExecutor(
                0, Integer.MAX_VALUE,
                0L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new IndexedThreadFactory(activityDef.getAlias()));
        ScopedCachingGeneratorSource gi = new ScopedGeneratorCache(
                new GeneratorInstantiator(), RuntimeScope.activity
        );
        activityDef.getParams().addListener(this);
    }

    public void setActivityMotorDispenser(MotorDispenser activityMotorDispenser) {
        this.activityMotorDispenser = activityMotorDispenser;
    }

    /**
     * <p>True-up the number of motor instances known to the executor. Start all non-running motors.
     * The protocol between the motors and the executor should be safe as long as each state change
     * is owned by either the motor logic or the activity executor but not both, and strictly serialized
     * as well. This is enforced by forcing start() to be serialized as well as using CAS on the motor states.</p>
     * <p/>
     * <p>The start method may be called to true-up the number of active motors in an activity executor after
     * changes to threads.</p>
     */
    public synchronized void start() {
        logger.info("starting activity " + activityDef.getLogName());

        adjustToActivityDef(activityDef);

    }

    private void adjustToActivityDef(ActivityDef activityDef) {

        Optional.ofNullable(activityMotorDispenser).orElseThrow(() ->
        new RuntimeException("cycleMotorFactory is required"));

        while (activityMotors.size() > activityDef.getThreads()) {
            ActivityMotor motor = activityMotors.get(activityMotors.size() - 1);
            logger.trace("Stopping cycle motor thread:" + motor);
            motor.getMotorController().requestStop();
            activityMotors.remove(activityMotors.size() - 1);
        }

        while (activityMotors.size() < activityDef.getThreads()) {
            ActivityMotor motor = activityMotorDispenser.getMotor(activityDef, activityMotors.size());
            logger.trace("Starting cycle motor thread:" + motor);
            activityMotors.add(motor);
        }

        activityMotors.stream()
                .filter(m -> !m.hasStarted())
                .forEach(executorService::execute);

    }

    public synchronized void forceStop() {
        stop();
        try {
            Thread.sleep(1000); // Well, yeah, but this isn't that complicated, people.
        } catch (InterruptedException ignored) {
        }

        logger.info("stopping activity forcibly " + activityDef.getLogName());
        List<Runnable> runnables = executorService.shutdownNow();

        logger.debug(runnables.size() + " threads never started.");

    }

    public synchronized void stop() {
        logger.info("stopping activity " + activityDef.getLogName());
        for (ActivityMotor activityMotor : activityMotors) {
            Optional.of(activityMotor)
                    .map(ActivityMotor::getMotorController)
                    .ifPresent(MotorController::requestStop);
        }
    }

    public void stopExecutor() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleParameterMapUpdate(ParameterMap parameterMap) {
        adjustToActivityDef(activityDef);
        activityMotors.stream().filter(
                m -> (m instanceof ActivityDefObserver)
        ).forEach(
                m -> ((ActivityDefObserver)m).onActivityDefUpdate(activityDef)
        );
        // TODO: clean this up: activityDef or parameterMap? Pick one.
    }

    public ActivityDef getActivityDef() {
        return activityDef;
    }
}

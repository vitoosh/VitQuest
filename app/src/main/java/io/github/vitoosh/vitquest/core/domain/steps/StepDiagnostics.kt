package io.github.vitoosh.vitquest.core.domain.steps

import java.time.Instant

/** What the step collector has seen since the app started. Shown on the diagnostics screen; never stored or sent. */
data class StepDiagnostics(
    /** Steps are being collected because the app is open or the background service runs. */
    val collecting: Boolean = false,
    val serviceRunning: Boolean = false,
    val counterAvailable: Boolean = false,
    val lastCounterValue: Long? = null,
    val lastCounterAt: Instant? = null,
    val motionAvailable: Boolean = false,
    val motionWakeUp: Boolean = false,
    val motionSamplesThisSession: Long = 0,
    val motionStepsThisSession: Long = 0,
    val lastMotionStepAt: Instant? = null,
)

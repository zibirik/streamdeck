package com.streampanel.core.model

import kotlinx.serialization.Serializable

@Serializable
data class MacroProgram(
    val id: String,
    val name: String,
    val variables: Map<String, String> = emptyMap(),
    val steps: List<MacroStep> = emptyList(),
)

@Serializable
sealed interface MacroStep {
    @Serializable
    data class RunAction(
        val action: ControlAction,
    ) : MacroStep

    @Serializable
    data class Delay(
        val durationMs: Long,
    ) : MacroStep

    @Serializable
    data class SetVariable(
        val name: String,
        val value: String,
    ) : MacroStep

    @Serializable
    data class Condition(
        val variable: String,
        val equals: String,
        val thenSteps: List<MacroStep>,
        val elseSteps: List<MacroStep> = emptyList(),
    ) : MacroStep

    @Serializable
    data class Loop(
        val count: Int,
        val steps: List<MacroStep>,
    ) : MacroStep

    @Serializable
    data class Timer(
        val delayMs: Long,
        val steps: List<MacroStep>,
    ) : MacroStep
}

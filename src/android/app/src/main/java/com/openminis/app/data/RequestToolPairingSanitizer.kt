package com.openminis.app.data

import com.openminis.app.data.model.AgentContentPart
import com.openminis.app.data.model.LLMMessage

/**
 * Repairs tool-call pairing on the FINAL history slice sent to a provider.
 *
 * Context compaction and request-budget pruning can legitimately remove one
 * side of a ToolUse -> ToolResult exchange after the mutable full history has
 * already been sanitized. OpenAI Responses then rejects a surviving
 * function_call_output with "No tool call found ... call_id". This pass is
 * pure and request-local: it never rewrites the persisted audit history.
 */
object RequestToolPairingSanitizer {
    data class Result(
        val messages: List<LLMMessage>,
        val removedOrphanResults: Int,
        val insertedMissingResults: Int,
    )

    fun sanitize(messages: List<LLMMessage>): Result {
        val working = messages.toMutableList()
        var inserted = 0

        // Ensure every assistant tool call has a matching result immediately
        // after it. Provider protocols require adjacency, not merely a match
        // somewhere later in the conversation.
        var i = 0
        while (i < working.size) {
            val message = working[i]
            val uses = if (message.role == LLMMessage.Role.ASSISTANT) {
                message.contentParts.filterIsInstance<AgentContentPart.ToolUse>()
            } else emptyList()
            if (uses.isEmpty()) {
                i++
                continue
            }

            val next = working.getOrNull(i + 1)
            val nextResults = if (next?.role == LLMMessage.Role.USER) {
                next.contentParts.filterIsInstance<AgentContentPart.ToolResult>()
            } else emptyList()
            val nextResultIds = nextResults.map { it.id }.toSet()
            val uniqueUses = uses.distinctBy { it.id }
            val missing = uniqueUses.filter { it.id !in nextResultIds }
            if (missing.isNotEmpty()) {
                val placeholders = missing.map { use ->
                    AgentContentPart.ToolResult(
                        id = use.id,
                        name = use.name,
                        content = "Tool execution was interrupted before its result entered the active context.",
                        isError = true,
                    )
                }
                inserted += placeholders.size
                if (next != null && next.role == LLMMessage.Role.USER && nextResults.isNotEmpty()) {
                    working[i + 1] = next.copy(contentParts = next.contentParts + placeholders)
                } else {
                    working.add(
                        i + 1,
                        LLMMessage(
                            role = LLMMessage.Role.USER,
                            content = "",
                            contentParts = placeholders,
                        ),
                    )
                }
            }
            i += 2
        }

        // Remove outputs whose claiming tool call did not survive the final
        // slice. This is the exact malformed shape behind OpenAI's
        // "No tool call found for function call output with call_id ...".
        val validUseIds = working.asSequence()
            .filter { it.role == LLMMessage.Role.ASSISTANT }
            .flatMap { it.contentParts.asSequence() }
            .filterIsInstance<AgentContentPart.ToolUse>()
            .map { it.id }
            .toSet()
        var removed = 0
        val cleaned = ArrayList<LLMMessage>(working.size)
        for (message in working) {
            if (message.role != LLMMessage.Role.USER) {
                cleaned.add(message)
                continue
            }
            // Remove outputs that are not adjacent to this assistant call.
            // Even when the same id exists elsewhere in the request, Responses
            // associates function_call_output with the immediately preceding
            // function_call item; a later stray output is still invalid.
            val adjacentUseIds = if (message.role == LLMMessage.Role.USER && cleaned.lastOrNull()?.role == LLMMessage.Role.ASSISTANT) {
                cleaned.last().contentParts.filterIsInstance<AgentContentPart.ToolUse>().map { it.id }.toSet()
            } else emptySet()
            val parts = message.contentParts.filter { part ->
                val keep = part !is AgentContentPart.ToolResult ||
                    (part.id in validUseIds && part.id in adjacentUseIds)
                if (!keep) removed++
                keep
            }
            if (parts.isEmpty() && message.content.isBlank() &&
                message.imageParts.isEmpty() && message.audioParts.isEmpty()
            ) {
                continue
            }
            cleaned.add(if (parts.size == message.contentParts.size) message else message.copy(contentParts = parts))
        }

        return Result(cleaned, removed, inserted)
    }
}

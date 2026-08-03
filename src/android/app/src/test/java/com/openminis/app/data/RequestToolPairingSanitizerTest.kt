package com.openminis.app.data

import com.openminis.app.data.model.AgentContentPart
import com.openminis.app.data.model.LLMMessage
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestToolPairingSanitizerTest {
    private fun toolUse(id: String) = AgentContentPart.ToolUse(id, "shell_execute", JSONObject("{\"command\":\"true\"}"))
    private fun toolResult(id: String) = AgentContentPart.ToolResult(id, "shell_execute", "ok", false)

    @Test
    fun `removes function output whose tool call was compacted away`() {
        val result = RequestToolPairingSanitizer.sanitize(
            listOf(
                LLMMessage(LLMMessage.Role.USER, "summary"),
                LLMMessage(LLMMessage.Role.USER, "", contentParts = listOf(toolResult("call_orphan"))),
                LLMMessage(LLMMessage.Role.USER, "continue"),
            ),
        )

        assertEquals(1, result.removedOrphanResults)
        assertFalse(result.messages.flatMap { it.contentParts }.any {
            it is AgentContentPart.ToolResult && it.id == "call_orphan"
        })
        assertTrue(result.messages.any { it.content == "continue" })
    }

    @Test
    fun `keeps a complete adjacent tool exchange unchanged`() {
        val messages = listOf(
            LLMMessage(LLMMessage.Role.USER, "run"),
            LLMMessage(LLMMessage.Role.ASSISTANT, "", contentParts = listOf(toolUse("call_ok"))),
            LLMMessage(LLMMessage.Role.USER, "", contentParts = listOf(toolResult("call_ok"))),
        )
        val result = RequestToolPairingSanitizer.sanitize(messages)

        assertEquals(0, result.removedOrphanResults)
        assertEquals(0, result.insertedMissingResults)
        assertEquals(messages, result.messages)
    }

    @Test
    fun `inserts interrupted result for a surviving call without output`() {
        val result = RequestToolPairingSanitizer.sanitize(
            listOf(
                LLMMessage(LLMMessage.Role.USER, "run"),
                LLMMessage(LLMMessage.Role.ASSISTANT, "", contentParts = listOf(toolUse("call_missing"))),
                LLMMessage(LLMMessage.Role.USER, "next"),
            ),
        )

        assertEquals(1, result.insertedMissingResults)
        val inserted = result.messages[2].contentParts.filterIsInstance<AgentContentPart.ToolResult>()
        assertEquals("call_missing", inserted.single().id)
        assertTrue(inserted.single().isError)
        assertEquals("next", result.messages[3].content)
    }

    @Test
    fun `removes non adjacent result even when call id exists earlier`() {
        val result = RequestToolPairingSanitizer.sanitize(
            listOf(
                LLMMessage(LLMMessage.Role.USER, "run"),
                LLMMessage(LLMMessage.Role.ASSISTANT, "", contentParts = listOf(toolUse("call_old"))),
                LLMMessage(LLMMessage.Role.USER, "", contentParts = listOf(toolResult("call_old"))),
                LLMMessage(LLMMessage.Role.ASSISTANT, "done", contentParts = listOf(AgentContentPart.Text("done"))),
                LLMMessage(LLMMessage.Role.USER, "", contentParts = listOf(toolResult("call_old"))),
            ),
        )

        assertEquals(1, result.removedOrphanResults)
        assertEquals(1, result.messages.flatMap { it.contentParts }
            .filterIsInstance<AgentContentPart.ToolResult>().size)
    }

    @Test
    fun `removes only orphan result from mixed user message`() {
        val result = RequestToolPairingSanitizer.sanitize(
            listOf(LLMMessage(
                LLMMessage.Role.USER,
                "new instruction",
                contentParts = listOf(toolResult("call_orphan"), AgentContentPart.Text("new instruction")),
            )),
        )

        assertEquals(1, result.removedOrphanResults)
        assertEquals("new instruction", result.messages.single().content)
        assertEquals(1, result.messages.single().contentParts.size)
        assertTrue(result.messages.single().contentParts.single() is AgentContentPart.Text)
    }
}

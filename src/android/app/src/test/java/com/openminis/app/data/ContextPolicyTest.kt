package com.openminis.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextPolicyTest {
    @Test
    fun `large context warns before compact threshold`() {
        val policy = ContextPolicy.forContextWindow(200_000)
        assertEquals(160_000, policy.offloadThreshold)
        assertEquals(180_000, policy.compactThreshold)
        assertEquals(ContextPolicy.CheckResult.NEEDS_COMPACT, policy.check(180_000, 200_000))
    }

    @Test
    fun `small context remains exhausted instead of auto compacting`() {
        val policy = ContextPolicy.forContextWindow(48_000)
        assertEquals(0, policy.compactThreshold)
        assertEquals(ContextPolicy.CheckResult.EXHAUSTED, policy.check(38_000, 48_000))
    }

    @Test
    fun `compact threshold leaves enough headroom`() {
        val policy = ContextPolicy.forContextWindow(128_000)
        assertTrue(policy.compactThreshold < 128_000)
        assertEquals(ContextPolicy.CheckResult.OK, policy.check(policy.compactThreshold - 1, 128_000))
    }
}

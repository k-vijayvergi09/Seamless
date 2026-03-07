package com.samsung.android.seamless.service

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptAccumulatorTest {

    @Test
    fun `onData keeps cumulative transcript when backend sends growing partials`() {
        val accumulator = TranscriptAccumulator()

        assertEquals("hello", accumulator.onData("hello"))
        assertEquals("hello world", accumulator.onData("hello world"))
        assertEquals("hello world today", accumulator.onData("hello world today"))
    }

    @Test
    fun `onData avoids duplicating identical chunks`() {
        val accumulator = TranscriptAccumulator()

        assertEquals("testing one", accumulator.onData("testing one"))
        assertEquals("testing one", accumulator.onData("testing one"))
    }

    @Test
    fun `onEndSpeech commits active utterance and clears active state`() {
        val accumulator = TranscriptAccumulator()

        accumulator.onData("first sentence")
        assertEquals("first sentence", accumulator.onEndSpeech())
        assertEquals("first sentence", accumulator.combined())
    }

    @Test
    fun `reset seeds prior transcript and continues from it`() {
        val accumulator = TranscriptAccumulator()

        accumulator.reset("existing transcript")
        assertEquals("existing transcript", accumulator.combined())
        assertEquals("existing transcript new words", accumulator.onData("new words"))
    }

    @Test
    fun `merge handles suffix-prefix overlap`() {
        val accumulator = TranscriptAccumulator()

        assertEquals(
            "go to market now",
            accumulator.mergeTranscriptChunks("go to market", "market now")
        )
    }
}

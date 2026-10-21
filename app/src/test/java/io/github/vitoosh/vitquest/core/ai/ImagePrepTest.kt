package io.github.vitoosh.vitquest.core.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class ImagePrepTest {
    @Test
    fun scalesDownToTheLongestSideAndKeepsTheShape() {
        assertEquals(1024 to 768, ImagePrep.targetSize(4000, 3000, 1024))
        assertEquals(240 to 320, ImagePrep.targetSize(3000, 4000, 320))
        assertEquals(320 to 1, ImagePrep.targetSize(6400, 10, 320))
    }

    @Test
    fun neverEnlargesASmallImage() {
        assertEquals(300 to 200, ImagePrep.targetSize(300, 200, 1024))
    }
}

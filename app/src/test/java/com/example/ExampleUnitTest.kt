package com.example

import com.example.model.resolutionLabel
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun resolutionLabel_handlesStandardAndUltrawideAndOrientation() {
    assertNull(resolutionLabel(0, 1080))
    assertNull(resolutionLabel(1920, 0))
    assertEquals("1080p", resolutionLabel(1920, 1080))
    assertEquals("1080p", resolutionLabel(1080, 1920))
    assertEquals("1080p", resolutionLabel(1920, 800))
    assertEquals("4K", resolutionLabel(3840, 2160))
    assertEquals("8K", resolutionLabel(7680, 4320))
    assertEquals("1440p", resolutionLabel(2560, 1440))
    assertEquals("720p", resolutionLabel(1280, 720))
    assertEquals("SD", resolutionLabel(854, 480))
  }
}

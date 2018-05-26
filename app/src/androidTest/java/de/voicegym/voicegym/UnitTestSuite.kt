package de.voicegym.voicegym

import org.junit.runners.Suite
import org.junit.runner.RunWith


// Runs all unit tests.
@RunWith(Suite::class)
@Suite.SuiteClasses(MediaCodecTest::class)
class UnitTestSuite

package de.voicegym.voicegym.model

import org.junit.runners.Suite
import org.junit.runner.RunWith

// Runs all unit tests.
@RunWith(Suite::class)
@Suite.SuiteClasses(DatabaseTest::class)
class DbTestSuite

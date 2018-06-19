package de.voicegym.voicegym.recordActivity.fragments

import android.support.v4.app.Fragment

/**
 * this is necessary to make sure that an AbstractInstrumentFragment
 * both implements the InstrumentFragmentInterface while extending the Fragment class
 */
abstract class AbstractInstrumentFragment : Fragment(), InstrumentFragmentInterface {
}

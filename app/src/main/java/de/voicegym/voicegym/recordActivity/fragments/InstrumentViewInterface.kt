package de.voicegym.voicegym.recordActivity.fragments

import de.voicegym.voicegym.menu.settings.FourierInstrumentViewSettings

interface InstrumentViewInterface {
    /**
     * Informs the InstrumentView of changed settings
     */
    fun updateInstrumentViewSettings(settings: FourierInstrumentViewSettings)

}

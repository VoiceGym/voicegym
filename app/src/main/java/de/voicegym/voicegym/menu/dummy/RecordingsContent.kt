package de.voicegym.voicegym.menu.dummy

import java.util.ArrayList
import java.util.HashMap


object RecordingsContent {

    /**
     * An array of sample (dummy) items.
     */
    val ITEMS: MutableList<RecordingItem> = ArrayList()

    /**
     * A map of sample (dummy) items, by ID.
     */
    val ITEM_MAP: MutableMap<String, RecordingItem> = HashMap()

    private val COUNT = 25

    init {
        // Add some sample items.
        for (i in 1..COUNT) {
            addItem(createRecordingsItem(i))
        }
    }

    private fun addItem(item: RecordingItem) {
        ITEMS.add(item)
        ITEM_MAP.put(item.id, item)
    }

    private fun createRecordingsItem(position: Int): RecordingItem {
        return RecordingItem(position.toString(), "Item " + position, makeDetails(position))
    }

    private fun makeDetails(position: Int): String {
        val builder = StringBuilder()
        builder.append("Details about Item: ").append(position)
        for (i in 0..position - 1) {
            builder.append("\nMore details information here.")
        }
        return builder.toString()
    }

    /**
     * A dummy item representing a piece of content.
     */
    data class RecordingItem(val id: String, val content: String, val details: String) {
        override fun toString(): String = content
    }
}

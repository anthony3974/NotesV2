package com.tony3974.mynotes

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray

class Note(val id: Long, text: String) {
    var text by mutableStateOf(text)
}

// Holds the notes. A ViewModel survives screen rotation, so nothing is lost when the phone turns.
class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("preferences", Context.MODE_PRIVATE)
    private var nextId = 0L
    private var lastDeleted: Pair<Int, Note>? = null

    val notes = mutableStateListOf<Note>()

    // The note that was just added and should get the cursor
    var focusNoteId by mutableStateOf<Long?>(null)
        private set

    init {
        load()
    }

    fun add() {
        val note = Note(nextId++, "")
        notes.add(note)
        focusNoteId = note.id
    }

    fun focusHandled() {
        focusNoteId = null
    }

    fun edit(note: Note, text: String) {
        note.text = text
        save()
    }

    fun delete(note: Note) {
        val index = notes.indexOf(note)
        if (index < 0) return
        notes.removeAt(index)
        lastDeleted = index to note
        save()
    }

    // Puts the last deleted note back where it was
    fun undoDelete() {
        val (index, note) = lastDeleted ?: return
        notes.add(index.coerceAtMost(notes.size), note)
        lastDeleted = null
        save()
    }

    // All notes are stored together as one JSON list under a single key.
    // Empty notes are not saved (same as the original app).
    private fun save() {
        val json = JSONArray()
        notes.map { it.text }.filter { it.isNotEmpty() }.forEach { json.put(it) }
        prefs.edit { putString(NOTES_KEY, json.toString()) }
    }

    private fun load() {
        val json = JSONArray(prefs.getString(NOTES_KEY, null) ?: return)
        for (i in 0 until json.length()) {
            notes.add(Note(nextId++, json.getString(i)))
        }
    }

    private companion object {
        const val NOTES_KEY = "notes"
    }
}

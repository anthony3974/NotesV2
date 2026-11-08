package com.tony3974.mynotes

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tony3974.mynotes.ui.theme.MauiGray200
import com.tony3974.mynotes.ui.theme.MauiGray500
import com.tony3974.mynotes.ui.theme.MauiPrimary
import com.tony3974.mynotes.ui.theme.MauiPrimaryDark
import com.tony3974.mynotes.ui.theme.MauiPrimaryDarkText
import com.tony3974.mynotes.ui.theme.MyNotesTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // White status bar icons on the purple title bar, like the MAUI app
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            MyNotesTheme {
                MainPage()
            }
        }
    }
}

// Notes per column when the phone is sideways
private const val NOTES_PER_SIDE = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(vm: NotesViewModel = viewModel()) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Delete, then offer "Undo" for a few seconds
    val onDelete: (Note) -> Unit = { note ->
        vm.delete(note)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = "Note deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) vm.undoDelete()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing, // keeps notes above the keyboard and clear of notches
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MauiPrimary,
                    titleContentColor = Color.White,
                ),
                actions = {
                    TextButton(onClick = { vm.add() }) {
                        Text("Add", color = Color.White, fontSize = 16.sp)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            if (landscape) {
                // Sideways: notes 1-5 on the left, the rest on the right (scrolls past 10)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    NoteList(vm.notes.take(NOTES_PER_SIDE), vm, onDelete, Modifier.weight(1f), spacing = 0.dp)
                    NoteList(vm.notes.drop(NOTES_PER_SIDE), vm, onDelete, Modifier.weight(1f), spacing = 0.dp)
                }
            } else {
                NoteList(vm.notes, vm, onDelete, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NoteList(
    notes: List<Note>,
    vm: NotesViewModel,
    onDelete: (Note) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 15.dp,
) {
    val listState = rememberLazyListState()
    val focusId = vm.focusNoteId

    // Scroll a newly added note into view
    LaunchedEffect(focusId) {
        val index = notes.indexOfFirst { it.id == focusId }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        items(notes, key = { it.id }) { note ->
            NoteRow(
                text = note.text,
                onTextChange = { vm.edit(note, it) },
                onDelete = { onDelete(note) },
                requestFocus = note.id == focusId,
                onFocusRequested = vm::focusHandled,
            )
        }
    }
}

@Composable
private fun NoteRow(
    text: String,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
    requestFocus: Boolean,
    onFocusRequested: () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val focusRequester = remember { FocusRequester() }

    // Put the cursor in a newly added note
    if (requestFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            onFocusRequested()
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("my notes", fontSize = 14.sp) },
            textStyle = TextStyle(fontSize = 14.sp),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .focusRequester(focusRequester),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = if (dark) Color.White else Color.Black,
                unfocusedTextColor = if (dark) Color.White else Color.Black,
                focusedPlaceholderColor = if (dark) MauiGray500 else MauiGray200,
                unfocusedPlaceholderColor = if (dark) MauiGray500 else MauiGray200,
            ),
        )
        MauiButton("Del", onClick = onDelete)
    }
}

// Button styled like the MAUI default: purple, 8dp corners, 14sp text, 44dp min size
@Composable
private fun MauiButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val dark = isSystemInDarkTheme()
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (dark) MauiPrimaryDark else MauiPrimary,
            contentColor = if (dark) MauiPrimaryDarkText else Color.White,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text, fontSize = 14.sp)
    }
}

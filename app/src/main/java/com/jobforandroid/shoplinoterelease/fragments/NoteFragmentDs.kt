package com.jobforandroid.shoplinoterelease.fragments

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.jobforandroid.shoplinoterelease.BD.MainViewModel
import com.jobforandroid.shoplinoterelease.BD.NoteAdapterDs
import com.jobforandroid.shoplinoterelease.activities.MainApp
import com.jobforandroid.shoplinoterelease.activities.NewNoteActivityDs
import com.jobforandroid.shoplinoterelease.activities.NoteViewActivityDs
import com.jobforandroid.shoplinoterelease.databinding.FragmentNoteDsBinding
import com.jobforandroid.shoplinoterelease.entities.NoteItem
import com.jobforandroid.shoplinoterelease.ui.ds.DeleteNoteDialogDs

class NoteFragmentDs : BaseFragment(), NoteAdapterDs.Listener {
    private var _binding: FragmentNoteDsBinding? = null
    private val binding get() = _binding!!

    private lateinit var editLauncher: ActivityResultLauncher<Intent>
    private lateinit var noteViewLauncher: ActivityResultLauncher<Intent>
    private var adapter: NoteAdapterDs? = null
    private lateinit var defPref: SharedPreferences

    private val mainViewModel: MainViewModel by activityViewModels {
        MainViewModel.MainViewModelFactory((context?.applicationContext as MainApp).database)
    }

    override fun onClickNew() {
        editLauncher.launch(Intent(activity, NewNoteActivityDs::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onEditResult()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteDsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRcView()
        observer()
    }

    private fun initRcView() = with(binding) {
        defPref = activity?.let { PreferenceManager.getDefaultSharedPreferences(it) }!!
        rcViewNote.layoutManager = getLayoutManager()
        adapter = NoteAdapterDs(this@NoteFragmentDs, defPref)
        rcViewNote.adapter = adapter
    }

    private fun getLayoutManager(): RecyclerView.LayoutManager {
        return if (defPref.getString("note_style_key", "Linear") == "Linear") {
            LinearLayoutManager(activity)
        } else {
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        }
    }

    private fun observer() {
        mainViewModel.allNotes.observe(viewLifecycleOwner) {
            val hasNotes = it.isNotEmpty()
            binding.sectionHeader.root.visibility = if (hasNotes) View.VISIBLE else View.GONE
            binding.rcViewNote.visibility = if (hasNotes) View.VISIBLE else View.GONE
            binding.emptyState.visibility = if (hasNotes) View.GONE else View.VISIBLE
            adapter?.submitList(it)
        }
    }

    private fun onEditResult() {
        editLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result -> handleNoteResult(result.resultCode, result.data) }
        noteViewLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result -> handleNoteResult(result.resultCode, result.data) }
    }

    private fun handleNoteResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) return
        val resultNote = try {
            data?.getSerializableExtra(NEW_NOTE_KEY) as? NoteItem
        } catch (_: RuntimeException) {
            null
        } ?: return
        val editState = try {
            data?.getStringExtra(EDIT_STATE_KEY)
        } catch (_: RuntimeException) {
            null
        }
        when (editState) {
            "update" -> if (resultNote.id != null && resultNote.id > 0) {
                mainViewModel.updateNote(resultNote)
            }
            "new" -> if (resultNote.id == null) {
                mainViewModel.insertNote(resultNote)
            }
            "delete" -> if (resultNote.id != null && resultNote.id > 0) {
                mainViewModel.deleteNote(resultNote.id)
            }
        }
    }

    override fun onClickItem(note: NoteItem) {
        val intent = Intent(activity, NoteViewActivityDs::class.java).apply {
            putExtra(NoteViewActivityDs.NOTE_EXTRA, note)
        }
        noteViewLauncher.launch(intent)
    }

    override fun onEditClick(note: NoteItem) {
        editLauncher.launch(Intent(activity, NewNoteActivityDs::class.java).apply {
            putExtra(NEW_NOTE_KEY, note)
        })
    }

    override fun onDeleteClick(note: NoteItem) {
        DeleteNoteDialogDs.show(requireContext()) {
            note.id?.let { mainViewModel.deleteNote(it) }
        }
    }

    companion object {
        const val NEW_NOTE_KEY = "title_key"
        const val EDIT_STATE_KEY = "edit_state_key"

        @JvmStatic
        fun newInstance() = NoteFragmentDs()
    }

    override fun onDestroyView() {
        binding.rcViewNote.adapter = null
        adapter = null
        _binding = null
        super.onDestroyView()
    }
}

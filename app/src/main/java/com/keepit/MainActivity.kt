package com.keepit

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var prefs: SharedPreferences
    var isMessagingUnlocked = false
    private val SECRET_TRIGGER = "SMS"

    lateinit var pickImageLauncher: ActivityResultLauncher<Array<String>>
    lateinit var recordAudioPermission: ActivityResultLauncher<String>
    lateinit var cameraPermission: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("KeepitPrefs", Context.MODE_PRIVATE)
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        setupPermissionLaunchers()
        setupTabs()
    }

    private fun setupPermissionLaunchers() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                supportFragmentManager.findFragmentById(R.id.viewPager)?.let { frag ->
                    (frag as? JournalFragment)?.onImagePicked(it)
                }
            }
        }

        recordAudioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                supportFragmentManager.findFragmentById(R.id.viewPager)?.let { frag ->
                    (frag as? JournalFragment)?.startVoiceRecording()
                }
            } else {
                Toast.makeText(this, "❌ Autorisation microphone necessaire", Toast.LENGTH_SHORT).show()
            }
        }

        cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                supportFragmentManager.findFragmentById(R.id.viewPager)?.let { frag ->
                    (frag as? JournalFragment)?.dispatchTakePictureIntent()
                }
            } else {
                Toast.makeText(this, "❌ Autorisation appareil photo necessaire", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupTabs() {
        viewPager.adapter = ViewPagerAdapter(this)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "💖 Journal"
                1 -> { if (isMessagingUnlocked) tab.text = "🔒 Messages" else tab.view.visibility = android.view.View.GONE }
            }
        }.attach()
    }

    fun checkSecretCode(content: String) {
        if (content.trim() == SECRET_TRIGGER && !isMessagingUnlocked) {
            isMessagingUnlocked = true
            setupTabs()
            viewPager.currentItem = 1
            Toast.makeText(this, "🔓 Messagerie deverrouillee !", Toast.LENGTH_LONG).show()
        }
    }

    private inner class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = if (isMessagingUnlocked) 2 else 1
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> JournalFragment()
            1 -> MessagesFragment()
            else -> JournalFragment()
        }
    }

    fun saveNote(title: String, content: String, imageUri: String? = null, voicePath: String? = null) {
        val notes = JSONArray(prefs.getString("notes", "[]"))
        val note = JSONObject()
        note.put("title", title)
        note.put("content", content)
        note.put("timestamp", System.currentTimeMillis())
        if (imageUri != null) note.put("imageUri", imageUri)
        if (voicePath != null) note.put("voicePath", voicePath)
        notes.put(note)
        prefs.edit().putString("notes", notes.toString()).apply()
        checkSecretCode(content)
    }

    fun deleteNote(pos: Int) {
        val notes = JSONArray(prefs.getString("notes", "[]"))
        notes.remove(pos)
        prefs.edit().putString("notes", notes.toString()).apply()
    }

    fun getNotes(): JSONArray = JSONArray(prefs.getString("notes", "[]"))

    fun saveContact(name: String, num: String) {
        val contacts = JSONArray(prefs.getString("contacts", "[]"))
        val c = JSONObject()
        c.put("name", num)
        c.put("number", num)
        contacts.put(c)
        prefs.edit().putString("contacts", contacts.toString()).apply()
    }

    fun deleteContact(pos: Int) {
        val contacts = JSONArray(prefs.getString("contacts", "[]"))
        contacts.remove(pos)
        prefs.edit().putString("contacts", contacts.toString()).apply()
    }

    fun getContacts(): JSONArray = JSONArray(prefs.getString("contacts", "[]"))
}

// ============== FRAGMENT JOURNAL — NOTES VOCALES + IMAGES ==============
class JournalFragment : Fragment(R.layout.fragment_journal) {
    private lateinit var prefs: SharedPreferences
    private lateinit var llNotes: LinearLayout
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var btnSave: Button
    private lateinit var etSearch: EditText
    private lateinit var btnVoice: Button
    private lateinit var btnImage: Button
    private lateinit var btnCamera: Button
    private var selectedImageUri: String? = null
    private var voiceFilePath: String? = null
    private var recorder: MediaRecorder? = null
    private var isRecording = false
    private var photoFile: File? = null

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = requireActivity().getSharedPreferences("KeepitPrefs", Context.MODE_PRIVATE)

        llNotes = view.findViewById(R.id.llNotes)
        etTitle = view.findViewById(R.id.etTitle)
        etContent = view.findViewById(R.id.etContent)
        btnSave = view.findViewById(R.id.btnSaveNote)
        etSearch = view.findViewById(R.id.etSearch)
        btnVoice = view.findViewById(R.id.btnVoice)
        btnImage = view.findViewById(R.id.btnImage)
        btnCamera = view.findViewById(R.id.btnCamera)

        btnVoice.setOnClickListener { toggleVoiceRecording() }
        btnImage.setOnClickListener { pickImage() }
        btnCamera.setOnClickListener { takePhoto() }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()
            if (title.isNotEmpty() || content.isNotEmpty() || selectedImageUri != null || voiceFilePath != null) {
                (activity as MainActivity).saveNote(title, content, selectedImageUri, voiceFilePath)
                etTitle.text.clear()
                etContent.text.clear()
                selectedImageUri = null
                voiceFilePath = null
                Toast.makeText(requireContext(), "✅ Note sauvegardee", Toast.LENGTH_SHORT).show()
                loadNotes()
            } else {
                Toast.makeText(requireContext(), "⚠️ Remplis au moins un champ", Toast.LENGTH_SHORT).show()
            }
        }

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { filterNotes(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        loadNotes()
    }

    private fun toggleVoiceRecording() {
        if (isRecording) {
            stopVoiceRecording()
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecording()
            } else {
                (activity as MainActivity).recordAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    fun startVoiceRecording() {
        val audioDir = File(requireContext().filesDir, "audio")
        audioDir.mkdirs()
        val audioFile = File(audioDir, "note_${System.currentTimeMillis()}.3gp")
        voiceFilePath = audioFile.absolutePath

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(requireContext()) else MediaRecorder()
        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFile.absolutePath)
            prepare()
            start()
        }
        isRecording = true
        btnVoice.text = "⏹️ Enregistrement..."
        btnVoice.setBackgroundColor(0xFFFF0000.toInt())
        Toast.makeText(requireContext(), "🎙️ Enregistrement en cours...", Toast.LENGTH_SHORT).show()
    }

    private fun stopVoiceRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        isRecording = false
        btnVoice.text = "🎤 Note Vocale"
        btnVoice.setBackgroundColor(0xFFFF4081.toInt())
        Toast.makeText(requireContext(), "✅ Enregistrement sauvegarde", Toast.LENGTH_SHORT).show()
    }

    private fun pickImage() {
        (activity as MainActivity).pickImageLauncher.launch(arrayOf("image/*"))
    }

    fun onImagePicked(uri: Uri) {
        selectedImageUri = uri.toString()
        Toast.makeText(requireContext(), "📷 Image selectionnee", Toast.LENGTH_SHORT).show()
    }

    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent()
        } else {
            (activity as MainActivity).cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    fun dispatchTakePictureIntent() {
        val photoDir = File(requireContext().filesDir, "photos")
        photoDir.mkdirs()
        photoFile = File(photoDir, "photo_${System.currentTimeMillis()}.jpg")
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(requireContext().packageManager) != null) {
            val photoURI = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile!!)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(takePictureIntent, 1002)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1002 && resultCode == -1) {
            photoFile?.let { selectedImageUri = Uri.fromFile(it).toString() }
            Toast.makeText(requireContext(), "📸 Photo capturee", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadNotes() = filterNotes("")

    private fun filterNotes(query: String) {
        llNotes.removeAllViews()
        val notes = (activity as MainActivity).getNotes()
        val q = query.lowercase()
        for (i in notes.length() - 1 downTo 0) {
            val note = notes.getJSONObject(i)
            val title = note.optString("title", "")
            val content = note.optString("content", "")
            if (q.isNotEmpty() && !title.lowercase().contains(q) && !content.lowercase().contains(q)) continue

            val time = note.getLong("timestamp")
            val imageUri = note.optString("imageUri", "")
            val voicePath = note.optString("voicePath", "")

            val noteView = layoutInflater.inflate(R.layout.item_note, null)
            noteView.findViewById<TextView>(R.id.tvTitle).text = title
            noteView.findViewById<TextView>(R.id.tvContent).text = content
            noteView.findViewById<TextView>(R.id.tvDate).text = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", time)

            val imgPreview = noteView.findViewById<ImageView>(R.id.ivPreview)
            if (imageUri.isNotEmpty()) {
                try { imgPreview.setImageURI(Uri.parse(imageUri)) } catch (e: Exception) {}
                imgPreview.visibility = android.view.View.VISIBLE
            } else imgPreview.visibility = android.view.View.GONE

            val voiceIcon = noteView.findViewById<TextView>(R.id.tvVoice)
            if (voicePath.isNotEmpty()) voiceIcon.visibility = android.view.View.VISIBLE
            else voiceIcon.visibility = android.view.View.GONE

            noteView.findViewById<Button>(R.id.btnDelete).setOnClickListener {
                (activity as MainActivity).deleteNote(i)
                loadNotes()
                Toast.makeText(requireContext(), "🗑️ Note supprimee", Toast.LENGTH_SHORT).show()
            }

            llNotes.addView(noteView)
        }
    }
}

// ============== FRAGMENT MESSAGERIE ==============
class MessagesFragment : Fragment(R.layout.fragment_messages) {
    private lateinit var llContacts: LinearLayout

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        llContacts = view.findViewById(R.id.llContacts)
        view.findViewById<Button>(R.id.btnAddContact).setOnClickListener { showAddDialog() }
        loadContacts()
    }

    private fun showAddDialog() {
        val v = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("➕ Contact")
            .setView(v)
            .setPositiveButton("Ajouter") { _, _ ->
                val n = v.findViewById<EditText>(R.id.etName).text.toString().trim()
                val num = v.findViewById<EditText>(R.id.etNumber).text.toString().trim()
                if (n.isNotEmpty() && num.isNotEmpty()) {
                    (activity as MainActivity).saveContact(n, num)
                    loadContacts()
                }
            }.show()
    }

    private fun loadContacts() {
        llContacts.removeAllViews()
        val c = (activity as MainActivity).getContacts()
        for (i in 0 until c.length()) {
            val contact = c.getJSONObject(i)
            val v = layoutInflater.inflate(R.layout.item_contact, null)
            v.findViewById<TextView>(R.id.tvName).text = contact.getString("name")
            v.findViewById<TextView>(R.id.tvNumber).text = contact.getString("number")
            v.findViewById<Button>(R.id.btnDeleteContact).setOnClickListener {
                (activity as MainActivity).deleteContact(i)
                loadContacts()
            }
            llContacts.addView(v)
        }
    }
}

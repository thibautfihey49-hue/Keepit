package com.keepit

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
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

    private val SECRET_TRIGGER = "sms"

    lateinit var pickImageLauncher: ActivityResultLauncher<String>
    lateinit var recordAudioPermission: ActivityResultLauncher<String>
    lateinit var cameraPermission: ActivityResultLauncher<String>

    private var pendingPhotoFile: File? = null

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
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
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
                Toast.makeText(this, "❌ Autorisation micro nécessaire", Toast.LENGTH_SHORT).show()
            }
        }

        cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) dispatchTakePictureIntent()
            else Toast.makeText(this, "❌ Autorisation appareil photo nécessaire", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabs() {
        viewPager.adapter = ViewPagerAdapter(this)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "💖 Journal"
                1 -> tab.text = "🔒 Messages"
            }
        }.attach()

        tabLayout.getTabAt(1)?.view?.visibility = if (isMessagingUnlocked) View.VISIBLE else View.GONE
    }

    fun refreshTabs() {
        setupTabs()
    }

    fun checkSecretCode(content: String) {
        if (content.trim().lowercase() == SECRET_TRIGGER && !isMessagingUnlocked) {
            isMessagingUnlocked = true
            refreshTabs()
            viewPager.currentItem = 1
            Toast.makeText(this, "🔓 Messagerie déverrouillée ! ✨", Toast.LENGTH_LONG).show()
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
        val notes = JSONArray(prefs.getString("notes", "[]") ?: "[]")
        val note = JSONObject()
        note.put("title", title)
        note.put("content", content)
        note.put("timestamp", System.currentTimeMillis())
        if (!imageUri.isNullOrEmpty()) note.put("imageUri", imageUri)
        if (!voicePath.isNullOrEmpty()) note.put("voicePath", voicePath)
        notes.put(note)
        prefs.edit().putString("notes", notes.toString()).apply()
        checkSecretCode(content)
    }

    fun deleteNote(pos: Int) {
        val notes = JSONArray(prefs.getString("notes", "[]") ?: "[]")
        if (pos in 0 until notes.length()) {
            val newNotes = JSONArray()
            for (i in 0 until notes.length()) {
                if (i != pos) newNotes.put(notes.get(i))
            }
            prefs.edit().putString("notes", newNotes.toString()).apply()
        }
    }

    fun getNotes(): JSONArray = JSONArray(prefs.getString("notes", "[]") ?: "[]")

    fun saveContact(name: String, num: String) {
        val contacts = JSONArray(prefs.getString("contacts", "[]") ?: "[]")
        val c = JSONObject()
        c.put("name", name)
        c.put("number", num)
        contacts.put(c)
        prefs.edit().putString("contacts", contacts.toString()).apply()
    }

    fun deleteContact(pos: Int) {
        val contacts = JSONArray(prefs.getString("contacts", "[]") ?: "[]")
        if (pos in 0 until contacts.length()) {
            val newContacts = JSONArray()
            for (i in 0 until contacts.length()) {
                if (i != pos) newContacts.put(contacts.get(i))
            }
            prefs.edit().putString("contacts", newContacts.toString()).apply()
        }
    }

    fun getContacts(): JSONArray = JSONArray(prefs.getString("contacts", "[]") ?: "[]")

    fun dispatchTakePictureIntent() {
        val photoDir = File(filesDir, "photos")
        photoDir.mkdirs()
        pendingPhotoFile = File(photoDir, "photo_${System.currentTimeMillis()}.jpg")

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoURI = FileProvider.getUriForFile(this, "$packageName.fileprovider", pendingPhotoFile!!)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(takePictureIntent, 1002)
        }
    }

    @Deprecated("Deprecated in Android API, kept for compatibility with existing code")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1002 && resultCode == RESULT_OK) {
            pendingPhotoFile?.let { file ->
                val uri = Uri.fromFile(file)
                supportFragmentManager.findFragmentById(R.id.viewPager)?.let { frag ->
                    (frag as? JournalFragment)?.onImagePicked(uri)
                }
            }
        }
    }
}

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
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
            if (title.isNotEmpty() || content.isNotEmpty() || !selectedImageUri.isNullOrEmpty() || !voiceFilePath.isNullOrEmpty()) {
                (activity as MainActivity).saveNote(title, content, selectedImageUri, voiceFilePath)
                etTitle.text.clear()
                etContent.text.clear()
                selectedImageUri = null
                voiceFilePath = null
                Toast.makeText(requireContext(), "✅ Note sauvegardée ! ✨", Toast.LENGTH_SHORT).show()
                loadNotes()
            } else {
                Toast.makeText(requireContext(), "⚠️ Écris quelque chose d'abord !", Toast.LENGTH_SHORT).show()
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterNotes(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        loadNotes()
    }

    private fun toggleVoiceRecording() {
        if (isRecording) stopVoiceRecording()
        else {
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
        btnVoice.setBackgroundColor(0xFFFF4757.toInt())
        Toast.makeText(requireContext(), "🎙️ Enregistrement en cours...", Toast.LENGTH_SHORT).show()
    }

    private fun stopVoiceRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
        }
        recorder = null
        isRecording = false
        btnVoice.text = "🎤 Voix"
        btnVoice.setBackgroundColor(0xFF6C5CE7.toInt())
        Toast.makeText(requireContext(), "✅ Note vocale prête !", Toast.LENGTH_SHORT).show()
    }

    private fun pickImage() {
        (activity as MainActivity).pickImageLauncher.launch("image/*")
    }

    fun onImagePicked(uri: Uri) {
        selectedImageUri = uri.toString()
        Toast.makeText(requireContext(), "📷 Image sélectionnée ! ✨", Toast.LENGTH_SHORT).show()
    }

    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            (activity as MainActivity).dispatchTakePictureIntent()
        } else {
            (activity as MainActivity).cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun playVoiceNote(path: String) {
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(requireContext(), "❌ Fichier introuvable", Toast.LENGTH_SHORT).show()
            return
        }

        if (isPlaying) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
            Toast.makeText(requireContext(), "⏹️ Lecture arrêtée", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
                setOnCompletionListener {
                    isPlaying = false
                    Toast.makeText(requireContext(), "✅ Terminé !", Toast.LENGTH_SHORT).show()
                }
            }
            isPlaying = true
            Toast.makeText(requireContext(), "▶️ Lecture...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "❌ Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
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
            noteView.findViewById<TextView>(R.id.tvDate).text = android.text.format.DateFormat.format("dd/MM/yyyy • HH:mm", time)

            val imgPreview = noteView.findViewById<ImageView>(R.id.ivPreview)
            if (imageUri.isNotEmpty()) {
                try {
                    imgPreview.setImageURI(Uri.parse(imageUri))
                    imgPreview.visibility = View.VISIBLE
                    imgPreview.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(imageUri))
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivity(intent)
                    }
                } catch (_: Exception) {
                    imgPreview.visibility = View.GONE
                }
            } else {
                imgPreview.visibility = View.GONE
            }

            val voiceContainer = noteView.findViewById<LinearLayout>(R.id.llVoice)
            val btnPlayVoice = noteView.findViewById<Button>(R.id.btnPlayVoice)
            if (voicePath.isNotEmpty()) {
                voiceContainer.visibility = View.VISIBLE
                btnPlayVoice.setOnClickListener { playVoiceNote(voicePath) }
            } else {
                voiceContainer.visibility = View.GONE
            }

            noteView.findViewById<Button>(R.id.btnDelete).setOnClickListener {
                (activity as MainActivity).deleteNote(i)
                loadNotes()
                Toast.makeText(requireContext(), "🗑️ Note supprimée !", Toast.LENGTH_SHORT).show()
            }

            llNotes.addView(noteView)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {
        }
        mediaPlayer = null
        isPlaying = false
    }
}

class MessagesFragment : Fragment(R.layout.fragment_messages) {
    private lateinit var llContacts: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        llContacts = view.findViewById(R.id.llContacts)
        view.findViewById<Button>(R.id.btnAddContact).setOnClickListener { showAddDialog() }
        loadContacts()
    }

    private fun showAddDialog() {
        val v = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("➕ Nouveau Contact")
            .setView(v)
            .setPositiveButton("Ajouter ✨") { _, _ ->
                val n = v.findViewById<EditText>(R.id.etName).text.toString().trim()
                val num = v.findViewById<EditText>(R.id.etNumber).text.toString().trim()
                if (n.isNotEmpty() && num.isNotEmpty()) {
                    (activity as MainActivity).saveContact(n, num)
                    loadContacts()
                }
            }
            .show()
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

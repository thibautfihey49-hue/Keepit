package com.keepit

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var prefs: SharedPreferences
    private var isUnlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("KeepitPrefs", Context.MODE_PRIVATE)
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        viewPager.adapter = ViewPagerAdapter(this)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "💖 Journal"
                1 -> tab.text = "🔒 Messages"
            }
        }.attach()

        // Verrouiller l'onglet Messages
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == 1 && !isUnlocked) {
                    showPasswordDialog()
                }
            }
        })
    }

    private fun showPasswordDialog() {
        val savedPassword = prefs.getString("password", "")
        if (savedPassword.isNullOrEmpty()) {
            showSetPasswordDialog()
        } else {
            showVerifyPasswordDialog(savedPassword)
        }
    }

    private fun showSetPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password_set, null)
        val etPass1 = dialogView.findViewById<EditText>(R.id.etPassword1)
        val etPass2 = dialogView.findViewById<EditText>(R.id.etPassword2)

        android.app.AlertDialog.Builder(this)
            .setTitle("🔒 Creer mot de passe")
            .setView(dialogView)
            .setPositiveButton("Valider") { _, _ ->
                val p1 = etPass1.text.toString()
                val p2 = etPass2.text.toString()
                if (p1.length >= 4 && p1 == p2) {
                    prefs.edit().putString("password", p1).apply()
                    isUnlocked = true
                    Toast.makeText(this, "🔒 Messagerie deverrouillee", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "⚠️ Mot de passe trop court ou different", Toast.LENGTH_SHORT).show()
                    viewPager.currentItem = 0
                }
            }
            .setNegativeButton("Annuler") { _, _ -> viewPager.currentItem = 0 }
            .show()
    }

    private fun showVerifyPasswordDialog(savedPassword: String) {
        val etPass = EditText(this)
        etPass.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        etPass.hint = "Entrez le mot de passe"

        android.app.AlertDialog.Builder(this)
            .setTitle("🔒 Entrez le mot de passe")
            .setView(etPass)
            .setPositiveButton("Valider") { _, _ ->
                if (etPass.text.toString() == savedPassword) {
                    isUnlocked = true
                    Toast.makeText(this, "✅ Deverrouille", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "❌ Mot de passe incorrect", Toast.LENGTH_SHORT).show()
                    viewPager.currentItem = 0
                }
            }
            .setNegativeButton("Annuler") { _, _ -> viewPager.currentItem = 0 }
            .show()
    }

    private inner class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 2
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> JournalFragment()
                1 -> MessagesFragment()
                else -> JournalFragment()
            }
        }
    }

    // ========== Gestion des notes du Journal ==========
    fun saveNote(title: String, content: String) {
        val notesJson = prefs.getString("notes", "[]")
        val array = JSONArray(notesJson)
        val note = JSONObject()
        note.put("title", title)
        note.put("content", content)
        note.put("timestamp", System.currentTimeMillis())
        array.put(note)
        prefs.edit().putString("notes", array.toString()).apply()
    }

    fun getNotes(): JSONArray {
        return JSONArray(prefs.getString("notes", "[]"))
    }

    // ========== Gestion des contacts/messages ==========
    fun saveContact(name: String, number: String) {
        val contactsJson = prefs.getString("contacts", "[]")
        val array = JSONArray(contactsJson)
        val contact = JSONObject()
        contact.put("name", name)
        contact.put("number", number)
        array.put(contact)
        prefs.edit().putString("contacts", array.toString()).apply()
    }

    fun getContacts(): JSONArray {
        return JSONArray(prefs.getString("contacts", "[]"))
    }
}

// ============== FRAGMENT 1 : JOURNAL INTIME ==============
class JournalFragment : Fragment(R.layout.fragment_journal) {
    private lateinit var prefs: SharedPreferences
    private lateinit var llNotes: LinearLayout
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var btnSaveNote: Button

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = requireActivity().getSharedPreferences("KeepitPrefs", Context.MODE_PRIVATE)

        llNotes = view.findViewById(R.id.llNotes)
        etTitle = view.findViewById(R.id.etTitle)
        etContent = view.findViewById(R.id.etContent)
        btnSaveNote = view.findViewById(R.id.btnSaveNote)

        btnSaveNote.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()
            if (title.isNotEmpty() && content.isNotEmpty()) {
                (activity as MainActivity).saveNote(title, content)
                etTitle.text.clear()
                etContent.text.clear()
                Toast.makeText(requireContext(), "✅ Note sauvegardee", Toast.LENGTH_SHORT).show()
                loadNotes()
            } else {
                Toast.makeText(requireContext(), "⚠️ Remplis titre et contenu", Toast.LENGTH_SHORT).show()
            }
        }

        loadNotes()
    }

    private fun loadNotes() {
        llNotes.removeAllViews()
        val notes = (activity as MainActivity).getNotes()
        for (i in notes.length() - 1 downTo 0) {
            val note = notes.getJSONObject(i)
            val title = note.getString("title")
            val content = note.getString("content")
            val time = note.getLong("timestamp")

            val noteView = layoutInflater.inflate(R.layout.item_note, null)
            noteView.findViewById<TextView>(R.id.tvTitle).text = title
            noteView.findViewById<TextView>(R.id.tvContent).text = content
            noteView.findViewById<TextView>(R.id.tvDate).text = android.text.format.DateFormat.format("dd/MM HH:mm", time)

            llNotes.addView(noteView)
        }
    }
}

// ============== FRAGMENT 2 : MESSAGERIE DISCRÈTE ==============
class MessagesFragment : Fragment(R.layout.fragment_messages) {
    private lateinit var prefs: SharedPreferences
    private lateinit var llContacts: LinearLayout
    private lateinit var btnAddContact: Button

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = requireActivity().getSharedPreferences("KeepitPrefs", Context.MODE_PRIVATE)

        llContacts = view.findViewById(R.id.llContacts)
        btnAddContact = view.findViewById(R.id.btnAddContact)

        btnAddContact.setOnClickListener { showAddContactDialog() }

        loadContacts()
    }

    private fun showAddContactDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etNumber = dialogView.findViewById<EditText>(R.id.etNumber)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("➕ Nouveau contact")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { _, _ ->
                val name = etName.text.toString().trim()
                val number = etNumber.text.toString().trim()
                if (name.isNotEmpty() && number.isNotEmpty()) {
                    (activity as MainActivity).saveContact(name, number)
                    Toast.makeText(requireContext(), "✅ Contact ajoute", Toast.LENGTH_SHORT).show()
                    loadContacts()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun loadContacts() {
        llContacts.removeAllViews()
        val contacts = (activity as MainActivity).getContacts()
        for (i in 0 until contacts.length()) {
            val contact = contacts.getJSONObject(i)
            val name = contact.getString("name")
            val number = contact.getString("number")

            val contactView = layoutInflater.inflate(R.layout.item_contact, null)
            contactView.findViewById<TextView>(R.id.tvName).text = name
            contactView.findViewById<TextView>(R.id.tvNumber).text = number

            llContacts.addView(contactView)
        }
    }
}

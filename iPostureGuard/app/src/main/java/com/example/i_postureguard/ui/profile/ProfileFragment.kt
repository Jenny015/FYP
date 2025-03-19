package com.example.i_postureguard.ui.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.i_postureguard.R
import com.example.i_postureguard.Utils
import com.example.i_postureguard.databinding.FragmentProfileBinding
import com.example.i_postureguard.ui.login.FragmentLoginActivity
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var tr_carer: TableRow
    private lateinit var tr_add_carer : TableRow
    private lateinit var tr_edit_carer : TableRow
    private lateinit var btn_add_carer : Button
    private lateinit var btn_edit_carer : Button
    private lateinit var btn_remove_carer : Button
    private lateinit var tv_carer : TextView



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spinner: Spinner = view.findViewById(R.id.language_spinner)
        settingOfLangSpinner(spinner)

        val btnLoginLogout: Button = view.findViewById(R.id.btn_login_out)
        settingOfLoginLogoutButton(btnLoginLogout)

        if(Utils.isLogin(requireContext())){
            tr_carer = view.findViewById(R.id.tr_carer)
            tr_add_carer = view.findViewById(R.id.tr_add_carer)
            tr_edit_carer = view.findViewById(R.id.tr_edit_carer)
            btn_add_carer = view.findViewById(R.id.btn_add_carer)
            btn_edit_carer = view.findViewById(R.id.btn_edit_carer)
            btn_remove_carer = view.findViewById(R.id.btn_remove_carer)
            tv_carer = view.findViewById(R.id.tv_carer)
            val name : TextView = view.findViewById(R.id.tv_name)
            val gender : TextView = view.findViewById(R.id.tv_gender)
            val dob : TextView = view.findViewById(R.id.tv_dob)
            val age : TextView = view.findViewById(R.id.tv_age)
            fillUserInfo(name, gender, dob, age)
        }
    }

    private fun settingOfLangSpinner(spinner: Spinner){
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.language_options,
            R.layout.spinner_item
        )
        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinner.adapter = adapter

        val savedLanguage = Utils.getString(requireContext(), "lang", "en")
        spinner.setSelection(getSavedLanguagePos(savedLanguage))
        //Temp for DEV
        Toast.makeText(requireContext(), savedLanguage, Toast.LENGTH_LONG).show()

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedLanguage = parent.getItemAtPosition(position) as String
                val languageCode = getLanguageCode(selectedLanguage)
                if(languageCode != savedLanguage){
                    Utils.putString(requireContext(), "lang", languageCode)
                    Utils.changeAppLanguage(requireContext(), languageCode)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    private fun settingOfLoginLogoutButton(btn: Button){
        if(Utils.isLogin(requireContext())){ // Logout button if logged in
            btn.setOnClickListener {
                Utils.logout(requireContext())
                toLogin(requireContext())
            }
        } else { // Login button if logged out
            btn.text = getString(R.string.login)
            btn.setOnClickListener {
                toLogin(requireContext())
            }
        }
    }

    private fun getSavedLanguagePos(code: String): Int {
        return when (code) {
            "zh-rHK" -> 1
            else -> 0
        }
    }

    private fun getLanguageCode(language: String): String {
        return when (language) {
            "繁體中文" -> "zh-rHK"
            else -> "en"
        }
    }

    private fun toLogin(context: Context){
        val intent = Intent(this.requireContext(), FragmentLoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        (context as? Activity)?.finish()
    }

    private fun fillUserInfo(name: TextView, gender: TextView, dob: TextView, age: TextView){
        val user = Utils.getUserFromFirebase(requireContext(), Utils.getString(requireContext(), "phone", ""))
        refresh()
        name.text = user.name;
        gender.text = user.gender;
        dob.text = user.dob;
        age.text = calculateAge(user.dob).toString()
    }

    private fun calculateAge(birthDateString: String): Int {
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH)
        val birthDate = LocalDate.parse(birthDateString, formatter)
        val currentDate = LocalDate.now()

        return Period.between(birthDate, currentDate).years
    }

    private fun showAddCarerDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_carer, null)

        val btnClose: ImageButton = dialogView.findViewById(R.id.btn_back)
        val tvClose : TextView = dialogView.findViewById(R.id.tv_back)
        val emailEditText: EditText = dialogView.findViewById(R.id.et_email)
        val addButton: Button = dialogView.findViewById(R.id.btn_confirm)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
        addButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty() && isValidEmail(email)) {
                val updates: MutableMap<String, Any> = HashMap()
                updates["carer"] = email
                Utils.updateToFirebase(requireContext(), updates)
                refresh()
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter an email.", Toast.LENGTH_SHORT).show()
            }
        }
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        tvClose.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }

    private fun refresh() {
        val user = Utils.getLocalUser(requireContext())
        Toast.makeText(requireContext(), user.carer, Toast.LENGTH_SHORT).show()
        if(user.carer == null || user.carer == ""){
            tr_add_carer.visibility = View.VISIBLE
            tr_carer.visibility = View.GONE
            tr_edit_carer.visibility = View.GONE
            btn_add_carer.setOnClickListener {
                showAddCarerDialog()
            }
        } else {
            tr_carer.visibility = View.VISIBLE
            tr_edit_carer.visibility = View.VISIBLE
            tr_add_carer.visibility = View.GONE
            tv_carer.text = user.carer
            btn_edit_carer.setOnClickListener {
                showAddCarerDialog()
            }
            btn_remove_carer.setOnClickListener {
                removeCarer()
            }
        }
    }

    private fun removeCarer(){
        val updates: MutableMap<String, Any> = HashMap()
        updates["carer"] = ""
        Utils.updateToFirebase(requireContext(), updates)
        refresh()
    }
}

package jp.co.c_lis.ccl.morelocale.ui.locale_edit

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import dagger.hilt.android.AndroidEntryPoint
import jp.co.c_lis.ccl.morelocale.R
import jp.co.c_lis.ccl.morelocale.databinding.FragmentEditLocaleBinding
import jp.co.c_lis.ccl.morelocale.entity.LocaleItem
import jp.co.c_lis.ccl.morelocale.entity.Type
import jp.co.c_lis.ccl.morelocale.ui.locale_iso_list.LocaleIsoListFragment
import java.util.IllformedLocaleException

@AndroidEntryPoint
class EditLocaleFragment : Fragment(R.layout.fragment_edit_locale) {

    enum class MODE(
        val titleRes: Int,
        val positiveButtonLabelRes: Int,
        val showLabelInput: Boolean,
    ) {
        ADD(R.string.add_locale, R.string.add, true),
        EDIT(R.string.edit_locale, R.string.save, true),
        SET(R.string.set_custom_locale, R.string.set, false),
    }

    companion object {
        val TAG = EditLocaleFragment::class.simpleName

        const val RESULT_KEY_LOCALE_ITEM = "result_key_locale_item"

        private const val KEY_MODE = "key_mode"
        private const val KEY_LOCALE_ITEM = "key_locale_item"

        fun getAddInstance(): EditLocaleFragment {
            val args = Bundle().apply {
                putInt(KEY_MODE, MODE.ADD.ordinal)
            }
            return EditLocaleFragment().also {
                it.arguments = args
            }
        }

        fun getEditInstance(localeItem: LocaleItem): EditLocaleFragment {
            val args = Bundle().apply {
                putInt(KEY_MODE, MODE.EDIT.ordinal)
                putParcelable(KEY_LOCALE_ITEM, localeItem)
            }
            return EditLocaleFragment().also {
                it.arguments = args
            }
        }

        fun getSetInstance(localeItem: LocaleItem?): EditLocaleFragment {
            val args = Bundle().apply {
                putInt(KEY_MODE, MODE.SET.ordinal)
                putParcelable(KEY_LOCALE_ITEM, localeItem)
            }
            return EditLocaleFragment().also {
                it.arguments = args
            }
        }
    }

    private var binding: FragmentEditLocaleBinding? = null

    private var mode = MODE.ADD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mode = MODE.entries.toTypedArray()[requireArguments().getInt(KEY_MODE)]
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        setHasOptionsMenu(true)
    }

    private fun validation(): Boolean {
        val binding = binding ?: return false

        if (binding.inputLanguage.text.isNullOrEmpty()) {
            binding.textInputLayoutLanguage.error = getString(R.string.required)
            return false
        }

        return true
    }

    private fun createLocaleItem(editItem: LocaleItem?): LocaleItem? {
        val binding = binding ?: return null

        val label = binding.inputLabel.text.let {
            if (it.isNullOrBlank()) {
                null
            } else {
                it.toString()
            }
        }
        val language = binding.inputLanguage.text.let {
            if (it.isNullOrBlank()) {
                null
            } else {
                it.toString()
            }
        }
        val country = binding.inputCountry.text.let {
            if (it.isNullOrBlank()) {
                ""
            } else {
                it.toString()
            }
        }
        val variant = binding.inputVariant.text.let {
            if (it.isNullOrBlank()) {
                null
            } else {
                it.toString()
            }
        }
        val script = binding.inputScript.text.let {
            if (it.isNullOrBlank()) {
                null
            } else {
                it.toString()
            }
        }

        try {
            return LocaleItem(
                id = editItem?.id ?: 0,
                label = label,
                language = language,
                country = country,
                variant = variant,
                script = script,
            )
        } catch (ex: IllformedLocaleException) {
            Toast.makeText(
                context,
                ex.message,
                Toast.LENGTH_LONG
            ).show()
            return null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFragmentResultListener(Type.Iso639.name) { _, bundle ->
            val iso639Str = bundle.getString(LocaleIsoListFragment.RESULT_KEY_LOCALE)
                ?: return@setFragmentResultListener
            binding?.inputLanguage?.setText(iso639Str)
        }
        setFragmentResultListener(Type.Iso3166.name) { _, bundle ->
            val iso3166Str = bundle.getString(LocaleIsoListFragment.RESULT_KEY_LOCALE)
                ?: return@setFragmentResultListener
            binding?.inputCountry?.setText(iso3166Str)
        }

        val editItem = requireArguments().getParcelable<LocaleItem>(KEY_LOCALE_ITEM)
        binding = FragmentEditLocaleBinding.bind(view).also { binding ->
            val labelVisibility = if (mode.showLabelInput) {
                View.VISIBLE
            } else {
                View.GONE
            }
            arrayOf(binding.textInputLayoutLabel, binding.labelLocaleSpacer).forEach {
                it.visibility = labelVisibility
            }

            binding.buttonIso639.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.fragment_in, 0, 0, R.anim.fragment_out)
                    .add(R.id.fragment_container, LocaleIsoListFragment.getIso639Instance())
                    .addToBackStack(null)
                    .commit()
            }
            binding.buttonIso3166.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.fragment_in, 0, 0, R.anim.fragment_out)
                    .add(R.id.fragment_container, LocaleIsoListFragment.getIso3166Instance())
                    .addToBackStack(null)
                    .commit()
            }

            editItem?.also { localeItem ->
                binding.inputLabel.setText(localeItem.label)
                binding.inputLanguage.setText(localeItem.language)
                binding.inputCountry.setText(localeItem.country)
                binding.inputVariant.setText(localeItem.variant)
                binding.inputScript.setText(localeItem.script)
                localeItem.id
            }

            val activity = requireActivity()
            if (activity is AppCompatActivity) {
                setupActionBar(activity, binding.toolbar)
            }
        }
    }

    private fun setupActionBar(activity: AppCompatActivity, toolbar: Toolbar) {
        toolbar.setTitle(mode.titleRes)

        activity.setSupportActionBar(toolbar)
        activity.supportActionBar?.also {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.clear)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        menu.clear()

        inflater.inflate(R.menu.fragment_edit_locale, menu)

        menu.findItem(R.id.menu_set).also {
            it.setTitle(mode.positiveButtonLabelRes)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                parentFragmentManager.popBackStack()
                true
            }

            R.id.menu_set -> {
                if (!validation()) {
                    return true
                }
                val editItem = requireArguments().getParcelable<LocaleItem>(KEY_LOCALE_ITEM)
                createLocaleItem(editItem)?.also { localeItem ->
                    setFragmentResult(mode.name, bundleOf(RESULT_KEY_LOCALE_ITEM to localeItem))
                    parentFragmentManager.popBackStack()
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding?.unbind()
    }
}

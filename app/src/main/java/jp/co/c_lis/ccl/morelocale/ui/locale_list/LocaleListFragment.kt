package jp.co.c_lis.ccl.morelocale.ui.locale_list

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import jp.co.c_lis.ccl.morelocale.R
import jp.co.c_lis.ccl.morelocale.databinding.FragmentLocaleListBinding
import jp.co.c_lis.ccl.morelocale.entity.LocaleItem
import jp.co.c_lis.ccl.morelocale.ui.AboutDialog
import jp.co.c_lis.ccl.morelocale.ui.ConfirmDialog
import jp.co.c_lis.ccl.morelocale.ui.help.PermissionRequiredDialog
import jp.co.c_lis.ccl.morelocale.ui.license.LicenseFragment
import jp.co.c_lis.ccl.morelocale.ui.locale_edit.EditLocaleFragment
import jp.co.c_lis.ccl.morelocale.widget.WrapContentLinearLayoutManager
import timber.log.Timber


@AndroidEntryPoint
class LocaleListFragment : Fragment(R.layout.fragment_locale_list) {
    companion object {
        fun getInstance(): LocaleListFragment {
            return LocaleListFragment()
        }
    }

    private var binding: FragmentLocaleListBinding? = null

    private val viewModel: LocaleListViewModel by viewModels()

    private val permissionDialog = PermissionRequiredDialog.getInstance()

    private val menuCallback = object : LocaleListAdapter.MenuCallback {
        override fun onEdit(localeItem: LocaleItem) {
            onEditLocale(localeItem)
        }

        override fun onDelete(localeItem: LocaleItem) {
            ConfirmDialog.show(
                targetFragment = this@LocaleListFragment,
                title = "",
                message = getString(R.string.confirm_delete),
                positiveButtonLabel = getString(R.string.delete),
                negativeButtonLabel = getString(android.R.string.cancel),
                onPositiveButtonClicked = { viewModel.deleteLocale(localeItem) },
            )
        }
    }

    private val currentListItemMenuCallback = object : CurrentLocaleListAdapter.MenuCallback {
        override fun onEdit(localeItem: LocaleItem) {
            onEditLocale(localeItem)
        }

        override fun onDelete(localeItem: LocaleItem) {
            ConfirmDialog.show(
                targetFragment = this@LocaleListFragment,
                title = "",
                message = getString(R.string.confirm_delete),
                positiveButtonLabel = getString(R.string.delete),
                negativeButtonLabel = getString(android.R.string.cancel),
                onPositiveButtonClicked = { viewModel.delFromCurrentLocales(localeItem) },
            )
        }
    }

    private fun onEditLocale(localeItem: LocaleItem) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fragment_in,
                R.anim.fragment_out,
                R.anim.fragment_in,
                R.anim.fragment_out
            )
            .add(R.id.fragment_container, EditLocaleFragment.getEditInstance(localeItem))
            .addToBackStack(null)
            .commit()
    }

    private var adapter: LocaleListAdapter? = null
    private var currentListItemAdapter: CurrentLocaleListAdapter? = null
    private val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {

        private var startPosition: Int = 0
        private var endPosition: Int = 0

        override fun isLongPressDragEnabled() = false

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
        ) = makeMovementFlags(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        )

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            endPosition = target.absoluteAdapterPosition
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            if (viewHolder != null && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                startPosition = viewHolder.absoluteAdapterPosition
            } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                viewModel.moveInCurrentLocales(startPosition, endPosition)
            }
        }
    })

    override fun onAttach(context: Context) {
        super.onAttach(context)

        setHasOptionsMenu(true)

        adapter = LocaleListAdapter(
            LayoutInflater.from(context),
            menuCallback
        ) { localeItem ->
            Timber.d("Change locale ${localeItem.displayName}")
            setLocale(localeItem)
        }

        currentListItemAdapter = CurrentLocaleListAdapter(
            LayoutInflater.from(context),
            currentListItemMenuCallback,
            itemTouchHelper::startDrag
        )
    }

    private fun setLocale(localeItem: LocaleItem) {
        viewModel.addToCurrentLocales(localeItem)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFragmentResultListener(EditLocaleFragment.MODE.ADD.name) { requestKey, bundle ->
            val localeItemAdded =
                bundle.getParcelable<LocaleItem>(EditLocaleFragment.RESULT_KEY_LOCALE_ITEM)
                    ?: return@setFragmentResultListener
            Timber.d("Fragment Result $requestKey ${localeItemAdded.displayName}")
            viewModel.addLocale(localeItemAdded)
        }
        setFragmentResultListener(EditLocaleFragment.MODE.EDIT.name) { requestKey, bundle ->
            Timber.d("Fragment Result $requestKey")
            val localeItemEdited =
                bundle.getParcelable<LocaleItem>(EditLocaleFragment.RESULT_KEY_LOCALE_ITEM)
                    ?: return@setFragmentResultListener
            viewModel.editLocale(localeItemEdited)
        }
        setFragmentResultListener(EditLocaleFragment.MODE.SET.name) { requestKey, bundle ->
            Timber.d("Fragment Result $requestKey")
            val localeItem =
                bundle.getParcelable<LocaleItem>(EditLocaleFragment.RESULT_KEY_LOCALE_ITEM)
                    ?: return@setFragmentResultListener
            setLocale(localeItem)
        }

        viewModel.currentLocales.observe(viewLifecycleOwner) { currentLocale ->
            currentListItemAdapter?.also {
                it.submitList(currentLocale)
            }
        }

        viewModel.canSave.observe(viewLifecycleOwner) { canSave ->
            binding?.also {
                it.canSave = canSave
            }
        }

        viewModel.localeList.observe(viewLifecycleOwner) { localeItemList ->
            adapter?.also {
                it.submitList(localeItemList) {
                    binding?.progress?.visibility = View.GONE
                }
            }
        }

        viewModel.alertsEvents.observe(viewLifecycleOwner) { typeAlert ->
            Timber.d("typeAlert: $typeAlert, ${permissionDialog.isAdded}")
            when (typeAlert) {
                AlertsMoreLocale.NEED_PERMISSION -> {
                    permissionDialog.show(parentFragmentManager, PermissionRequiredDialog.TAG)
                }

                else -> {}
            }
        }

        binding = FragmentLocaleListBinding.bind(view).also { binding ->
            binding.lifecycleOwner = viewLifecycleOwner
            binding.recyclerView.layoutManager = WrapContentLinearLayoutManager(
                requireContext(), LinearLayoutManager.VERTICAL, false
            )
            binding.recyclerView.adapter = adapter
            binding.recyclerViewCurrentLocales.layoutManager = WrapContentLinearLayoutManager(
                requireContext(), LinearLayoutManager.VERTICAL, false
            )
            binding.recyclerViewCurrentLocales.adapter = currentListItemAdapter

            itemTouchHelper.attachToRecyclerView(binding.recyclerViewCurrentLocales)

            binding.saveLocalesSettingButton.setOnClickListener {
                viewModel.setLocales()
            }

            val activity = requireActivity()
            if (activity is AppCompatActivity) {
                setupActionBar(activity, binding.toolbar)
            }
        }

        viewModel.loadLocaleListAndCurrentLocaleList(requireContext())
    }

    private fun setupActionBar(activity: AppCompatActivity, toolBar: Toolbar) {
        activity.setSupportActionBar(toolBar)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.fragment_locale_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add_locale -> {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.fragment_in,
                        R.anim.fragment_out,
                        R.anim.fragment_in,
                        R.anim.fragment_out
                    )
                    .add(R.id.fragment_container, EditLocaleFragment.getAddInstance())
                    .addToBackStack(null)
                    .commit()
            }

            R.id.menu_license -> {
                parentFragmentManager.beginTransaction()
                    .add(R.id.fragment_container, LicenseFragment.getInstance())
                    .addToBackStack(null)
                    .commit()
            }

            R.id.menu_about -> {
                AboutDialog.getInstance()
                    .show(parentFragmentManager, AboutDialog.TAG)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding?.unbind()
    }
}

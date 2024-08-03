package jp.co.c_lis.ccl.morelocale.ui.locale_list

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import jp.co.c_lis.ccl.morelocale.R
import jp.co.c_lis.ccl.morelocale.databinding.ListItemLocaleInCurrentLocaleBinding
import jp.co.c_lis.ccl.morelocale.entity.LocaleItem

class CurrentLocaleListAdapter(
    private val inflater: LayoutInflater,
    private val menuCallback: MenuCallback? = null,
    private val startDragCallback: (RecyclerView.ViewHolder) -> Unit,
) : ListAdapter<LocaleItem, RecyclerView.ViewHolder>(LocaleItem.itemDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return LocaleItemViewHolder(
            inflater.inflate(R.layout.list_item_locale_in_current_locale, parent, false),
            menuCallback, startDragCallback
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is LocaleItemViewHolder) {
            holder.bind(getItem(position))
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is LocaleItemViewHolder) {
            holder.unbind()
        }
    }

    class LocaleItemViewHolder(
        itemView: View,
        private val menuCallback: MenuCallback?,
        private val startDragCallback: (RecyclerView.ViewHolder) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {
        private val binding = ListItemLocaleInCurrentLocaleBinding.bind(itemView)

        @SuppressLint("ClickableViewAccessibility")
        fun bind(localeItem: LocaleItem) {
            binding.locale = localeItem

            binding.more.visibility = View.VISIBLE
            binding.more.setOnClickListener {
                showPopupMenu(it, localeItem)
            }
            binding.dragHandler.performClick()
            binding.dragHandler.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    startDragCallback(this)
                }
                return@setOnTouchListener false
            }
        }

        private fun showPopupMenu(it: View, localeItem: LocaleItem) {
            PopupMenu(itemView.context, it).also { popupMenu ->
                popupMenu.menuInflater.inflate(
                    R.menu.list_item_current_locale,
                    popupMenu.menu
                )
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.menu_delete -> menuCallback?.onDelete(localeItem)
                        R.id.menu_edit -> menuCallback?.onEdit(localeItem)
                    }
                    return@setOnMenuItemClickListener true
                }
            }.show()
        }

        fun unbind() {
            binding.unbind()
        }
    }

    interface MenuCallback {
        fun onEdit(localeItem: LocaleItem)
        fun onDelete(localeItem: LocaleItem)
    }
}

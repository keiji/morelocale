package jp.co.c_lis.ccl.morelocale.ui.locale_list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import jp.co.c_lis.ccl.morelocale.R
import jp.co.c_lis.ccl.morelocale.databinding.ListItemLocaleBinding
import jp.co.c_lis.ccl.morelocale.entity.LocaleItem

class CurrentLocaleListAdapter(
    private val inflater: LayoutInflater,
    private val menuCallback: MenuCallback? = null,
) : ListAdapter<LocaleItem, RecyclerView.ViewHolder>(LocaleItem.itemDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return LocaleItemViewHolder(
            inflater.inflate(R.layout.list_item_locale, parent, false),
            menuCallback
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
    ) : RecyclerView.ViewHolder(itemView) {
        private val binding = ListItemLocaleBinding.bind(itemView)

        fun bind(localeItem: LocaleItem) {
            binding.locale = localeItem

            binding.more.visibility = View.VISIBLE
            binding.root.setOnLongClickListener {
                showPopupMenu(binding.more, localeItem)
                return@setOnLongClickListener true
            }
            binding.more.setOnClickListener {
                showPopupMenu(it, localeItem)
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
                        R.id.menu_up -> menuCallback?.onMove(localeItem, true)
                        R.id.menu_down -> menuCallback?.onMove(localeItem, false)
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
        fun onMove(localeItem: LocaleItem, isUp: Boolean)
        fun onEdit(localeItem: LocaleItem)
        fun onDelete(localeItem: LocaleItem)
    }
}

package dev.bartuzen.qbitcontroller.ui.main

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.databinding.ItemServerBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.utils.getThemeColor
import dev.bartuzen.qbitcontroller.utils.themeColors

class ServerListAdapter(
    private val onClick: (serverConfig: ServerConfig) -> Unit,
    private val onLongClick: (serverConfig: ServerConfig) -> Unit
) : ListAdapter<ServerConfig, ServerListAdapter.ViewHolder>(DiffCallBack()) {
    var selectedServerId: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemServerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { serverConfig ->
            holder.bind(serverConfig)
        }
    }

    inner class ViewHolder(private val binding: ItemServerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { serverConfig ->
                        currentList.forEachIndexed { i, config ->
                            if (config.id == selectedServerId || config.id == serverConfig.id) {
                                notifyItemChanged(i)
                            }
                        }
                        selectedServerId = serverConfig.id

                        onClick(serverConfig)
                    }
                }
            }

            binding.root.setOnLongClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { serverConfig ->
                        onLongClick(serverConfig)
                    }
                }
                true
            }
        }

        fun bind(serverConfig: ServerConfig) {
            val context = binding.root.context

            val backgroundColor = if (serverConfig.id == selectedServerId) {
                context.getThemeColor(themeColors.colorPrimaryVariant, alpha = 35)
            } else {
                Color.TRANSPARENT
            }
            binding.root.setBackgroundColor(backgroundColor)

            if (serverConfig.name != null) {
                binding.textName.text = serverConfig.name
            } else {
                binding.textName.visibility = View.GONE
            }
            binding.textUrl.text = serverConfig.visibleUrl
        }
    }

    class DiffCallBack : DiffUtil.ItemCallback<ServerConfig>() {
        override fun areItemsTheSame(oldItem: ServerConfig, newItem: ServerConfig) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ServerConfig, newItem: ServerConfig) =
            oldItem.name == newItem.name && oldItem.visibleUrl == newItem.visibleUrl
    }
}

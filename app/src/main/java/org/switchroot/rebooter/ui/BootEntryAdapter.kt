package org.switchroot.rebooter.ui

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.switchroot.rebooter.R
import org.switchroot.rebooter.databinding.ItemBootEntryBinding
import org.switchroot.rebooter.ini.IconResolver
import org.switchroot.rebooter.model.BootEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BootEntryAdapter(
    private val onSelected: (BootEntry) -> Unit
) : ListAdapter<BootEntry, BootEntryAdapter.ViewHolder>(DIFF) {

    // Caché en memoria para no decodificar el mismo bitmap varias veces
    private val iconCache = LruCache<String, Bitmap>(30)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBootEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onSelected, iconCache)
    }

    class ViewHolder(private val binding: ItemBootEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val defaultTint = binding.icon.imageTintList
        private var iconJob: Job? = null

        fun bind(entry: BootEntry, onSelected: (BootEntry) -> Unit, cache: LruCache<String, Bitmap>) {
            val context = binding.root.context
            iconJob?.cancel() 

            val label: String
            binding.icon.setImageDrawable(null)
            binding.label.isSelected = true // Necesario para que el marquee funcione

            when (entry) {
                is BootEntry.HekateEntry -> {
                    label = entry.label
                    val iconUri = entry.iconUri
                    if (iconUri != null) {
                        val cacheKey = iconUri.toString()
                        val cached = cache.get(cacheKey)
                        if (cached != null) {
                            binding.icon.setImageBitmap(cached)
                            binding.icon.imageTintList = null
                        } else {
                            // Usamos el scope de la actividad para asegurar la carga inmediata
                            val scope = (context as? AppCompatActivity)?.lifecycleScope
                            iconJob = scope?.launch {
                                val bitmap = withContext(Dispatchers.IO) {
                                    try {
                                        context.contentResolver.openInputStream(iconUri)?.use {
                                            BitmapFactory.decodeStream(it)
                                        }
                                    } catch (e: Exception) { null }
                                }
                                if (bitmap != null) {
                                    cache.put(cacheKey, bitmap)
                                    binding.icon.setImageBitmap(bitmap)
                                    binding.icon.imageTintList = null
                                } else {
                                    binding.icon.setImageResource(IconResolver.resolve(entry.iconToken))
                                    binding.icon.imageTintList = defaultTint
                                }
                            }
                        }
                    } else {
                        binding.icon.setImageResource(IconResolver.resolve(entry.iconToken))
                        binding.icon.imageTintList = defaultTint
                    }
                }

                is BootEntry.SystemAction -> {
                    label = when (entry.action) {
                        BootEntry.SystemAction.Action.RESTART -> context.getString(R.string.action_restart)
                        BootEntry.SystemAction.Action.SHUTDOWN -> context.getString(R.string.action_shutdown)
                    }
                    val iconRes = when (entry.action) {
                        BootEntry.SystemAction.Action.RESTART -> R.drawable.ic_boot_restart
                        BootEntry.SystemAction.Action.SHUTDOWN -> R.drawable.ic_boot_shutdown
                    }
                    binding.icon.setImageResource(iconRes)
                    binding.icon.imageTintList = defaultTint
                }
            }

            binding.label.text = label
            binding.root.contentDescription = label
            binding.root.nextFocusUpId = R.id.toggleHekateIpl
            binding.root.setOnClickListener { onSelected(entry) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BootEntry>() {
            override fun areItemsTheSame(oldItem: BootEntry, newItem: BootEntry) = oldItem == newItem
            override fun areContentsTheSame(oldItem: BootEntry, newItem: BootEntry) = oldItem == newItem
        }
    }
}

package com.example.snapitout

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class TemplateAdapter(
    private val context: Context,
    private val items: MutableList<Template>,
    private val onTemplateClick: (Template) -> Unit,
    private val onCreateClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CREATE = 0
        private const val TYPE_TEMPLATE = 1
    }

    override fun getItemViewType(position: Int) = if (position == 0) TYPE_CREATE else TYPE_TEMPLATE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_CREATE) {
            val v = LayoutInflater.from(context).inflate(R.layout.item_template_create_tile, parent, false)
            CreateVH(v)
        } else {
            val v = LayoutInflater.from(context).inflate(R.layout.item_template_vertical_strip, parent, false)
            TemplateVH(v)
        }
    }

    override fun getItemCount() = items.size + 1

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CreateVH) {
            holder.itemView.setOnClickListener { onCreateClick() }
        } else if (holder is TemplateVH) {
            val template = items[position - 1]
            holder.name.text = template.name
            val thumbnailUri = template.slotUris.firstOrNull()
            if (!thumbnailUri.isNullOrEmpty()) {
                Glide.with(context).load(thumbnailUri).centerCrop().placeholder(R.drawable.placeholder_image).into(holder.thumb)
            } else {
                holder.thumb.setImageResource(R.drawable.placeholder_image)
            }
            holder.itemView.setOnClickListener { onTemplateClick(template) }
        }
    }

    class TemplateVH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvTemplateName)
        val thumb: ImageView = view.findViewById(R.id.ivTemplateThumb)
    }

    class CreateVH(view: View) : RecyclerView.ViewHolder(view)
}

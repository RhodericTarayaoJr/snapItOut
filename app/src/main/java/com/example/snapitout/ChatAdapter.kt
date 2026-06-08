package com.example.snapitout

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ChatAdapter(private val messages: ArrayList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
        val messageImage: ImageView = view.findViewById(R.id.messageImage)
        val container: LinearLayout = view.findViewById(R.id.messageContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = messages[position]

        val params = holder.container.layoutParams as ViewGroup.MarginLayoutParams

        // RESET VISIBILITY FIRST (VERY IMPORTANT IN RECYCLERVIEW)
        holder.messageText.visibility = View.GONE
        holder.messageImage.visibility = View.GONE

        // ALIGNMENT + STYLE
        if (msg.isUser) {
            holder.container.gravity = android.view.Gravity.END
            holder.container.setBackgroundResource(R.drawable.user_bubble)
        } else {
            holder.container.gravity = android.view.Gravity.START
            holder.container.setBackgroundResource(R.drawable.ai_bubble)
        }
        holder.container.layoutParams = params

        // TEXT
        msg.text?.let {
            holder.messageText.visibility = View.VISIBLE
            holder.messageText.text = it
        }

        // IMAGE
        msg.imageUrl?.let {
            holder.messageImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(it)
                .into(holder.messageImage)
        }
    }

    override fun getItemCount(): Int = messages.size
}
package com.example.sms.logs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sms.R

class SmsLogAdapter(private var logs: List<SmsLogEntity>) : RecyclerView.Adapter<SmsLogAdapter.SmsLogViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmsLogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sms_log, parent, false)
        return SmsLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: SmsLogViewHolder, position: Int) {
        val log = logs[position]
        holder.senderText.text = log.sender
        holder.messageText.text = log.sms_text
        holder.timestampText.text = log.timestamp.toString()
    }

    override fun getItemCount(): Int {
        return logs.size
    }

    fun updateLogs(newLogs: List<SmsLogEntity>) {
        logs = newLogs
        notifyDataSetChanged()
    }

    class SmsLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderText: TextView = itemView.findViewById(R.id.senderText)
        val messageText: TextView = itemView.findViewById(R.id.messageText)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
    }
}

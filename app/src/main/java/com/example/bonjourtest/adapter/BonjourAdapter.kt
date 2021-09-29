package com.example.bonjourtest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bonjourtest.R
import com.example.bonjourtest.databinding.ItemUserBinding
import com.example.bonjourtest.models.Device

class BonjourAdapter(var devices: ArrayList<Device>) :
    RecyclerView.Adapter<BonjourAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vBind = ItemUserBinding.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val vBind = holder.vBind
        val device = devices[position]
        vBind.tvName.text = device.name
    }

    override fun getItemCount(): Int = devices.size
}

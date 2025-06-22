package com.example.apiforeca

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LocationViewHolder(parent: ViewGroup) :
         RecyclerView.ViewHolder(LayoutInflater.from(parent.context)

             .inflate(R.layout.item_city, parent, false)) {

             var name: TextView = itemView.findViewById(R.id.locationName)

    fun bind(location: ForecastLocation) {
        name.text = "${location.name} (${location.country})"
    }
}
package com.smartsolutions.paquetes.ui.usage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.databinding.ItemUsageDetailsBinding
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.managers.models.Traffic
import java.text.SimpleDateFormat
import java.util.*

class UsageAppDetailsRecyclerAdapter(
    var traffics: Pair<List<Traffic>, DateCalendarUtils.MyTimeUnit>
): RecyclerView.Adapter<UsageAppDetailsRecyclerAdapter.ViewHolder>() {


    inner class ViewHolder(var binding: ItemUsageDetailsBinding?): RecyclerView.ViewHolder(binding!!.root) {

        fun onBind(traffic: Traffic, myTimeUnit: DateCalendarUtils.MyTimeUnit) {

            binding?.apply {
                val date = when (myTimeUnit) {
                    DateCalendarUtils.MyTimeUnit.HOUR, DateCalendarUtils.MyTimeUnit.MINUTE -> {
                        SimpleDateFormat("hh:mm aa", Locale.US).format(Date(traffic.startTime))
                    }
                    DateCalendarUtils.MyTimeUnit.DAY -> {
                        "Día " + SimpleDateFormat("dd", Locale.getDefault()).format(Date(traffic.startTime))
                    }
                    DateCalendarUtils.MyTimeUnit.MONTH -> {
                        SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(traffic.startTime))
                    }
                }

                textDate.text = date

                val total = traffic.totalBytes.getValue()
                val upload = traffic.txBytes.getValue()
                val download = traffic.rxBytes.getValue()

                textTotalValue.text = "${total.value}"
                textTotalUnit.text = total.dataUnit.name
                textUploadValue.text = "${upload.value}"
                textUploadUnit.text = upload.dataUnit.name
                textDownloadValue.text = "${download.value}"
                textDownloadUnit.text = download.dataUnit.name

                card.setOnClickListener {
                    if (linTotal.visibility == View.GONE){
                        linTotal.visibility = View.VISIBLE
                        linDetails.visibility = View.GONE
                    }else {
                        linDetails.visibility = View.VISIBLE
                        linTotal.visibility = View.GONE
                    }
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemUsageDetailsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(traffics.first[position], traffics.second)
    }

    override fun getItemCount(): Int {
        return traffics.first.size
    }
}
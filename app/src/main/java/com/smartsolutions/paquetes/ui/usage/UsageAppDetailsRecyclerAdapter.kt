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


    inner class ViewHolder(private val binding: ItemUsageDetailsBinding): RecyclerView.ViewHolder(binding.root) {

        fun onBind(traffic: Traffic, myTimeUnit: DateCalendarUtils.MyTimeUnit) {

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

            binding.textDate.text = date

            val total = traffic.totalBytes.getValue()
            val upload = traffic.txBytes.getValue()
            val download = traffic.rxBytes.getValue()

            binding.textTotalValue.text = "${total.value}"
            binding.textTotalUnit.text = total.dataUnit.name
            binding.textUploadValue.text = "${upload.value}"
            binding.textUploadUnit.text = upload.dataUnit.name
            binding.textDownloadValue.text = "${download.value}"
            binding.textDownloadUnit.text = download.dataUnit.name

            binding.card.setOnClickListener {
                if (binding.linTotal.visibility == View.GONE){
                    binding.linTotal.visibility = View.VISIBLE
                    binding.linDetails.visibility = View.GONE
                }else {
                    binding.linDetails.visibility = View.VISIBLE
                    binding.linTotal.visibility = View.GONE
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
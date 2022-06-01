package com.example.pegasus

import android.os.Bundle
import android.provider.CallLog
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.kittinunf.fuel.httpGet
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [WifiFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WifiFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null


    private lateinit var mostOftenCallsChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_wifi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Instead of view.findViewById(R.id.hello) as TextView

        "http://asawicki.ddns.net:5000/wifi"
            .httpGet().responseObject(NavigationService.Wifi.Deserializer()) { _, _, result ->
                val (wifis, error) = result
                // adjust epoch to seconds precision
                if (wifis != null) {


                    val currentMonth = LocalDateTime.now().month.value
                    val currentYear = LocalDateTime.now().year
                    val firstDayOfMonth = LocalDateTime.of(currentYear, currentMonth, 1, 0, 0)
                    val timeArray = LongArray(6)
                    for (index in 0 until 6) {
                        timeArray[index] = firstDayOfMonth.minusMonths(index.toLong())
                            .toEpochSecond(ZoneOffset.UTC)
                        Log.d("Time", timeArray[index].toString())
                    }

                    val labels = Array<String>(6) { _ -> "" }
                    for (index in 0 until 6) {
                        labels[index] = firstDayOfMonth.minusMonths(index.toLong()).month.toString()
                    }

                    run {
                        val map = mutableMapOf<String, Int>()
                        for (wifi in wifis) {
                            if (wifi.ssid != "") {
                                when (val count = map[wifi.ssid])
                                {
                                    null -> map[wifi.ssid] = 1
                                    else -> map[wifi.ssid] = count + 1
                                }
                            }
                        }
                        val sorted = map.toList().sortedBy { (key, value) -> value }
                        val numberOfCallsEntries: ArrayList<BarEntry> = ArrayList()
                        for (index in sorted.size-6 until sorted.size) {
                            val barEntry = BarEntry((index-sorted.size+6).toFloat(), sorted[index].second.toFloat())
                            numberOfCallsEntries.add(barEntry)
                        }
                        val labelss = Array<String>(6) { _ -> "" }
                        for (index in sorted.size-6 until sorted.size) {
                            labelss[index-sorted.size+6] = sorted[index].first
                        }
                        mostOftenCallsChart = view.findViewById(R.id.mostPopular)
                        mostOftenCallsChart.description.isEnabled = false;
                        mostOftenCallsChart.xAxis.valueFormatter = IndexAxisValueFormatter(labelss)
                        mostOftenCallsChart.xAxis.labelRotationAngle = -45F
                        mostOftenCallsChart.data = BarData(BarDataSet(numberOfCallsEntries, "Most popular wifi SSID around"))
                        mostOftenCallsChart.invalidate()

                    }

                    run {
                        val map = mutableMapOf<String, Int>()
                        for (wifi in wifis) {
                            if (wifi.ssid != "") {
                                if (wifi.frequency > 4500) {
                                    when (val count = map[wifi.ssid])
                                    {
                                        null -> map[wifi.ssid] = 1
                                        else -> map[wifi.ssid] = count + 1
                                    }
                                }

                            }
                        }
                        val sorted = map.toList().sortedBy { (key, value) -> value }
                        val numberOfCallsEntries: ArrayList<BarEntry> = ArrayList()
                        for (index in sorted.size-6 until sorted.size) {
                            val barEntry = BarEntry((index-sorted.size+6).toFloat(), sorted[index].second.toFloat())
                            numberOfCallsEntries.add(barEntry)
                        }
                        val labelss = Array<String>(6) { _ -> "" }
                        for (index in sorted.size-6 until sorted.size) {
                            labelss[index-sorted.size+6] = sorted[index].first
                        }
                        mostOftenCallsChart = view.findViewById(R.id.mostPopular5GHZ)
                        mostOftenCallsChart.description.isEnabled = false;
                        mostOftenCallsChart.xAxis.valueFormatter = IndexAxisValueFormatter(labelss)
                        mostOftenCallsChart.xAxis.labelRotationAngle = -45F
                        mostOftenCallsChart.data = BarData(BarDataSet(numberOfCallsEntries, "Most popular 5GHz wifi SSID around"))
                        mostOftenCallsChart.invalidate()

                    }

                    run {
                        val map = mutableMapOf<String, Int>()
                        for (wifi in wifis) {
                            if (wifi.ssid != "") {
                                if (wifi.capabilities.contains("wpa", ignoreCase = true)
                                    || wifi.capabilities.contains("wep", ignoreCase = true)) {}
                                else {
                                    when (val count = map[wifi.ssid])
                                    {
                                        null -> map[wifi.ssid] = 1
                                        else -> map[wifi.ssid] = count + 1
                                    }
                                }

                            }
                        }
                        val sorted = map.toList().sortedBy { (key, value) -> value }
                        val numberOfCallsEntries: ArrayList<BarEntry> = ArrayList()
                        for (index in sorted.size-6 until sorted.size) {
                            val barEntry = BarEntry((index-sorted.size+6).toFloat(), sorted[index].second.toFloat())
                            numberOfCallsEntries.add(barEntry)
                        }
                        val labelss = Array<String>(6) { _ -> "" }
                        for (index in sorted.size-6 until sorted.size) {
                            labelss[index-sorted.size+6] = sorted[index].first
                        }
                        mostOftenCallsChart = view.findViewById(R.id.unprotectedPublicNetworks)
                        mostOftenCallsChart.description.isEnabled = false;
                        mostOftenCallsChart.xAxis.valueFormatter = IndexAxisValueFormatter(labelss)
                        mostOftenCallsChart.xAxis.labelRotationAngle = -45F
                        mostOftenCallsChart.data = BarData(BarDataSet(numberOfCallsEntries, "Unprotected wifi network from public places"))
                        mostOftenCallsChart.invalidate()

                    }


                }
            }

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment StatusFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            WifiFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
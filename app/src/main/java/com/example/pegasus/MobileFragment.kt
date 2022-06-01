package com.example.pegasus

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
 * Use the [MobileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MobileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private lateinit var mostOftenCallsChart: BarChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mobile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Instead of view.findViewById(R.id.hello) as TextView

        "http://asawicki.ddns.net:5000/navigation"
            .httpGet().responseObject(NavigationService.Location.Deserializer()) { _, _, result ->
                val (locations, error) = result
                // adjust epoch to seconds precision
                if (locations != null) {


                    val currentMonth = LocalDateTime.now().month.value
                    val currentYear = LocalDateTime.now().year
                    val firstDayOfMonth = LocalDateTime.of(currentYear, currentMonth, 1, 0, 0)
                    val timeArray = LongArray(6)
                    val timeArrayDay = LongArray(7)
                    for (index in 0 until 6) {
                        timeArray[index] = firstDayOfMonth.minusMonths(index.toLong())
                            .toEpochSecond(ZoneOffset.UTC)
                        Log.d("Time", timeArray[index].toString())
                    }
                    for (index in 0 until 7) {
                        timeArrayDay[index] = firstDayOfMonth.minusDays(index.toLong())
                            .toEpochSecond(ZoneOffset.UTC)
                        Log.d("Time", timeArrayDay[index].toString())
                    }

                    val labels = Array<String>(6) { _ -> "" }
                    for (index in 0 until 6) {
                        labels[index] = firstDayOfMonth.minusMonths(index.toLong()).month.toString()
                    }
                    val labelsDay = Array<String>(7) { _ -> "" }
                    for (index in 0 until 7) {
                        labelsDay[index] = firstDayOfMonth.minusDays(index.toLong()).dayOfWeek.toString()
                    }

                    run {
                        var longitude: Double = 0.0
                        var latitude: Double = 0.0
                        val durationSum = Array<Double>(6) { _ -> 0.0 }
                        for (location in locations) {
                            for (index in 0 until 6) {
                                if (location.time >= timeArray[index]) {
                                    if (longitude == 0.0) {
                                        longitude = location.longitude
                                        latitude = location.latitude
                                    } else
                                    {
                                        durationSum[index] += distance(latitude, longitude, location.latitude, location.longitude)
                                    }
                                    break
                                }
                            }
                        }
                        val timeSpentEntries: ArrayList<BarEntry> = ArrayList()
                        mostOftenCallsChart = view.findViewById(R.id.locationDistance)
                        for (i in 0 until 6) {
                            val barEntry = BarEntry(i.toFloat(), durationSum[i].toFloat())
                            timeSpentEntries.add(barEntry)
                            Log.d("Sum", durationSum[i].toString())
                        }
                        mostOftenCallsChart.description.isEnabled = false;
                        mostOftenCallsChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

                        mostOftenCallsChart.data = BarData(BarDataSet(timeSpentEntries, "Distance in meters moved in last months"))
                        mostOftenCallsChart.invalidate()

                    }
                    run {
                        var longitude: Double = 0.0
                        var latitude: Double = 0.0
                        val durationSum = Array<Double>(4) { _ -> 0.0 }
                        for (location in locations) {
                            val dt = Instant.ofEpochSecond(location.time)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime().hour
                            var i: Int = 0
                            if (dt < 6)
                                i = 0
                            else if (dt < 12)
                                i = 1
                            else if (dt < 18)
                                i = 2
                            else if (dt < 24)
                                i = 3
                            if (longitude == 0.0) {
                                longitude = location.longitude
                                latitude = location.latitude
                            } else
                            {
                                durationSum[i] += distance(latitude, longitude, location.latitude, location.longitude)
                            }
                        }
                        val timeSpentEntries: ArrayList<BarEntry> = ArrayList()
                        mostOftenCallsChart = view.findViewById(R.id.locationDistanceByDaytime)
                        for (i in 0 until 4) {
                            val barEntry = BarEntry(i.toFloat(), durationSum[i].toFloat())
                            timeSpentEntries.add(barEntry)
                            Log.d("Sum", durationSum[i].toString())
                        }
                        val types = arrayOf("Night", "Morning", "Afternoon", "Evening")
                        mostOftenCallsChart.description.isEnabled = false;
                        mostOftenCallsChart.xAxis.valueFormatter = IndexAxisValueFormatter(types)
                        mostOftenCallsChart.xAxis.labelCount = 4

                        mostOftenCallsChart.data = BarData(BarDataSet(timeSpentEntries, "Distance in meters moved day time"))
                        mostOftenCallsChart.invalidate()

                    }
                    run {
                        var longitude: Double = 0.0
                        var latitude: Double = 0.0
                        val durationSum = Array<Double>(7) { _ -> 0.0 }
                        for (location in locations) {
                            for (index in 0 until 7) {
                                if (location.time >= timeArray[index]) {
                                    if (longitude == 0.0) {
                                        longitude = location.longitude
                                        latitude = location.latitude
                                    } else
                                    {
                                        durationSum[index] += distance(latitude, longitude, location.latitude, location.longitude) / 0.8
                                    }
                                    break
                                }
                            }
                        }
                        val timeSpentEntries: ArrayList<BarEntry> = ArrayList()
                        mostOftenCallsChart = view.findViewById(R.id.distanceDays)
                        for (i in 0 until 7) {
                            val barEntry = BarEntry(i.toFloat(), durationSum[i].toFloat())
                            timeSpentEntries.add(barEntry)
                            Log.d("Sum", durationSum[i].toString())
                        }
                        mostOftenCallsChart.description.isEnabled = false;
                        mostOftenCallsChart.xAxis.valueFormatter = IndexAxisValueFormatter(labelsDay)
                        mostOftenCallsChart.xAxis.labelRotationAngle = -45F

                        mostOftenCallsChart.data = BarData(BarDataSet(timeSpentEntries, "Steps in this week"))
                        mostOftenCallsChart.invalidate()

                    }




                }
            }

    }

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val theta = lon1 - lon2
        var dist = (Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + (Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta))))
        dist = Math.acos(dist)
        dist = rad2deg(dist)
        dist = dist * 60 * 1.1515
        return dist
    }

    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MessageFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MobileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
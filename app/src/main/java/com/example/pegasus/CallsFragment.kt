package com.example.pegasus

import android.os.Bundle
import android.provider.CallLog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.kittinunf.fuel.httpGet
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
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
 * Use the [CallsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CallsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null


    private lateinit var timeSpent: BarChart
    private lateinit var numberOfCallsChart: BarChart
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
        return inflater.inflate(R.layout.fragment_calls, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Instead of view.findViewById(R.id.hello) as TextView

        "http://asawicki.ddns.net:5000/call"
            .httpGet().responseObject(NavigationService.Call.Deserializer()) { _, _, result ->
                val (calls, error) = result
                // adjust epoch to seconds precision
                if (calls != null) {
                    for (i in 0 until calls.size) {
                        calls[i].time = calls[i].time / 1000
                    }


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
                        val durationSum = Array<Double>(6) { _ -> 0.0 }
                        for (call in calls) {
                            for (index in 0 until 6) {
                                if (call.time >= timeArray[index]) {
                                    durationSum[index] += call.duration.toDouble() / 60
                                    break
                                }
                            }
                        }
                        val timeSpentEntries: ArrayList<BarEntry> = ArrayList()
                        timeSpent = view.findViewById(R.id.timeSpent)
                        for (i in 0 until 6) {
                            val barEntry = BarEntry(i.toFloat(), durationSum[i].toFloat())
                            timeSpentEntries.add(barEntry)
                        }
                        timeSpent.description.isEnabled = false;
                        timeSpent.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

                        timeSpent.data = BarData(BarDataSet(timeSpentEntries, "Time spent talking on mobile phone in minutes"))
                        timeSpent.invalidate()
                    }
                    run {
                        val numberOfCalls = Array<Double>(6) { _ -> 0.0 }
                        for (call in calls) {
                            for (index in 0 until 6) {
                                if (call.time >= timeArray[index]) {
                                    numberOfCalls[index] += 1.0
                                    break
                                }
                            }
                        }
                        val numberOfCallsEntries: ArrayList<BarEntry> = ArrayList()
                        numberOfCallsChart = view.findViewById(R.id.numberOfCalls);
                        for (i in 0 until 6) {
                            val barEntry = BarEntry(i.toFloat(), numberOfCalls[i].toFloat())
                            numberOfCallsEntries.add(barEntry)
                        }
                        numberOfCallsChart.description.isEnabled = false;
                        numberOfCallsChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                        numberOfCallsChart.data = BarData(BarDataSet(numberOfCallsEntries, "Number of phone calls in particular month"))
                        numberOfCallsChart.invalidate()
                    }
                    run {
                        val map = mutableMapOf<String, Int>()
                        for (call in calls) {
                            when (val count = map[call.number])
                            {
                                null -> map[call.number] = 1
                                else -> map[call.number] = count + 1
                            }
                        }
                        Log.d("Sort", "dupa")
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
                        mostOftenCallsChart = view.findViewById(R.id.mostOftenNumbers)
                        mostOftenCallsChart.description.isEnabled = false;
                        mostOftenCallsChart.xAxis.valueFormatter = IndexAxisValueFormatter(labelss)
                        mostOftenCallsChart.xAxis.labelRotationAngle = -45F
                        mostOftenCallsChart.data = BarData(BarDataSet(numberOfCallsEntries, "Most often phone calls destinations"))
                        mostOftenCallsChart.invalidate()

                    }
                    run {
                        val map = mutableMapOf<String, Int>()
                        for (call in calls) {
                            when (val count = map[call.number])
                            {
                                null -> map[call.number] = call.duration / 60
                                else -> map[call.number] = count + call.duration / 60
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
                        mostOftenCallsChart = view.findViewById(R.id.mostOftenNumbersMinutes)
                        mostOftenCallsChart.description.isEnabled = false;
                        mostOftenCallsChart.xAxis.valueFormatter = IndexAxisValueFormatter(labelss)
                        mostOftenCallsChart.xAxis.labelRotationAngle = -45F
                        mostOftenCallsChart.data = BarData(BarDataSet(numberOfCallsEntries, "Minutes talked on the phone with particular numbers"))
                        mostOftenCallsChart.invalidate()

                    }
                    run {
                        val durationSum = Array<Double>(6) { _ -> 0.0 }
                        for (call in calls) {
                            if (call.type == CallLog.Calls.INCOMING_TYPE)
                                durationSum[0] += call.duration.toDouble() / 60
                            else if (call.type == CallLog.Calls.OUTGOING_TYPE)
                                durationSum[1] += call.duration.toDouble() / 60
                            else if (call.type == CallLog.Calls.MISSED_TYPE)
                                durationSum[2] += call.duration.toDouble() / 60
                            else if (call.type == CallLog.Calls.VOICEMAIL_TYPE)
                                durationSum[3] += call.duration.toDouble() / 60
                            else if (call.type == CallLog.Calls.REJECTED_TYPE)
                                durationSum[4] += call.duration.toDouble() / 60
                            else if (call.type == CallLog.Calls.BLOCKED_TYPE)
                                durationSum[5] += call.duration.toDouble() / 60
                        }
                        val timeSpentEntries: ArrayList<BarEntry> = ArrayList()
                        timeSpent = view.findViewById(R.id.differenciateTypeCalls)
                        for (i in 0 until 6) {
                            val barEntry = BarEntry(i.toFloat(), durationSum[i].toFloat())
                            timeSpentEntries.add(barEntry)
                        }
                        val types = arrayOf("Incoming", "Outgoing", "Missed", "Voicemail", "Rejected", "Blocked")
                        val labelss = Array<String>(6) { _ -> "" }
                        for (index in 0 until 6) {
                            labelss[index] = types[index]
                        }
                        timeSpent.description.isEnabled = false;
                        timeSpent.xAxis.valueFormatter = IndexAxisValueFormatter(labelss)

                        timeSpent.data = BarData(BarDataSet(timeSpentEntries, "Minutes of phone calls by call types"))
                        timeSpent.invalidate()

                    }
                    run {
                        val durationSum = Array<Double>(6) { _ -> 0.0 }
                        for (call in calls) {
                            if (call.type == CallLog.Calls.INCOMING_TYPE)
                                durationSum[0] += 1.0
                            else if (call.type == CallLog.Calls.OUTGOING_TYPE)
                                durationSum[1] += 1.0
                            else if (call.type == CallLog.Calls.MISSED_TYPE)
                                durationSum[2] += 1.0
                            else if (call.type == CallLog.Calls.VOICEMAIL_TYPE)
                                durationSum[3] += 1.0
                            else if (call.type == CallLog.Calls.REJECTED_TYPE)
                                durationSum[4] += 1.0
                            else if (call.type == CallLog.Calls.BLOCKED_TYPE)
                                durationSum[5] += 1.0
                        }
                        val timeSpentEntries: ArrayList<BarEntry> = ArrayList()
                        timeSpent = view.findViewById(R.id.differenciateTypeCallsPerCall)
                        for (i in 0 until 6) {
                            val barEntry = BarEntry(i.toFloat(), durationSum[i].toFloat())
                            timeSpentEntries.add(barEntry)
                        }
                        val types = arrayOf("Incoming", "Outgoing", "Missed", "Voicemail", "Rejected", "Blocked")
                        val labelss = Array<String>(6) { _ -> "" }
                        for (index in 0 until 6) {
                            labelss[index] = types[index]
                        }
                        timeSpent.description.isEnabled = false;
                        timeSpent.xAxis.valueFormatter = IndexAxisValueFormatter(labelss)

                        timeSpent.data = BarData(BarDataSet(timeSpentEntries, "Calls of phone by call types"))
                        timeSpent.invalidate()

                    }
                    run {
                        val durationSum = Array<Double>(4) { _ -> 0.0 }
                        for (call in calls) {
                            val dt = Instant.ofEpochSecond(call.time)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime().hour
                            if (dt < 6)
                                durationSum[0] += 1.0
                            else if (dt < 12)
                                durationSum[1] += 1.0
                            else if (dt < 18)
                                durationSum[2] += 1.0
                            else if (dt < 24)
                                durationSum[3] += 1.0
                        }
                        val timeSpentEntries: ArrayList<BarEntry> = ArrayList()
                        timeSpent = view.findViewById(R.id.numberOfCallsByDayTime)
                        for (i in 0 until 4) {
                            val barEntry = BarEntry(i.toFloat(), durationSum[i].toFloat())
                            timeSpentEntries.add(barEntry)
                        }
                        val types = arrayOf("Night", "Morning", "Afternoon", "Evening")
                        val labelss = Array<String>(4) { _ -> "" }
                        for (index in 0 until 4) {
                            labelss[index] = types[index]
                        }
                        timeSpent.description.isEnabled = false;
                        timeSpent.xAxis.valueFormatter = IndexAxisValueFormatter(types)
                        timeSpent.xAxis.labelCount = 4

                        timeSpent.data = BarData(BarDataSet(timeSpentEntries, "Number of phone calls by time of day"))
                        timeSpent.invalidate()

                    }
                    run {
                        val durationSum = Array<Double>(4) { _ -> 0.0 }
                        for (call in calls) {
                            val dt = Instant.ofEpochSecond(call.time)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime().hour
                            if (dt < 6)
                                durationSum[0] += call.duration.toDouble() / 60
                            else if (dt < 12)
                                durationSum[1] += call.duration.toDouble() / 60
                            else if (dt < 18)
                                durationSum[2] += call.duration.toDouble() / 60
                            else if (dt < 24)
                                durationSum[3] += call.duration.toDouble() / 60
                        }
                        val timeSpentEntries: ArrayList<BarEntry> = ArrayList()
                        timeSpent = view.findViewById(R.id.numberOfMinutesByDayTime)
                        for (i in 0 until 4) {
                            val barEntry = BarEntry(i.toFloat(), durationSum[i].toFloat())
                            timeSpentEntries.add(barEntry)
                        }
                        val types = arrayOf("Night", "Morning", "Afternoon", "Evening")
                        val labelss = Array<String>(4) { _ -> "" }
                        for (index in 0 until 4) {
                            labelss[index] = types[index]
                        }
                        timeSpent.description.isEnabled = false;
                        timeSpent.xAxis.valueFormatter = IndexAxisValueFormatter(types)
                        timeSpent.xAxis.labelCount = 4

                        timeSpent.data = BarData(BarDataSet(timeSpentEntries, "Mintes of phone calls by time of day"))
                        timeSpent.invalidate()

                    }
                    run {
                        val map = mutableMapOf<String, Int>()
                        for (call in calls) {
                            val dt = Instant.ofEpochSecond(call.time)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime().hour
                            if (dt>18) {
                                when (val count = map[call.number])
                                {
                                    null -> map[call.number] = call.duration / 60
                                    else -> map[call.number] = count + call.duration / 60
                                }
                            }
                        }
                        val sorted = map.toList().sortedBy { (key, value) -> value }
                        val numberOfCallsEntries: ArrayList<BarEntry> = ArrayList()
                        for (index in sorted.size-4 until sorted.size) {
                            val barEntry = BarEntry((index-sorted.size+4).toFloat(), sorted[index].second.toFloat())
                            numberOfCallsEntries.add(barEntry)
                        }
                        val labelss = Array<String>(4) { _ -> "" }
                        for (index in sorted.size-4 until sorted.size) {
                            labelss[index-sorted.size+4] = sorted[index].first
                        }
                        mostOftenCallsChart = view.findViewById(R.id.numberOfMinutesByEveningTime)
                        mostOftenCallsChart.description.isEnabled = false;
                        mostOftenCallsChart.xAxis.valueFormatter = IndexAxisValueFormatter(labelss)
                        mostOftenCallsChart.xAxis.labelRotationAngle = -45F
                        mostOftenCallsChart.xAxis.labelCount = 4
                        mostOftenCallsChart.data = BarData(BarDataSet(numberOfCallsEntries, "Minutes talked on the phone with particular numbers in evening time"))
                        mostOftenCallsChart.invalidate()

                    }
                    run {
                        val map = mutableMapOf<String, Int>()
                        for (call in calls) {
                            val dt = Instant.ofEpochSecond(call.time)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime().hour
                            if (dt>18) {
                                when (val count = map[call.number])
                                {
                                    null -> map[call.number] = 1
                                    else -> map[call.number] = count + 1
                                }
                            }
                        }
                        val sorted = map.toList().sortedBy { (key, value) -> value }
                        val numberOfCallsEntries: ArrayList<BarEntry> = ArrayList()
                        for (index in sorted.size-4 until sorted.size) {
                            val barEntry = BarEntry((index-sorted.size+4).toFloat(), sorted[index].second.toFloat())
                            numberOfCallsEntries.add(barEntry)
                        }
                        val labelss = Array<String>(4) { _ -> "" }
                        for (index in sorted.size-4 until sorted.size) {
                            labelss[index-sorted.size+4] = sorted[index].first
                        }
                        mostOftenCallsChart = view.findViewById(R.id.numberOfCallsByEveningTime)
                        mostOftenCallsChart.description.isEnabled = false;
                        mostOftenCallsChart.xAxis.valueFormatter = IndexAxisValueFormatter(labelss)
                        mostOftenCallsChart.xAxis.labelRotationAngle = -45F
                        mostOftenCallsChart.xAxis.labelCount = 4
                        mostOftenCallsChart.data = BarData(BarDataSet(numberOfCallsEntries, "Minutes talked on the phone with particular numbers in evening time"))
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
         * @return A new instance of fragment CallFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CallsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
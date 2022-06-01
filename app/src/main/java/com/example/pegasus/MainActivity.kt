package com.example.pegasus

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class MainActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val actionBar: ActionBar? = supportActionBar
        actionBar?.hide()

        if (!isServiceRunningInForeground(this, NavigationService::class.java)) {

            val intent = Intent(this, NavigationService::class.java)
            startForegroundService(intent)
        }

        val tabTitle = arrayOf(getString(R.string.tab_name_1), getString(R.string.tab_name_2), getString(R.string.tab_name_3))

        val pager = findViewById<ViewPager2>(R.id.viewPager2)
        val t1 = findViewById<TabLayout>(R.id.tabLayout)
        pager.adapter = FragmentAdapter(supportFragmentManager, lifecycle)

        TabLayoutMediator(t1, pager) {
            tab, position ->
                tab.text = tabTitle[position]
        }.attach()


    }



    private fun isServiceRunningInForeground(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }
        return false
    }

}
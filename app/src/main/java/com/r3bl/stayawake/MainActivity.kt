/*
 * Copyright 2020 R3BL LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.r3bl.stayawake

import android.util.Log.d
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.util.Linkify
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.util.LinkifyCompat
import com.r3bl.stayawake.MyTileService.Companion.TAG
import com.r3bl.stayawake.MyTileService.Companion.coldStart
import com.r3bl.stayawake.MyTileServiceSettings.loadSharedPreferences
import com.r3bl.stayawake.MyTileServiceSettings.saveSharedPreferencesAfterRunningLambda
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.spinner_timeout
import kotlinx.android.synthetic.main.activity_main.view.*
import java.util.concurrent.TimeUnit

/** More info: https://stackoverflow.com/a/25510848/2085356 */
private class MySpinnerAdapter(context: Context, resource: Int, items: List<String>, private val font: Typeface) :
  ArrayAdapter<String>(context, resource, items) {
  // Affects default (closed) state of the spinner.
  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
      (super.getView(position, convertView, parent) as TextView).apply { typeface = font }

  // Affects opened state of the spinner.
  override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
      (super.getDropDownView(position, convertView, parent) as TextView).apply { typeface = font }
}


class MainActivity : AppCompatActivity() {
  private lateinit var typeNotoSansRegular: Typeface
  private lateinit var typeNotoSansBold: Typeface
  private lateinit var typeTitilumWebLight: Typeface
  private lateinit var typeTitilumWebRegular: Typeface

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    //showAppIconInActionBar();
    //hideStatusBar();
    loadAndApplyFonts()
    formatMessages()
    if (loadSharedPreferences(this).autoStartEnabled) coldStart(this, false)
    loadSharedPreferences(this)
    setupCheckbox()
    setupSpinner(typeNotoSansRegular)
  }

  private fun setupCheckbox() {
    loadSharedPreferences(this).autoStartEnabled.let { autoStartEnabled ->
      checkbox_prefs_auto_start.isChecked = autoStartEnabled
      d(TAG, "setupCheckbox: set checkbox state to: $autoStartEnabled")
    }
  }

  private fun loadAndApplyFonts() {
    typeNotoSansRegular = Typeface.createFromAsset(assets, "fonts/notosans_regular.ttf")
    typeNotoSansBold = Typeface.createFromAsset(assets, "fonts/notosans_bold.ttf")
    typeTitilumWebLight = Typeface.createFromAsset(assets, "fonts/titilliumweb_light.ttf")
    typeTitilumWebRegular = Typeface.createFromAsset(assets, "fonts/titilliumweb_regular.ttf")

    listOf<TextView>(text_app_title).forEach { it.typeface = typeNotoSansBold }

    listOf<TextView>(text_marketing_message).forEach { it.typeface = typeTitilumWebLight }

    listOf<TextView>(text_introduction_heading, text_installation_heading, text_opensource_title).forEach {
      it.typeface = typeTitilumWebRegular
    }

    listOf<TextView>(text_spinner_timeout_description,
                     button_start_awake,
                     checkbox_prefs_auto_start,
                     text_introduction_content,
                     text_install_body,
                     text_install_body_1,
                     text_install_body_2,
                     text_install_body_3,
                     text_opensource_body).forEach {
      it.typeface = typeNotoSansRegular
    }
  }

  private fun setupSpinner(font: Typeface) = with(spinner_timeout) {
    // Create custom adatper to handle font.
    val myAdapter = MySpinnerAdapter(this@MainActivity,
                                     android.R.layout.simple_spinner_item,
                                     resources.getStringArray(R.array.spinner_timeout_choices).toList(),
                                     font)
    adapter = myAdapter

    // Restore saved selection.
    val savedTimeoutInSec: Long = loadSharedPreferences(this@MainActivity).timeoutNotChargingSec
    val savedTimeoutInMin: Long = TimeUnit.MINUTES.convert(savedTimeoutInSec, TimeUnit.SECONDS)
    val position = myAdapter.getPosition(savedTimeoutInMin.toString())
    if (position != -1) {
      spinner_timeout.setSelection(position)
      formatMessages()
      d(TAG, "setupSpinner: set spinner selection to position: $position")
    }

    // Attach listeners to handle user selection.
    spinner_timeout.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onNothingSelected(parent: AdapterView<*>?) {}

      override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val selectionInMin: String = parent?.getItemAtPosition(position).toString()
        val selectionInSec: Long = TimeUnit.SECONDS.convert(selectionInMin.toLong(), TimeUnit.MINUTES)
        saveSharedPreferencesAfterRunningLambda(this@MainActivity) {
          timeoutNotChargingSec = selectionInSec.toLong()
        }
        formatMessages()
        d(TAG, "onItemSelected: spinner selection is $selectionInMin")
      }
    }
  }

  private fun hideStatusBar() {
    val decorView = window.decorView
    // Hide the status bar.
    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
    // Remember that you should never show the action bar if the status bar is hidden, so hide that too if necessary.
    actionBar?.hide()
  }

  private fun showAppIconInActionBar() {
    supportActionBar?.apply {
      setDisplayShowHomeEnabled(true)
      setLogo(R.mipmap.ic_launcher)
      setDisplayUseLogoEnabled(true)
    }
  }

  private fun formatMessages() {
    // Add actual minutes to string template.
    val hours = TimeUnit.SECONDS.toMinutes(loadSharedPreferences(this).timeoutNotChargingSec)
    text_introduction_content.text = getString(R.string.introduction_body, hours)

    // Linkify github link.
    LinkifyCompat.addLinks(text_opensource_body, Linkify.WEB_URLS)

    // Spanning color on textviews.
    applySpan(text_install_body_1, "Step 1")
    applySpan(text_install_body_2, "Step 2")
    applySpan(text_install_body_3, "Step 3")
  }

  private fun applySpan(textView: TextView, substring: String) {
    setColorSpanOnTextView(textView,
                           getString(R.string.install_body_1, substring),
                           substring,
                           getColor(R.color.colorTextDark))
  }

  private fun setColorSpanOnTextView(view: TextView,
                                     fulltext: String,
                                     subtext: String,
                                     color: Int
  ) {
    view.setText(fulltext, TextView.BufferType.SPANNABLE)
    val str = view.text as Spannable
    val i = fulltext.indexOf(subtext)
    str.setSpan(ForegroundColorSpan(color), i, i + subtext.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
  }

  fun buttonStartAwakeClicked(ignore: View) = coldStart(this, true)

  fun checkboxClicked(view: View) = (view as CheckBox).let { checkbox ->
    saveSharedPreferencesAfterRunningLambda(this) { autoStartEnabled = checkbox.isChecked }
  }
}
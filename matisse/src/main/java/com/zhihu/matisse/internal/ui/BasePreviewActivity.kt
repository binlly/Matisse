/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.entity.IncapableCause
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.model.SelectedItemCollection
import com.zhihu.matisse.internal.ui.adapter.PreviewPagerAdapter
import com.zhihu.matisse.internal.ui.widget.CheckView
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils
import com.zhihu.matisse.internal.utils.Platform
import kotlinx.android.synthetic.main.activity_media_preview.*

abstract class BasePreviewActivity: AppCompatActivity(), View.OnClickListener, ViewPager.OnPageChangeListener {

    protected val mSelectedCollection = SelectedItemCollection(this)
    protected lateinit var mSpec: SelectionSpec

    protected lateinit var mAdapter: PreviewPagerAdapter

    protected var mPreviousPos = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(SelectionSpec.getInstance().themeId)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_preview)
        if (Platform.hasKitKat()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        mSpec = SelectionSpec.getInstance()
        if (mSpec.needOrientationRestriction()) {
            requestedOrientation = mSpec.orientation
        }

        if (savedInstanceState == null) {
            mSelectedCollection.onCreate(intent.getBundleExtra(EXTRA_DEFAULT_BUNDLE))
        } else {
            mSelectedCollection.onCreate(savedInstanceState)
        }

        button_back.setOnClickListener(this)
        button_apply.setOnClickListener(this)

        pager.addOnPageChangeListener(this)
        mAdapter = PreviewPagerAdapter(supportFragmentManager, null)
        pager.adapter = mAdapter
        check_view.setCountable(mSpec.countable)

        check_view.setOnClickListener {
            val item = mAdapter.getMediaItem(pager.currentItem)
            if (mSelectedCollection.isSelected(item)) {
                mSelectedCollection.remove(item)
                if (mSpec.countable) {
                    check_view.setCheckedNum(CheckView.UNCHECKED)
                } else {
                    check_view.setChecked(false)
                }
            } else {
                if (assertAddSelection(item)) {
                    mSelectedCollection.add(item)
                    if (mSpec.countable) {
                        check_view.setCheckedNum(mSelectedCollection.checkedNumOf(item))
                    } else {
                        check_view.setChecked(true)
                    }
                }
            }
            updateApplyButton()
        }
        updateApplyButton()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mSelectedCollection.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        sendBackResult(false)
        super.onBackPressed()
    }

    override fun onClick(v: View) {
        if (v.id == R.id.button_back) {
            onBackPressed()
        } else if (v.id == R.id.button_apply) {
            sendBackResult(true)
            finish()
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        val adapter = pager.adapter as PreviewPagerAdapter
        if (mPreviousPos != -1 && mPreviousPos != position) {
            (adapter.instantiateItem(pager, mPreviousPos) as PreviewItemFragment).resetView()

            val item = adapter.getMediaItem(position)
            if (mSpec.countable) {
                val checkedNum = mSelectedCollection.checkedNumOf(item)
                check_view.setCheckedNum(checkedNum)
                if (checkedNum > 0) {
                    check_view.isEnabled = true
                } else {
                    check_view.isEnabled = !mSelectedCollection.maxSelectableReached()
                }
            } else {
                val checked = mSelectedCollection.isSelected(item)
                check_view.setChecked(checked)
                if (checked) {
                    check_view.isEnabled = true
                } else {
                    check_view.isEnabled = !mSelectedCollection.maxSelectableReached()
                }
            }
            updateSize(item)
        }
        mPreviousPos = position
    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    private fun updateApplyButton() {
        val selectedCount = mSelectedCollection.count()
        if (selectedCount == 0) {
            button_apply.setText(R.string.button_apply_default)
            button_apply.isEnabled = false
        } else if (selectedCount == 1 && mSpec.singleSelectionModeEnabled()) {
            button_apply.setText(R.string.button_apply_default)
            button_apply.isEnabled = true
        } else {
            button_apply.isEnabled = true
            button_apply.text = getString(R.string.button_apply, selectedCount)
        }
    }

    protected fun updateSize(item: Item) {
        if (item.isGif) {
            size.visibility = View.VISIBLE
            size.text = PhotoMetadataUtils.getSizeInMB(item.size).toString() + "M"
        } else {
            size.visibility = View.GONE
        }
    }

    protected fun sendBackResult(apply: Boolean) {
        val intent = Intent()
        intent.putExtra(EXTRA_RESULT_BUNDLE, mSelectedCollection.dataWithBundle)
        intent.putExtra(EXTRA_RESULT_APPLY, apply)
        setResult(Activity.RESULT_OK, intent)
    }

    private fun assertAddSelection(item: Item): Boolean {
        val cause = mSelectedCollection.isAcceptable(item)
        IncapableCause.handleCause(this, cause)
        return cause == null
    }

    companion object {
        const val EXTRA_DEFAULT_BUNDLE = "extra_default_bundle"
        const val EXTRA_RESULT_BUNDLE = "extra_result_bundle"
        const val EXTRA_RESULT_APPLY = "extra_result_apply"
    }
}

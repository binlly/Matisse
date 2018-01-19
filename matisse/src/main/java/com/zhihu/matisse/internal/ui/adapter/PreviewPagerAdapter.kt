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
package com.zhihu.matisse.internal.ui.adapter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.ViewGroup
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.ui.PreviewItemFragment
import java.util.*

class PreviewPagerAdapter(manager: FragmentManager, private val onPrimaryItemSet: ((Int) -> Unit)?):
        FragmentPagerAdapter(manager) {

    private val mItems = ArrayList<Item>()

    override fun getItem(position: Int): Fragment {
        return PreviewItemFragment.newInstance(mItems[position])
    }

    override fun getCount(): Int {
        return mItems.size
    }

    override fun setPrimaryItem(container: ViewGroup?, position: Int, obj: Any?) {
        super.setPrimaryItem(container, position, obj)
        onPrimaryItemSet?.invoke(position)
    }

    fun getMediaItem(position: Int): Item {
        return mItems[position]
    }

    fun addAll(items: List<Item>) {
        mItems.addAll(items)
    }
}

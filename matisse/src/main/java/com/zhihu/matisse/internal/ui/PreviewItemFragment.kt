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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase
import kotlinx.android.synthetic.main.fragment_preview_item.view.*
import org.jetbrains.anko.toast

class PreviewItemFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_preview_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val item = arguments.getParcelable<Item>(ARGS_ITEM) ?: return

        if (item.isVideo) {
            view.video_play_button.visibility = View.VISIBLE
            view.video_play_button.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(item.uri, "video/*")
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    context.toast(R.string.error_no_video_activity)
                }
            }
        } else {
            view.video_play_button.visibility = View.GONE
        }

        view.image_view.displayType = ImageViewTouchBase.DisplayType.FIT_TO_SCREEN

        val size = PhotoMetadataUtils.getBitmapSize(item.contentUri, activity)
        if (item.isGif) {
            SelectionSpec.getInstance().imageEngine.loadGifImage(context, size.x, size.y, view.image_view,
                    item.contentUri)
        } else {
            SelectionSpec.getInstance().imageEngine.loadImage(context, size.x, size.y, view.image_view, item.contentUri)
        }
    }

    fun resetView() {
        view?.image_view?.resetMatrix()
    }

    companion object {

        private const val ARGS_ITEM = "args_item"

        fun newInstance(item: Item): PreviewItemFragment {
            val fragment = PreviewItemFragment()
            val bundle = Bundle()
            bundle.putParcelable(ARGS_ITEM, item)
            fragment.arguments = bundle
            return fragment
        }
    }
}

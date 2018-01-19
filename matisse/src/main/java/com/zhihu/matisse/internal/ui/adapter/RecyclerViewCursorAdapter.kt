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

import android.database.Cursor
import android.provider.MediaStore
import android.support.v7.widget.RecyclerView

abstract class RecyclerViewCursorAdapter<VH: RecyclerView.ViewHolder> internal constructor(c: Cursor?):
        RecyclerView.Adapter<VH>() {

    var cursor: Cursor? = null
        private set
    private var mRowIDColumn: Int = 0

    init {
        setHasStableIds(true)
        swapCursor(c)
    }

    protected abstract fun onBindViewHolder(holder: VH, cursor: Cursor)

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = getValidCursor(cursor, position)
        if (!c.moveToPosition(position)) {
            throw IllegalStateException("Could not move cursor to position $position when trying to bind view holder")
        }

        onBindViewHolder(holder, c)
    }

    override fun getItemViewType(position: Int): Int {
        val c = getValidCursor(cursor, position)
        if (!c.moveToPosition(position)) {
            throw IllegalStateException(
                    "Could not move cursor to position $position when trying to get item view type.")
        }
        return getItemViewType(position, c)
    }

    protected abstract fun getItemViewType(position: Int, cursor: Cursor): Int

    override fun getItemCount(): Int {
        return cursor?.count ?: 0
    }

    override fun getItemId(position: Int): Long {
        val c = getValidCursor(cursor, position)
        if (!c.moveToPosition(position)) {
            throw IllegalStateException("Could not move cursor to position $position when trying to get an item id")
        }

        return c.getLong(mRowIDColumn)
    }

    fun swapCursor(newCursor: Cursor?) {
        if (newCursor === cursor) {
            return
        }

        if (newCursor != null) {
            cursor = newCursor
            mRowIDColumn = cursor!!.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            // notify the observers about the new cursor
            notifyDataSetChanged()
        } else {
            notifyItemRangeRemoved(0, itemCount)
            cursor = null
            mRowIDColumn = -1
        }
    }

    private fun getValidCursor(cursor: Cursor?, pos: Int): Cursor {
        val e = IllegalStateException("Cannot lookup item id when cursor is in invalid state.")
        val c = cursor ?: throw e
        if (c.isClosed) throw e
        if (!c.moveToPosition(pos)) {
            throw IllegalStateException("Could not move cursor to position $pos when trying to get an item id")
        }
        return c
    }
}

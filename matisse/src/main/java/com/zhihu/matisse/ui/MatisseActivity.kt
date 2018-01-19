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
package com.zhihu.matisse.ui

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.TextView
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.entity.Album
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.model.AlbumCollection
import com.zhihu.matisse.internal.model.SelectedItemCollection
import com.zhihu.matisse.internal.ui.AlbumPreviewActivity
import com.zhihu.matisse.internal.ui.BasePreviewActivity
import com.zhihu.matisse.internal.ui.MediaSelectionFragment
import com.zhihu.matisse.internal.ui.SelectedPreviewActivity
import com.zhihu.matisse.internal.ui.adapter.AlbumMediaAdapter
import com.zhihu.matisse.internal.ui.adapter.AlbumsAdapter
import com.zhihu.matisse.internal.ui.widget.AlbumsSpinner
import com.zhihu.matisse.internal.utils.MediaStoreCompat
import com.zhihu.matisse.internal.utils.PathUtils
import kotlinx.android.synthetic.main.activity_matisse.*
import java.util.*

/**
 * Main Activity to display albums and media content (images/videos) in each album
 * and also support media selecting operations.
 */
class MatisseActivity: AppCompatActivity(), AlbumCollection.AlbumCallbacks, AdapterView.OnItemSelectedListener,
                       MediaSelectionFragment.SelectionProvider, View.OnClickListener,
                       AlbumMediaAdapter.CheckStateListener, AlbumMediaAdapter.OnMediaClickListener,
                       AlbumMediaAdapter.OnPhotoCapture {

    private val mAlbumCollection = AlbumCollection()
    private var mMediaStoreCompat: MediaStoreCompat? = null
    private val mSelectedCollection = SelectedItemCollection(this)
    private val mSpec: SelectionSpec = SelectionSpec.getInstance()

    private lateinit var mAlbumsSpinner: AlbumsSpinner
    private lateinit var mAlbumsAdapter: AlbumsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        // programmatically set theme before super.onCreate()
        setTheme(mSpec.themeId)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_matisse)

        if (mSpec.needOrientationRestriction()) {
            requestedOrientation = mSpec.orientation
        }

        if (mSpec.capture) {
            mMediaStoreCompat = MediaStoreCompat(this)
            if (mSpec.captureStrategy == null) throw RuntimeException("Don't forget to set CaptureStrategy.")
            mMediaStoreCompat?.setCaptureStrategy(mSpec.captureStrategy)
        }

        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.let {
            actionBar.setDisplayShowTitleEnabled(false)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
        val navigationIcon = toolbar.navigationIcon
        val ta = theme.obtainStyledAttributes(intArrayOf(R.attr.album_element_color))
        val color = ta.getColor(0, 0)
        ta.recycle()
        navigationIcon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)

        button_preview.setOnClickListener(this)
        button_apply.setOnClickListener(this)

        mSelectedCollection.onCreate(savedInstanceState)
        updateBottomToolbar()

        mAlbumsAdapter = AlbumsAdapter(this, null, false)
        mAlbumsSpinner = AlbumsSpinner(this)
        mAlbumsSpinner.setOnItemSelectedListener(this)
        mAlbumsSpinner.setSelectedTextView(findViewById<View>(R.id.selected_album) as TextView)
        mAlbumsSpinner.setPopupAnchorView(findViewById(R.id.toolbar))
        mAlbumsSpinner.setAdapter(mAlbumsAdapter)
        mAlbumCollection.onCreate(this, this)
        mAlbumCollection.onRestoreInstanceState(savedInstanceState)
        mAlbumCollection.loadAlbums()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mSelectedCollection.onSaveInstanceState(outState)
        mAlbumCollection.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mAlbumCollection.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        if (requestCode == REQUEST_CODE_PREVIEW) {
            val resultBundle = data.getBundleExtra(BasePreviewActivity.EXTRA_RESULT_BUNDLE)
            val selected = resultBundle.getParcelableArrayList<Item>(SelectedItemCollection.STATE_SELECTION)
            val collectionType = resultBundle.getInt(SelectedItemCollection.STATE_COLLECTION_TYPE,
                    SelectedItemCollection.COLLECTION_UNDEFINED)
            if (data.getBooleanExtra(BasePreviewActivity.EXTRA_RESULT_APPLY, false)) {
                val result = Intent()
                val selectedUris = ArrayList<Uri>()
                val selectedPaths = ArrayList<String>()
                if (selected != null) {
                    for (item in selected) {
                        selectedUris.add(item.contentUri)
                        selectedPaths.add(PathUtils.getPath(this, item.contentUri))
                    }
                }
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris)
                result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths)
                setResult(Activity.RESULT_OK, result)
                finish()
            } else {
                mSelectedCollection.overwrite(selected, collectionType)
                val mediaSelectionFragment = supportFragmentManager.findFragmentByTag(
                        MediaSelectionFragment::class.java.simpleName)
                (mediaSelectionFragment as? MediaSelectionFragment)?.refreshMediaGrid()
                updateBottomToolbar()
            }
        } else if (requestCode == REQUEST_CODE_CAPTURE) {
            // Just pass the data back to previous calling Activity.
            val contentUri = mMediaStoreCompat?.currentPhotoUri
            val path = mMediaStoreCompat?.currentPhotoPath
            val result = Intent()
            contentUri?.let {
                val selected = ArrayList<Uri>()
                selected.add(contentUri)
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selected)
            }
            path?.let {
                val selectedPath = ArrayList<String>()
                selectedPath.add(path)
                result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPath)
            }
            setResult(Activity.RESULT_OK, result)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) this@MatisseActivity.revokeUriPermission(
                    contentUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            finish()
        }
    }

    private fun updateBottomToolbar() {
        val selectedCount = mSelectedCollection.count()
        if (selectedCount == 0) {
            button_preview.isEnabled = false
            button_apply.isEnabled = false
            button_apply.text = getString(R.string.button_apply_default)
        } else if (selectedCount == 1 && mSpec.singleSelectionModeEnabled()) {
            button_preview.isEnabled = true
            button_apply.setText(R.string.button_apply_default)
            button_apply.isEnabled = true
        } else {
            button_preview.isEnabled = true
            button_apply.isEnabled = true
            button_apply.text = getString(R.string.button_apply, selectedCount)
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.button_preview) {
            val intent = Intent(this, SelectedPreviewActivity::class.java)
            intent.putExtra(BasePreviewActivity.EXTRA_DEFAULT_BUNDLE, mSelectedCollection.dataWithBundle)
            startActivityForResult(intent, REQUEST_CODE_PREVIEW)
        } else if (v.id == R.id.button_apply) {
            val result = Intent()
            val selectedUris = mSelectedCollection.asListOfUri() as ArrayList<Uri>
            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris)
            val selectedPaths = mSelectedCollection.asListOfString() as ArrayList<String>
            result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        mAlbumCollection.setStateCurrentSelection(position)
        mAlbumsAdapter.cursor.moveToPosition(position)
        val album = Album.valueOf(mAlbumsAdapter.cursor)
        if (album.isAll && SelectionSpec.getInstance().capture) {
            album.addCaptureCount()
        }
        onAlbumSelected(album)
    }

    override fun onNothingSelected(parent: AdapterView<*>) {

    }

    override fun onAlbumLoad(cursor: Cursor) {
        mAlbumsAdapter.swapCursor(cursor)
        // select default album.
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            cursor.moveToPosition(mAlbumCollection.currentSelection)
            mAlbumsSpinner.setSelection(this@MatisseActivity, mAlbumCollection.currentSelection)
            val album = Album.valueOf(cursor)
            if (album.isAll && SelectionSpec.getInstance().capture) {
                album.addCaptureCount()
            }
            onAlbumSelected(album)
        }
    }

    override fun onAlbumReset() {
        mAlbumsAdapter.swapCursor(null)
    }

    private fun onAlbumSelected(album: Album) {
        if (album.isAll && album.isEmpty) {
            container.visibility = View.GONE
            empty_view.visibility = View.VISIBLE
        } else {
            container.visibility = View.VISIBLE
            empty_view.visibility = View.GONE
            val fragment = MediaSelectionFragment.newInstance(album)
            supportFragmentManager.beginTransaction().replace(R.id.container, fragment,
                    MediaSelectionFragment::class.java.simpleName).commitAllowingStateLoss()
        }
    }

    override fun onUpdate() {
        // notify bottom toolbar that check state changed.
        updateBottomToolbar()
    }

    override fun onMediaClick(album: Album?, item: Item, adapterPosition: Int) {
        val intent = Intent(this, AlbumPreviewActivity::class.java)
        intent.putExtra(AlbumPreviewActivity.EXTRA_ALBUM, album)
        intent.putExtra(AlbumPreviewActivity.EXTRA_ITEM, item)
        intent.putExtra(BasePreviewActivity.EXTRA_DEFAULT_BUNDLE, mSelectedCollection.dataWithBundle)
        startActivityForResult(intent, REQUEST_CODE_PREVIEW)
    }

    override fun provideSelectedItemCollection(): SelectedItemCollection {
        return mSelectedCollection
    }

    override fun capture() {
        val compat = mMediaStoreCompat
        compat?.dispatchCaptureIntent(this, REQUEST_CODE_CAPTURE)
    }

    companion object {
        const val EXTRA_RESULT_SELECTION = "extra_result_selection"
        const val EXTRA_RESULT_SELECTION_PATH = "extra_result_selection_path"
        private const val REQUEST_CODE_PREVIEW = 23
        private const val REQUEST_CODE_CAPTURE = 24
    }
}

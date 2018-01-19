/*
 * Copyright (C) 2014 nohana, Inc.
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse

import android.content.ContentResolver
import android.net.Uri
import android.support.v4.util.ArraySet
import android.text.TextUtils
import android.webkit.MimeTypeMap
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils
import java.util.*

/**
 * MIME Type enumeration to restrict selectable media on the selection activity. Matisse only supports images and
 * videos.
 *
 *
 * Good example of mime types Android supports:
 * https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/media/java/android/media/MediaFile.java
 */

private fun arraySetOf(vararg suffixes: String): Set<String> {
    return ArraySet(Arrays.asList(*suffixes))
}

enum class MimeType private constructor(private val mMimeTypeName: String, private val mExtensions: Set<String>) {

    // ============== images ==============
    JPEG("image/jpeg", arraySetOf("jpg", "jpeg")),
    PNG("image/png", arraySetOf("png")),
    GIF("image/gif", arraySetOf("gif")),
    BMP("image/x-ms-bmp", arraySetOf("bmp")),
    WEBP("image/webp", arraySetOf("webp")),

    // ============== videos ==============
    MPEG("video/mpeg", arraySetOf("mpeg", "mpg")),
    MP4("video/mp4", arraySetOf("mp4", "m4v")),
    QUICKTIME("video/quicktime", arraySetOf("mov")),
    THREEGPP("video/3gpp", arraySetOf("3gp", "3gpp")),
    THREEGPP2("video/3gpp2", arraySetOf("3g2", "3gpp2")),
    MKV("video/x-matroska", arraySetOf("mkv")),
    WEBM("video/webm", arraySetOf("webm")),
    TS("video/mp2ts", arraySetOf("ts")),
    AVI("video/avi", arraySetOf("avi"));

    override fun toString(): String {
        return mMimeTypeName
    }

    fun checkType(resolver: ContentResolver, uri: Uri?): Boolean {
        val map = MimeTypeMap.getSingleton()
        if (uri == null) {
            return false
        }
        val type = map.getExtensionFromMimeType(resolver.getType(uri))
        var path: String? = null
        // lazy load the path and prevent resolve for multiple times
        var pathParsed = false
        for (extension in mExtensions) {
            if (extension == type) {
                return true
            }
            if (!pathParsed) {
                // we only resolve the path for one time
                path = PhotoMetadataUtils.getPath(resolver, uri)

                if (!TextUtils.isEmpty(path)) {
                    path = path.toLowerCase(Locale.US)
                }
                pathParsed = true
            }
            if (path != null && path.endsWith(extension)) {
                return true
            }
        }
        return false
    }

    companion object {

        fun ofAll(): Set<MimeType> {
            return EnumSet.allOf(MimeType::class.java)
        }

        fun of(type: MimeType, vararg rest: MimeType): Set<MimeType> {
            return EnumSet.of(type, *rest)
        }

        fun ofImage(): Set<MimeType> {
            return EnumSet.of(JPEG, PNG, GIF, BMP, WEBP)
        }

        fun ofVideo(): Set<MimeType> {
            return EnumSet.of(MPEG, MP4, QUICKTIME, THREEGPP, THREEGPP2, MKV, WEBM, TS, AVI)
        }
    }
}

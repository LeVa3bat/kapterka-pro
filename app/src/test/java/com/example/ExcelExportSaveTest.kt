package com.example

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import com.example.util.ExcelExportHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ExcelExportSaveTest {
    private lateinit var context: Context
    private lateinit var provider: FailingDownloadsProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        provider = FailingDownloadsProvider()
        provider.attachInfo(context, ProviderInfo().apply { authority = "media"; exported = true })
        ShadowContentResolver.registerProviderInternal("media", provider)
    }

    @Test
    fun `no Downloads entry returns failure instead of reporting success`() {
        val name = "failed-download-insert.xlsx"
        assertNull(ExcelExportHelper.saveXlsxToDownloads(context, name, byteArrayOf(1, 2, 3)))
        assertTrue(provider.insertCalled)
        assertFalse(provider.openFileCalled)
        assertFalse(provider.deleted)
        assertFalse(File(context.cacheDir, "exports/$name").exists())
    }

    @Test
    fun `missing output stream removes the incomplete Downloads entry`() {
        provider.allowInsert = true
        assertNull(ExcelExportHelper.saveXlsxToDownloads(context, "failed-download-write.xlsx", byteArrayOf(1, 2, 3)))
        assertTrue(provider.insertCalled)
        assertTrue(provider.openFileCalled)
        assertTrue(provider.deleted)
    }

    private class FailingDownloadsProvider : ContentProvider() {
        var allowInsert = false
        var deleted = false
        var insertCalled = false
        var openFileCalled = false

        override fun onCreate() = true
        override fun insert(uri: Uri, values: ContentValues?): Uri? {
            insertCalled = true
            return if (allowInsert) Uri.parse("content://media/downloads/1") else null
        }

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
            openFileCalled = true
            return null
        }
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            deleted = true
            return 1
        }
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun getType(uri: Uri) = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    }
}

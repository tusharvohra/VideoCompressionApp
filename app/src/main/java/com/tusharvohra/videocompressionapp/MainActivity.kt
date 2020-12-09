package com.tusharvohra.videocompressionapp

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException
import kotlinx.android.synthetic.main.activity_main.*


open class MainActivity : AppCompatActivity() {

    private lateinit var ffmpeg: FFmpeg

    private var path: String? = null

    private lateinit var uri2: Uri


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ffmpeg = FFmpeg.getInstance(this)

        initView()
        loadFFMpegBinary()
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun initView() {
        btn_select_video.setOnClickListener {
            val intent = Intent()
            intent.type = "video/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Video"), 0)
            vv_video.visibility = View.VISIBLE
        }

        btn_compress.setOnClickListener{
            path = generatePath(uri2, this.applicationContext)
            Log.i("APPDATA", path!!)
            val cmd = "-i /storage/self/primary/Movies/Instagram/VID_80890326_234112_584.mp4 -b 80k /storage/self/primary/CompressedVideos/output.mp4"
            val command: Array<String> = cmd.split(" ").toTypedArray()
            execFFmpegBinary(command)
        }

    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            uri2 = uri!!
            try {
                vv_video.setVideoURI(uri)
                vv_video.start()
                vv_video.canPause()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun generatePath(uri: Uri, context: Context): String? {
        var filePath: String? = null
        if (DocumentsContract.isDocumentUri(context, uri)) {
            val wholeID = DocumentsContract.getDocumentId(uri)
            val id = wholeID.split(":").toTypedArray()[1]
            val column = arrayOf(MediaStore.Video.Media.DATA)
            val sel: String = MediaStore.Video.Media._ID + "=?"
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                column, sel, arrayOf(id), null
            )
            val columnIndex = cursor?.getColumnIndex(column[0])
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    filePath = columnIndex?.let { cursor.getString(it) }
                }
            }
            cursor?.close()
        }
        return filePath
    }

    private fun loadFFMpegBinary() {
        try {
            ffmpeg.loadBinary(object : LoadBinaryResponseHandler() {
                override fun onFailure() {
                    showUnsupportedExceptionDialog()
                }
            })
        } catch (e: FFmpegNotSupportedException) {
            showUnsupportedExceptionDialog()
        }
    }

    private fun showUnsupportedExceptionDialog() {
        AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_baseline_announcement_24)
            .setTitle("Device Not Supported")
            .setMessage("FFmpeg is not supported on your device")
            .setCancelable(false)
            .setPositiveButton(
                "OK",
                DialogInterface.OnClickListener { dialog, which -> this.finish() })
            .create()
            .show()
    }

    private fun execFFmpegBinary(command: Array<String>) {
        try {
            ffmpeg.execute(command, object : ExecuteBinaryResponseHandler() {
                override fun onFailure(s: String) {
                    Log.d("APPDATA", "Failure : $s")
//                    addTextViewToLayout("FAILED with output : $s")
                }

                override fun onSuccess(s: String) {
                    Log.d("APPDATA", "Success : $s")
//                    addTextViewToLayout("SUCCESS with output : $s")
                }

                override fun onProgress(s: String) {
                    Log.d("APPDATA", "Started command : ffmpeg $command")
//                    addTextViewToLayout("progress : $s")
//                    progressDialog.setMessage("Processing\n$s")
                }

                override fun onStart() {
//                    outputLayout.removeAllViews()
                    Log.d("APPDATA", "Started command : ffmpeg $command")
//                    progressDialog.setMessage("Processing...")
//                    progressDialog.show()
                }

                override fun onFinish() {
                    Log.d("APPDATA", "Finished command : ffmpeg $command")
//                    progressDialog.dismiss()
                }
            })
        } catch (e: FFmpegCommandAlreadyRunningException) {
            // do nothing for now
        }
    }


}
package com.example.snapitout

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class PdfPreviewActivity : AppCompatActivity() {

    private lateinit var pdfPath: String
    private lateinit var previewImageView: ImageView
    private lateinit var printButton: Button
    private var currentPrintJob: PrintJob? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_preview)

        previewImageView = findViewById(R.id.pdfImageView)
        printButton = findViewById(R.id.printButton)

        pdfPath = intent.getStringExtra("PDF_PATH") ?: return

        renderPdfPreview(pdfPath)

        printButton.setOnClickListener {
            printPdf(pdfPath)
        }
    }

    /** Render the first page of the PDF into an ImageView with smoothing */
    private fun renderPdfPreview(path: String) {
        val file = File(path)
        val parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(parcelFileDescriptor)
        if (renderer.pageCount > 0) {
            val page = renderer.openPage(0)

            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            previewImageView.setLayerType(ImageView.LAYER_TYPE_SOFTWARE, paint)
            previewImageView.setImageBitmap(bitmap)

            page.close()
        }
        renderer.close()
        parcelFileDescriptor.close()
    }

    /** Print the PDF and monitor PrintJob completion */
    private fun printPdf(path: String) {
        val file = File(path)
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "SnapItOut_Print"

        val printAdapter = object : android.print.PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = android.print.PrintDocumentInfo.Builder(file.name)
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    file.inputStream().use { input ->
                        android.os.ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        val attributes = PrintAttributes.Builder()
            .setResolution(PrintAttributes.Resolution("res1", "res1", 300, 300))
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        // Capture the PrintJob
        currentPrintJob = printManager.print(jobName, printAdapter, attributes)

        // Monitor the PrintJob status in background
        Thread {
            while (currentPrintJob != null &&
                !currentPrintJob!!.isCompleted &&
                !currentPrintJob!!.isFailed &&
                !currentPrintJob!!.isCancelled) {
                Thread.sleep(500)
            }
            runOnUiThread {
                currentPrintJob?.let {
                    if (it.isCompleted) {
                        Toast.makeText(
                            this,
                            "Print finished",
                            Toast.LENGTH_SHORT
                        ).show()
                        // 🔹 FIX: simply finish this preview activity
                        finish()  // return to EventEditActivity as-is
                    } else if (it.isFailed || it.isCancelled) {
                        Toast.makeText(this, "Print was cancelled or failed.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }
}

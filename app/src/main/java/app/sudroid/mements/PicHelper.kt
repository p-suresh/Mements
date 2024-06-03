package app.sudroid.mements

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getColor
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cbrt

class PicHelper(var context: Context) {
    fun savePic(uri: Uri, name: String)/*: String?*/ {
        val cw = ContextWrapper(context)
        val directory = cw.getDir("pp", AppCompatActivity.MODE_PRIVATE)
        // Create imageDir
        val profilePath = File(directory, "${name}.jpg")
        var fos: FileOutputStream? = null
        val iStream = this.context.contentResolver.openInputStream(uri)
        val bitmapOrigImage = BitmapFactory.decodeStream(iStream)
        val bitmapImage = Bitmap.createScaledBitmap(bitmapOrigImage, 960, 960, false)
        iStream?.close()
        try {
            fos = FileOutputStream(profilePath)
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            try {
                fos!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
//        return directory.absolutePath
    }

    fun deletePic(name: String) {
        val cw = ContextWrapper(context)
        val directory = cw.getDir("pp", AppCompatActivity.MODE_PRIVATE)
        // Create imageDir
        try {
            val file = File(directory, "${name}.jpg")
            if (file.exists()) {
                file.delete()
            }
//            Toast.makeText(applicationContext, "${memberDetails?.fullName}_profile.jpg", Toast.LENGTH_LONG).show()
        } catch (e: java.lang.Exception) {
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
        }
    }

    fun loadPic(name: String, imageView: ImageView) {
        val cw = ContextWrapper(context)
        val directory = cw.getDir("pp", AppCompatActivity.MODE_PRIVATE)
        try {
            val file = File(directory, "${name}.jpg")
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeStream(FileInputStream(file))
                imageView.setImageBitmap(bitmap)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    fun getAvatar(context: Context, shape: Int, charAt: Char): Bitmap {
        val text = charAt.toString().uppercase(Locale.getDefault())
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val squirclePath = getSquirclePath(-4, -4, 30)
        val paintFill = Paint()
        val bounds = Rect()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textSize = 33f
        paint.setShadowLayer(1f, 0f, 1f, Color.GRAY)
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.getTextBounds(text, 0, 1, bounds)
        paintFill.color = getColor(context, R.color.colorPrimary)
        paintFill.isAntiAlias = true
        val x = canvas.width / 2
        val y = (canvas.height / 2 - (paint.descent() + paint.ascent()) / 2).toInt()
        if (shape == 2)
            canvas.drawPath(squirclePath, paintFill)
        else {
            val rect = Rect(0, 0, 48, 48)
            val rectF = RectF(rect)
            canvas.drawRoundRect(rectF, 48f, 48f, paintFill)
        }
        canvas.drawText(text, x.toFloat(), y.toFloat(), paint)
        return bitmap
    }

    private fun getSquirclePath(left: Int, top: Int, radius: Int): Path {
        //Formula: (|x|)^3 + (|y|)^3 = radius^3
        val radiusToPow = (radius * radius * radius).toDouble()
        val path = Path()
        path.moveTo((-radius).toFloat(), 0F)
        for (x in -radius..radius) path.lineTo(
            x.toFloat(),
            cbrt(radiusToPow - abs(x * x * x)).toFloat()
        )
        for (x in radius downTo -radius) path.lineTo(
            x.toFloat(),
            -cbrt(radiusToPow - abs(x * x * x)).toFloat()
        )
        path.close()
        val matrix = Matrix()
        matrix.postTranslate((left + radius).toFloat(), (top + radius).toFloat())
        path.transform(matrix)
        return path
    }
}

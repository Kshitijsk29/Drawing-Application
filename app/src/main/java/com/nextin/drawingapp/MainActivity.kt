package com.nextin.drawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.nextin.drawingapp.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private var binding : ActivityMainBinding? = null

    private var drawingview: DrawingView? =null
    private var mImageButtonPaintColor:ImageButton? =null

    private val openGallaryLauncher : ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            result->
            if (result.resultCode == RESULT_OK && result.data !=null)
            {
                val imageBackground :ImageView =findViewById(R.id.iv_background)
                imageBackground.setImageURI(result.data?.data)
            }
        }
     val requestPermission : ActivityResultLauncher<Array<String>> =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
    {
        permission ->
        permission.entries.forEach {
            val permissionName =it.key
            val isGranted = it.value
            if(isGranted)
            {
                Toast.makeText(
                    this@MainActivity,
                    "Permission granted now you can read the storage files.",
                    Toast.LENGTH_LONG).show()

                val pickIntent =Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGallaryLauncher.launch(pickIntent)
            }
            else
            {
                if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE){
                    Toast.makeText(
                        this@MainActivity,
                        "Oops you just denied the permission.",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        drawingview = findViewById(R.id.drawing_view)

        drawingview?.setSizeForBrush(20.toFloat())

        val linearlayoutPaintColor = findViewById<LinearLayout>(R.id.ll_pallet)
        mImageButtonPaintColor =linearlayoutPaintColor[1] as ImageButton
        mImageButtonPaintColor!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        //val ibBursh : ImageButton = findViewById(R.id.ib_brush)
        binding?.ibBrush?.setOnClickListener {
            showBrushSizeDialog()
        }

        //val ibUndo : ImageButton = findViewById(R.id.ibUndo)
        binding?.ibUndo?.setOnClickListener {
            drawingview?.onClickUndoButton()
        }

       // val ibRedo : ImageButton = findViewById(R.id.ibRedo)
        binding?.ibRedo?.setOnClickListener {
            drawingview?.onClickRedo()
        }
        //val ibSave : ImageButton = findViewById(R.id.ibSave)
        binding?.ibSave?.setOnClickListener {
            if(isReadStorageAllowed()){
                lifecycleScope.launch{
                    val flDrawingView :FrameLayout
                    =findViewById(R.id.fl_drawing_view_container)
                    savedBitmapFile(getBitMapFromView(flDrawingView))
                }
            }
        }

       // val ibGallery: ImageButton = findViewById(R.id.ibGallery)
        binding?.ibGallery?.setOnClickListener {
            requestStoragePermission()
        }


    }

    private fun showBrushSizeDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size :")

        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingview?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            drawingview?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener {
            drawingview?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

     fun paintClicked(view: View){
        if (view !=mImageButtonPaintColor)
        {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingview?.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
            )

            mImageButtonPaintColor?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )
            mImageButtonPaintColor =view
        }
    }
      fun showRationalDailog(
        title : String,
        message : String
    ){
        val builder :AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){dialog, _->
            dialog.dismiss()
            }
        builder.create().show()
    }


     private fun isReadStorageAllowed(): Boolean{
         val result = ContextCompat.checkSelfPermission(this,
             Manifest.permission.READ_EXTERNAL_STORAGE)

         return result == PackageManager.PERMISSION_GRANTED
     }
    private fun requestStoragePermission()
    {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE))
        {
            showRationalDailog("Kids Drawing App","Kids Drawing App " +
                    "needs to Access Your External Storage")
        }
        else
        {
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
            )
        }
    }

    private  fun getBitMapFromView(view: View) :Bitmap{
        val returnedBitmap =Bitmap.createBitmap(
            view.width, view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background

        if (bgDrawable !=null){
            bgDrawable.draw(canvas)
        }else
        {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)

        return returnedBitmap
    }

    private suspend fun savedBitmapFile(mBitmap: Bitmap?):String{
        var result =""
        withContext(Dispatchers.IO){
            if(mBitmap!=null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString() +
                            File.separator
                    + "DrawingApp_" + System.currentTimeMillis()/1000 + ".png")

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()


                    result =f.absolutePath
                    runOnUiThread{
                        if (result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,
                                "File Saved Successfully :$result"
                            ,Toast.LENGTH_SHORT).show()
                        }else
                        {
                            Toast.makeText(this@MainActivity,
                                "Something went wrong while saving the file "
                                ,Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                catch (e: Exception){
                    result=""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
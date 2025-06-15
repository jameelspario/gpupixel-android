package com.android.gpupixel

import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.gpupixel.databinding.ActivityMainBinding
import com.pixpark.gpupixel.FaceDetector
import com.pixpark.gpupixel.GPUPixel
import com.pixpark.gpupixel.GPUPixelFilter
import com.pixpark.gpupixel.GPUPixelSinkRawData
import com.pixpark.gpupixel.GPUPixelSourceRawData


class MainActivity : AppCompatActivity() {

    val CAMERA_PERMISSION_REQUEST_CODE: Int = 200
    val TAG: String = "GPUPixelDemo"

    private var mCamera2Helper: Camera2Helper? = null
    private var mSourceRawData: GPUPixelSourceRawData? = null
    private var mBeautyFilter: GPUPixelFilter? = null
    private var mFaceReshapeFilter: GPUPixelFilter? = null
    private var mLipstickFilter: GPUPixelFilter? = null
    private var mFaceDetector: FaceDetector? = null
    private var mSinkRawData: GPUPixelSinkRawData? = null

    private var mSmoothSeekbar: SeekBar? = null
    private var mWhitenessSeekbar: SeekBar? = null
    private var mThinFaceSeekbar: SeekBar? = null
    private var mBigeyeSeekbar: SeekBar? = null
    private var lipstickSeekbar: SeekBar? = null
    private var mGLSurfaceView: GLSurfaceView? = null
    private var mRenderer: MyRenderer? = null

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // Get log path
        val path = getExternalFilesDir("gpupixel")!!.absolutePath
        Log.i(TAG, "Log path: $path")


        // Initialize GPUPixel
        GPUPixel.Init(this)


        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        // Initialize UI
        initUI()


        // Check camera permission
        checkCameraPermission()
    }

    private fun initUI() {
        val orie = getWindowManager().getDefaultDisplay().getRotation() * 90;
        mGLSurfaceView = binding.surfaceView
        mGLSurfaceView?.setEGLContextClientVersion(2)
        mRenderer = MyRenderer(orie, mCamera2Helper)
        mGLSurfaceView?.setRenderer(mRenderer!!)

        // Setup beauty slider
        mSmoothSeekbar = binding.smoothSeekbar
        mSmoothSeekbar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mBeautyFilter?.SetProperty("skin_smoothing", progress / 10.0f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        mWhitenessSeekbar = binding.whitenessSeekbar
        mWhitenessSeekbar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mBeautyFilter?.SetProperty("whiteness", progress / 10.0f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        mThinFaceSeekbar = binding.thinfaceSeekbar
        mThinFaceSeekbar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mFaceReshapeFilter?.SetProperty("thin_face", progress / 160.0f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        mBigeyeSeekbar = binding.bigeyeSeekbar
        mBigeyeSeekbar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mFaceReshapeFilter?.SetProperty("big_eye", progress / 40.0f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        lipstickSeekbar = binding.lipstickSeekbar
        lipstickSeekbar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mLipstickFilter?.SetProperty("blend_level", progress / 10.0f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun setupCamera() {
        // Create and initialize camera
        mCamera2Helper = Camera2Helper(this)

        // Create GPUPixel processing chain
        mSourceRawData = GPUPixelSourceRawData.Create()

        // Create filters
        mBeautyFilter = GPUPixelFilter.Create(GPUPixelFilter.BEAUTY_FACE_FILTER)
        mFaceReshapeFilter = GPUPixelFilter.Create(GPUPixelFilter.FACE_RESHAPE_FILTER)
        mLipstickFilter = GPUPixelFilter.Create(GPUPixelFilter.LIPSTICK_FILTER)

        // Create output sink
        mSinkRawData = GPUPixelSinkRawData.Create()

        // Initialize face detection
        mFaceDetector = FaceDetector.Create()

        // Set camera frame callback
        mCamera2Helper?.setFrameCallback(Camera2Helper.FrameCallback { rgbaData, width, height ->
            // Get sensor orientation
            val sensorOrientation = mCamera2Helper!!.getSensorOrientation()

            // Check if front camera
            val isFrontCamera = GPUPixel.isFrontCamera(Camera2Helper.CAMERA_FACING)

            // Calculate rotation using GPUPixel
            val rotation = GPUPixel.calculateRotation(
                this@MainActivity, sensorOrientation, isFrontCamera
            )

            // Rotate RGBA data using GPUPixel
            val rotatedData = GPUPixel.rotateRgbaImage(rgbaData, width, height, rotation)

            // Width and height may be swapped after rotation
            val outWidth = if ((rotation == 90 || rotation == 270)) height else width
            val outHeight = if ((rotation == 90 || rotation == 270)) width else height

            // Perform face detection (using rotated data)
            val landmarks = mFaceDetector?.detect(
                rotatedData, outWidth, outHeight,
                outWidth * 4, FaceDetector.GPUPIXEL_MODE_FMT_VIDEO,
                FaceDetector.GPUPIXEL_FRAME_TYPE_RGBA
            )

            if (landmarks != null && landmarks.size > 0) {
                Log.d(TAG, "Face landmarks detected: " + landmarks.size)
                mFaceReshapeFilter?.SetProperty("face_landmark", landmarks)
                mLipstickFilter?.SetProperty("face_landmark", landmarks)
            }

            // Process the rotated RGBA data with GPUPixelSourceRawData
            mSourceRawData?.ProcessData(
                rotatedData, outWidth, outHeight, outWidth * 4,
                GPUPixelSourceRawData.FRAME_TYPE_RGBA
            )

            // Get processed RGBA data
            val processedRgba = mSinkRawData?.GetRgbaBuffer()

            // Set texture data and request redraw
            if (processedRgba != null && mRenderer != null) {
                val rgbaWidth = mSinkRawData?.GetWidth()
                val rgbaHeight = mSinkRawData?.GetHeight()

                mRenderer?.updateTextureData(processedRgba, rgbaWidth!!, rgbaHeight!!, 0)

                // Request GLSurfaceView to redraw
                mGLSurfaceView!!.requestRender()
            }
        })

        mSourceRawData?.AddSink(mLipstickFilter)
        mLipstickFilter?.AddSink(mBeautyFilter)
        mBeautyFilter?.AddSink(mFaceReshapeFilter)
        mFaceReshapeFilter?.AddSink(mSinkRawData)

        // Start camera
        mCamera2Helper?.startCamera()
    }

    fun checkCameraPermission() {
        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // If no camera permission, request permission
            ActivityCompat.requestPermissions(
                this, arrayOf<String>(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            // Has permission, set up camera
            setupCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera()
            } else {
                Toast.makeText(this, "No camera permission!", LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mGLSurfaceView!!.onResume()

        if (mCamera2Helper != null && !mCamera2Helper!!.isCameraOpened) {
            mCamera2Helper!!.startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        mGLSurfaceView!!.onPause()

        if (mCamera2Helper != null) {
            mCamera2Helper!!.stopCamera()
        }
    }

    override fun onDestroy() {
        // Release camera resources
        if (mCamera2Helper != null) {
            mCamera2Helper!!.stopCamera()
            mCamera2Helper = null
        }

        // Release face detector
        if (mFaceDetector != null) {
            mFaceDetector!!.destroy()
            mFaceDetector = null
        }
        //
        // Release GPUPixel resources
        if (mBeautyFilter != null) {
            mBeautyFilter!!.Destroy()
            mBeautyFilter = null
        }
        if (mFaceReshapeFilter != null) {
            mFaceReshapeFilter!!.Destroy()
            mFaceReshapeFilter = null
        }

        // Release GPUPixel resources
        if (mLipstickFilter != null) {
            mLipstickFilter!!.Destroy()
            mLipstickFilter = null
        }

        if (mSourceRawData != null) {
            mSourceRawData!!.Destroy()
            mSourceRawData = null
        }

        if (mSinkRawData != null) {
            mSinkRawData!!.Destroy()
            mSinkRawData = null
        }

        super.onDestroy()
    }
}
package com.oddlyspaced.testcam

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.oddlyspaced.testcam.databinding.ActivityMainBinding

// https://stackoverflow.com/questions/39893367/how-to-show-the-camera-preview-on-a-surfaceview
class MainActivity : AppCompatActivity(), SurfaceHolder.Callback, Handler.Callback {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "CamTest"
        private const val MSG_CAMERA_OPENED = 1
        private const val MSG_SURFACE_READY = 2
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 1001
    }

    private val handler = Handler(this)
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var cameraManager: CameraManager
    private var cameraIdList = arrayOf<String>()
    private lateinit var cameraStateCallBack: CameraDevice.StateCallback
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private var surfaceCreated = true
    private var isCameraConfigured = false
    private lateinit var cameraSurface: Surface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        surfaceView = binding.surfaceView
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        try {
            cameraIdList = cameraManager.cameraIdList
            for (id in cameraIdList) {
                Log.d("CAMERA ID", id)
                Log.d("CAMERA CHAR", cameraManager.getCameraCharacteristics(id).physicalCameraIds.toString())
                Log.d("---", "------")
            }


        }
        catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        cameraStateCallBack = object : CameraDevice.StateCallback() {
            override fun onOpened(p0: CameraDevice) {
                Toast.makeText(applicationContext, "on opened", Toast.LENGTH_SHORT).show()

                cameraDevice = p0
                handler.sendEmptyMessage(MSG_CAMERA_OPENED)
            }

            override fun onDisconnected(p0: CameraDevice) {
                Toast.makeText(applicationContext, "on disconnected", Toast.LENGTH_SHORT).show()
            }

            override fun onError(p0: CameraDevice, p1: Int) {
                Toast.makeText(applicationContext, "onError", Toast.LENGTH_SHORT).show();
            }

        }

    }

    override fun surfaceCreated(p0: SurfaceHolder) {
        cameraSurface = p0.surface
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        cameraSurface = p0.surface
        surfaceCreated = true
        handler.sendEmptyMessage(MSG_SURFACE_READY);
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        surfaceCreated = false
    }

    override fun handleMessage(p0: Message): Boolean {
        when(p0.what) {
            MSG_CAMERA_OPENED -> {
                if (surfaceCreated && this::cameraDevice.isInitialized && !isCameraConfigured) {
                    configureCamera()
                }
            }
            MSG_SURFACE_READY -> {
                if (surfaceCreated && this::cameraDevice.isInitialized && !isCameraConfigured) {
                    configureCamera()
                }
            }
        }
        return true
    }

    private fun configureCamera() {
        // prepare list of surfaces to be used in capture requests
        val sfl = arrayListOf<Surface>()
        sfl.add(cameraSurface)
        try {
            cameraDevice.createCaptureSession(sfl, CaptureSessionListener(), null)
        }
        catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        isCameraConfigured = true
    }

    private inner class CaptureSessionListener : CameraCaptureSession.StateCallback() {
        override fun onConfigured(p0: CameraCaptureSession) {
            Log.d(TAG, "CaptureSessionConfigure onConfigured")
            captureSession = p0
            try {
                val previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(cameraSurface)
                }
                captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null)
            }
            catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        override fun onConfigureFailed(p0: CameraCaptureSession) {
            Log.d(TAG, "CaptureSessionConfigure Failed")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        cameraManager.openCamera(cameraIdList[1], cameraStateCallBack, Handler())
                    }
                    catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // request permission
        val permCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (permCheck != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(this, arrayOf<String>(Manifest.permission.CAMERA), MY_PERMISSIONS_REQUEST_CAMERA)
                Toast.makeText(applicationContext, "Request Permission", Toast.LENGTH_SHORT).show()
            }
        }
        else {
            try {
                cameraManager.openCamera(cameraIdList[1], cameraStateCallBack, Handler())
            }
            catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

    override fun onStop() {
        super.onStop()

        try {
            if (this::captureSession.isInitialized) {
                captureSession.stopRepeating()
                captureSession.close()
            }
            isCameraConfigured = false
        }
        catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        finally {
            if (this::cameraDevice.isInitialized) {
                cameraDevice.close()
            }
        }
    }


}
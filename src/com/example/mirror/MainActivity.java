package com.example.mirror;

import java.io.File;
import java.io.FileOutputStream;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

public class MainActivity extends Activity implements PictureCallback {

	// Fields -----------------------------------------------------------------
	private Camera camera = null;
	private MirrorView camPreview = null;
	private FrameLayout previewLayout = null;
	private int cameraId = 0;
	
	// Methods ----------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		// Base implementation
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Find out if we even have a camera
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) {
			Toast.makeText(
					this,
					"No camera feature on this device",
					Toast.LENGTH_SHORT)
					.show();
		} else {
		
			// Get the ID of the front facing camera
			cameraId = findFirstFrontFacingCamera();
			
			// If we have a valid camera
			if (cameraId > 0) {
			
				// Get the preview frame and strip it of all of it's views
				previewLayout = (FrameLayout)findViewById(R.id.camPreview);
				previewLayout.removeAllViews();
			
				// Start the camera
				startCameraInLayout(previewLayout, cameraId);
				
				// Set up the button
				Button takePic = (Button)findViewById(R.id.capture);
				takePic.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						camera.takePicture(null, null, MainActivity.this);
					}
				});
			} else {
				Toast.makeText(
						this,
						"No front facing camera found",
						Toast.LENGTH_SHORT)
						.show();
			}
		}
	}
	@Override
	protected void onPause() {
		if (camera != null) {
			camera.release();
			camera = null;
		}
		super.onPause();
	}
	@Override
	protected void onResume() {
		super.onResume();
		if (camera == null && previewLayout != null) {
			previewLayout.removeAllViews();
			startCameraInLayout(previewLayout, cameraId);
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	private int findFirstFrontFacingCamera() {
		int foundId = -1;
		int numCams = Camera.getNumberOfCameras();
		for (int camId = 0; camId < numCams; camId++) {
			
			// Get the camera information
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(camId, info);
			
			// Check to see if this is a front facing camera
			if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
				foundId = camId;
				break;
			}
		}
		return foundId;
	}
	private void startCameraInLayout(FrameLayout layout, int cameraId) {
		camera = Camera.open(cameraId);
		if (camera != null) {
			
			// Create a new MirrorView object and 
			// add the view to the FrameLayout
			camPreview = new MirrorView(this, camera);
			layout.addView(camPreview);
			
		}
	}
	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		File pictureFileDir = new File(
				Environment.getExternalStoragePublicDirectory(
						Environment.DIRECTORY_PICTURES),
						"SimpleSelfCam");
		
		// If the picture directory does not and exist and 
		// we failed to create it
		if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
			Toast.makeText(
					this,
					"Can't create directory to save image",
					Toast.LENGTH_SHORT)
					.show();
			return;
		}
		
		// Create a new file for the image to be saved to
		String filename = pictureFileDir.getPath() + File.separator +
				"latest_mug.jpg";
		File pictureFile = new File(filename);
		
		// Write the image to storage
		try {
			FileOutputStream fos = new FileOutputStream(pictureFile);
			fos.write(data);
			fos.close();
			Toast.makeText(
					this,
					"Image saved as latest_mug.jpg",
					Toast.LENGTH_SHORT)
					.show();
		} catch (Exception ex) {
			Toast.makeText(
					this,
					"File not saved: " + ex.getMessage(),
					Toast.LENGTH_SHORT)
					.show();
		}
		
	}
	
	// Classes ----------------------------------------------------------------
	public class MirrorView extends SurfaceView implements SurfaceHolder.Callback {

		// Fields -----------------------------------------------------------------
		private SurfaceHolder holder;
		private Camera camera;
		
		// Constructor ------------------------------------------------------------
		@SuppressWarnings("deprecation")
		public MirrorView(Context context, Camera camera) {
			super(context);
			this.camera = camera;
			holder = getHolder();
			holder.addCallback(this);
			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		// Methods ----------------------------------------------------------------
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			
			// If the surface does not exist, just exit this method
			if (holder.getSurface() == null) {
				return;
			}
			
			// Otherwise, stop the camera...
			try {
				camera.stopPreview();
			} catch (Exception ex) {
				Toast.makeText(
						this.getContext(),
						ex.getMessage(),
						Toast.LENGTH_SHORT)
						.show();
			}
			
			// ... and reset the display window
			try {
				setCameraDisplayOrientationAndSize();
				camera.setPreviewDisplay(holder);
				camera.startPreview();
			} catch (Exception ex) {
				Toast.makeText(
						this.getContext(),
						"Error starting camera preview",
						Toast.LENGTH_SHORT)
						.show();
			}
		}
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			try {
				
				// Set the camera preview display and
				// start the preview capture
				camera.setPreviewDisplay(holder);
				camera.startPreview();
				
			} catch (Exception ex) {
				Toast.makeText(
						this.getContext(), 
						"Error starting camera preview",
						Toast.LENGTH_SHORT)
						.show();
			}
		}
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {		
		}
		private void setCameraDisplayOrientationAndSize() {
			int result;
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(cameraId, info);
			int rotation = getWindowManager().getDefaultDisplay()
											 .getRotation();
			int degrees = rotation * 90;
			
			// If this is a front facing camera
			if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
				result = (info.orientation + degrees) % 360;
				result = (360 - result) % 360;
			} else {
				result = (info.orientation - degrees + 360) % 360;
			}
			
			// Set the display orientation of the camera and 
			// get the preview size
			camera.setDisplayOrientation(result);
			Camera.Size previewSize = camera.getParameters().getPreviewSize();
			
			// Set the width and height of the preview window 
			// with a dependency of the rotation angle
			if (result == 90 || result == 270) {
				holder.setFixedSize(previewSize.height, previewSize.width);
			} else {
				holder.setFixedSize(previewSize.width, previewSize.height);
			}
		}
		
	}
	
}

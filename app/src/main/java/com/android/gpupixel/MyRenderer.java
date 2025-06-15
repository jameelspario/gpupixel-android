package com.android.gpupixel;

import android.hardware.camera2.CameraCharacteristics;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

// OpenGL Renderer class
class MyRenderer implements GLSurfaceView.Renderer {
    private int mTextureID = -1;
    private int mProgramHandle;
    private int mPositionHandle;
    private int mTextureCoordHandle;
    private int mTextureSamplerHandle;

    private ByteBuffer mTextureData;
    private int mTextureWidth;
    private int mTextureHeight;
    private int mRotation = 0; // Image rotation angle
    private boolean mTextureNeedsUpdate = false;
    private final Object mLock = new Object();

    int deviceOri;
    Camera2Helper mCamera2Helper;

    public MyRenderer(int deviceOri, Camera2Helper camera2Helper) {
        this.deviceOri = deviceOri;
        this.mCamera2Helper = camera2Helper;
    }

    // Vertex coordinates
    private final float[] VERTICES = {
            -1.0f, -1.0f, // Bottom left
            1.0f, -1.0f, // Bottom right
            -1.0f, 1.0f, // Top left
            1.0f, 1.0f // Top right
    };

    // Texture coordinates - default (0° rotation)
    private final float[] TEXTURE_COORDS_0 = {
            0.0f, 1.0f, // Bottom left
            1.0f, 1.0f, // Bottom right
            0.0f, 0.0f, // Top left
            1.0f, 0.0f // Top right
    };

    // Texture coordinates - 90° clockwise rotation
    private final float[] TEXTURE_COORDS_90 = {
            0.0f, 0.0f, // Bottom left
            0.0f, 1.0f, // Bottom right
            1.0f, 0.0f, // Top left
            1.0f, 1.0f // Top right
    };

    // Texture coordinates - 180° clockwise rotation
    private final float[] TEXTURE_COORDS_180 = {
            1.0f, 0.0f, // Bottom left
            0.0f, 0.0f, // Bottom right
            1.0f, 1.0f, // Top left
            0.0f, 1.0f // Top right
    };

    // Texture coordinates - 270° clockwise rotation
    private final float[] TEXTURE_COORDS_270 = {
            1.0f, 1.0f, // Bottom left
            1.0f, 0.0f, // Bottom right
            0.0f, 1.0f, // Top left
            0.0f, 0.0f // Top right
    };

    // Texture coordinates for front camera mirroring - default (0° rotation)
    private final float[] TEXTURE_COORDS_MIRROR_0 = {
            1.0f, 1.0f, // Bottom left
            0.0f, 1.0f, // Bottom right
            1.0f, 0.0f, // Top left
            0.0f, 0.0f // Top right
    };

    // Currently used texture coordinates
    private float[] TEXTURE_COORDS = TEXTURE_COORDS_0;

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTexCoordBuffer;

    // View dimension properties
    private int mViewWidth;
    private int mViewHeight;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set background color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Initialize vertex coordinate buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mVertexBuffer = bb.asFloatBuffer();
        mVertexBuffer.put(VERTICES);
        mVertexBuffer.position(0);

        // Initialize texture coordinate buffer - using default texture coordinates
        updateTextureCoordinates(0);

        // Create vertex shader
        String vertexShaderCode = "attribute vec2 aPosition;\n"
                + "attribute vec2 aTextureCoord;\n"
                + "varying vec2 vTextureCoord;\n"
                + "void main() {\n"
                + "  gl_Position = vec4(aPosition, 0.0, 1.0);\n"
                + "  vTextureCoord = aTextureCoord;\n"
                + "}";

        // Create fragment shader
        String fragmentShaderCode = "precision mediump float;\n"
                + "varying vec2 vTextureCoord;\n"
                + "uniform sampler2D uTexture;\n"
                + "void main() {\n"
                + "  gl_FragColor = texture2D(uTexture, vTextureCoord);\n"
                + "}";

        // Compile shaders
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        // Create program
        mProgramHandle = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgramHandle, vertexShader);
        GLES20.glAttachShader(mProgramHandle, fragmentShader);
        GLES20.glLinkProgram(mProgramHandle);

        // Get attribute locations
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        mTextureCoordHandle = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        mTextureSamplerHandle = GLES20.glGetUniformLocation(mProgramHandle, "uTexture");

        // Create texture
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureID = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);

        // Set texture parameters
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    // Update texture coordinates based on rotation angle
    private void updateTextureCoordinates(int rotation) {
        boolean isFrontCamera = (mCamera2Helper != null)
                && (Camera2Helper.CAMERA_FACING == CameraCharacteristics.LENS_FACING_FRONT);

        // Select appropriate texture coordinates
        switch (rotation) {
            case 90:
                TEXTURE_COORDS = isFrontCamera ? TEXTURE_COORDS_270 : TEXTURE_COORDS_90;
                break;
            case 180:
                TEXTURE_COORDS = isFrontCamera ? TEXTURE_COORDS_180 : TEXTURE_COORDS_180;
                break;
            case 270:
                TEXTURE_COORDS = isFrontCamera ? TEXTURE_COORDS_90 : TEXTURE_COORDS_270;
                break;
            default: // 0 degrees
                TEXTURE_COORDS = isFrontCamera ? TEXTURE_COORDS_MIRROR_0 : TEXTURE_COORDS_0;
                break;
        }

        // Update texture coordinate buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(TEXTURE_COORDS.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mTexCoordBuffer = bb.asFloatBuffer();
        mTexCoordBuffer.put(TEXTURE_COORDS);
        mTexCoordBuffer.position(0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        // Store view dimensions for later calculations
        mViewWidth = width;
        mViewHeight = height;

        // If texture data already exists, update vertex coordinates to match texture aspect
        // ratio
        if (mTextureWidth > 0 && mTextureHeight > 0) {
            updateVertexCoordinates();
        }
    }

    // Update vertex coordinates to maintain video aspect ratio
    private void updateVertexCoordinates() {
        if (mViewWidth <= 0 || mViewHeight <= 0 || mTextureWidth <= 0 || mTextureHeight <= 0) {
            return;
        }

        // Calculate view and texture aspect ratios
        float viewAspectRatio = (float) mViewWidth / mViewHeight;
        float textureAspectRatio;

        // Consider rotation
        if (mRotation == 90 || mRotation == 270) {
            // Width and height are swapped after rotation
            textureAspectRatio = (float) mTextureHeight / mTextureWidth;
        } else {
            textureAspectRatio = (float) mTextureWidth / mTextureHeight;
        }

        // Set vertex coordinates while maintaining texture aspect ratio
        float[] adjustedVertices = new float[8];

        if (textureAspectRatio > viewAspectRatio) {
            // Texture is wider than view, adjust height
            float heightRatio = viewAspectRatio / textureAspectRatio;
            adjustedVertices[0] = -1.0f; // Bottom left x
            adjustedVertices[1] = -heightRatio; // Bottom left y
            adjustedVertices[2] = 1.0f; // Bottom right x
            adjustedVertices[3] = -heightRatio; // Bottom right y
            adjustedVertices[4] = -1.0f; // Top left x
            adjustedVertices[5] = heightRatio; // Top left y
            adjustedVertices[6] = 1.0f; // Top right x
            adjustedVertices[7] = heightRatio; // Top right y
        } else {
            // Texture is higher than view, adjust width
            float widthRatio = textureAspectRatio / viewAspectRatio;
            adjustedVertices[0] = -widthRatio; // Bottom left x
            adjustedVertices[1] = -1.0f; // Bottom left y
            adjustedVertices[2] = widthRatio; // Bottom right x
            adjustedVertices[3] = -1.0f; // Bottom right y
            adjustedVertices[4] = -widthRatio; // Top left x
            adjustedVertices[5] = 1.0f; // Top left y
            adjustedVertices[6] = widthRatio; // Top right x
            adjustedVertices[7] = 1.0f; // Top right y
        }

        // Update vertex buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(adjustedVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mVertexBuffer = bb.asFloatBuffer();
        mVertexBuffer.put(adjustedVertices);
        mVertexBuffer.position(0);

        Log.d("TAG",
                "Video aspect ratio updated: View=" + viewAspectRatio
                        + ", Texture=" + textureAspectRatio + ", Rotation=" + mRotation);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        synchronized (mLock) {
            if (mTextureNeedsUpdate && mTextureData != null) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mTextureWidth,
                        mTextureHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                        mTextureData);
                mTextureNeedsUpdate = false;
            }
        }

        // Use shader program
        GLES20.glUseProgram(mProgramHandle);

        // Set vertex coordinates
        GLES20.glVertexAttribPointer(
                mPositionHandle, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Set texture coordinates
        GLES20.glVertexAttribPointer(
                mTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mTexCoordBuffer);
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);

        // Set texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);
        GLES20.glUniform1i(mTextureSamplerHandle, 0);

        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordHandle);
    }

    // Update texture data and handle rotation
    public void updateTextureData(byte[] data, int width, int height, int sensorOrientation) {
        synchronized (mLock) {
            boolean sizeChanged = (mTextureWidth != width || mTextureHeight != height);

            if (mTextureData == null || sizeChanged) {
                mTextureData = ByteBuffer.allocateDirect(width * height * 4);
                mTextureData.order(ByteOrder.nativeOrder());
                mTextureWidth = width;
                mTextureHeight = height;
            }

            mTextureData.clear();
            mTextureData.put(data);
            mTextureData.position(0);
            mTextureNeedsUpdate = true;

            // When rotation changes, update texture coordinates
            int rotation = getRotationDegrees(sensorOrientation);
            boolean rotationChanged = (rotation != mRotation);

            if (rotationChanged) {
                mRotation = rotation;
                updateTextureCoordinates(mRotation);
            }

            // If size or rotation changes, update vertex coordinates to maintain aspect ratio
            if (sizeChanged || rotationChanged) {
                updateVertexCoordinates();
            }
        }
    }

    // Get appropriate rotation angle based on sensor orientation
    private int getRotationDegrees(int sensorOrientation) {
        // Determine rotation angle, considering device orientation and front/rear camera
        int deviceOrientation = deviceOri;

        // Front camera needs special handling
        boolean isFrontCamera = (mCamera2Helper != null)
                && (Camera2Helper.CAMERA_FACING == CameraCharacteristics.LENS_FACING_FRONT);

        int rotationDegrees;
        if (isFrontCamera) {
            rotationDegrees = (sensorOrientation + deviceOrientation) % 360;
        } else {
            rotationDegrees = (sensorOrientation - deviceOrientation + 360) % 360;
        }

        return rotationDegrees;
    }

    // Compile shader
    private int compileShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        if (compileStatus[0] == 0) {
            Log.e("TAG", "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }
}

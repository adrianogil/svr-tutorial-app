package com.example.svrtutorialapp;

import android.view.MotionEvent;

import com.samsungxr.ITouchEvents;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRMain;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRPicker;
import com.samsungxr.io.SXRCursorController;
import com.samsungxr.io.SXRGazeCursorController;
import com.samsungxr.io.SXRInputManager;
import com.samsungxr.mixedreality.IMixedReality;
import com.samsungxr.mixedreality.IMixedRealityEvents;
import com.samsungxr.mixedreality.IPlaneEvents;
import com.samsungxr.mixedreality.SXRMixedReality;
import com.samsungxr.mixedreality.SXRPlane;
import com.samsungxr.mixedreality.SXRTrackingState;
import com.samsungxr.utility.Log;

import java.util.EnumSet;

public class Main extends SXRMain {

    private final String TAG = Main.class.getSimpleName();
    private SXRContext mContext;
    private PointCloud mPointCloud;
    private SXRMixedReality mMixedReality;
    private SXRCursorController mCursorController = null;

    @Override
    public void onInit(SXRContext sxrContext) {

        mContext = sxrContext;

        mPointCloud = new PointCloud(sxrContext);

        mMixedReality = new SXRMixedReality(sxrContext.getMainScene());
        mMixedReality.getEventReceiver().addListener(planeEventsListener);
        mMixedReality.getEventReceiver().addListener(mixedRealityEventsListener);
        mMixedReality.getEventReceiver().addListener(mPointCloud);
        mMixedReality.resume();
    }

    @Override
    public SplashMode getSplashMode() {
        return SplashMode.NONE;
    }

    private IPlaneEvents planeEventsListener = new IPlaneEvents() {
        @Override
        public void onPlaneDetected(SXRPlane plane) {
            Log.d(TAG, "on plane detected");
            SXRNode planeNode = createPlaneNode();
            planeNode.attachComponent(plane);
            mContext.getMainScene().addNode(planeNode);

            //Stop to show the Points Cloud
            mPointCloud.disablePoints();
            mMixedReality.getEventReceiver().removeListener(mPointCloud);
        }

        @Override
        public void onPlaneStateChange(SXRPlane plane, SXRTrackingState trackingState) {

        }

        @Override
        public void onPlaneMerging(SXRPlane childPlane, SXRPlane parentPlane) {

        }

        @Override
        public void onPlaneGeometryChange(SXRPlane plane) {
            if (plane.getTrackingState() == SXRTrackingState.TRACKING) {
                SXRNode ownerObject = plane.getOwnerObject();
                if (ownerObject != null && ownerObject.getChildrenCount() > 0) {
                    SXRNode quad = ownerObject.getChildByIndex(0);
                    if (quad != null) {
                        quad.getTransform().setScale(
                                plane.getWidth() * 0.9f,
                                plane.getHeight() * 0.9f,
                                1f);
                    }
                }
            }
        }
    };

    private ITouchEvents mTouchEventsHandler = new ITouchEvents() {

        @Override
        public void onEnter(SXRNode sceneObj, SXRPicker.SXRPickedObject collision) {

        }

        @Override
        public void onExit(SXRNode sceneObj, SXRPicker.SXRPickedObject collision) {

        }

        @Override
        public void onTouchStart(SXRNode sceneObj, SXRPicker.SXRPickedObject collision) {

        }

        @Override
        public void onTouchEnd(SXRNode sceneObj, SXRPicker.SXRPickedObject collision) {
            Log.d(TAG, "onTouchEnd");
            final float[] hitPos = collision.hitLocation;
            Log.d(TAG, "position x: " + hitPos[0]+" y: " + hitPos[1] + " z: " + hitPos[2]);
        }

        @Override
        public void onInside(SXRNode sceneObj, SXRPicker.SXRPickedObject collision) {

        }

        @Override
        public void onMotionOutside(SXRPicker picker, MotionEvent motionEvent) {

        }
    };

    private IMixedRealityEvents mixedRealityEventsListener = new IMixedRealityEvents() {
        @Override
        public void onMixedRealityStart(IMixedReality mr) {
            mCursorController = null;
            SXRInputManager inputManager = mContext.getInputManager();
            final int cursorDepth = 5;
            final EnumSet<SXRPicker.EventOptions> eventOptions = EnumSet.of(
                    SXRPicker.EventOptions.SEND_PICK_EVENTS,
                    SXRPicker.EventOptions.SEND_TOUCH_EVENTS,
                    SXRPicker.EventOptions.SEND_TO_LISTENERS,
                    SXRPicker.EventOptions.SEND_TO_HIT_OBJECT);

            inputManager.selectController((newController, oldController) -> {
                if (mCursorController != null) {
                    mCursorController.removePickEventListener(mTouchEventsHandler);
                }
                newController.addPickEventListener(mTouchEventsHandler);
                newController.setCursorDepth(cursorDepth);
                newController.setCursorControl(SXRCursorController.CursorControl.CURSOR_CONSTANT_DEPTH);
                newController.getPicker().setPickClosest(false);
                newController.getPicker().setEventOptions(eventOptions);
                mCursorController = newController;
                if (newController instanceof SXRGazeCursorController) {
                    ((SXRGazeCursorController) newController).setTouchScreenDepth(mr.getScreenDepth());
                    // Don't show any cursor
                    newController.setCursor(null);
                }
            });
        }

        @Override
        public void onMixedRealityStop(IMixedReality mr) {
        }

        @Override
        public void onMixedRealityUpdate(IMixedReality mr) {
        }

        @Override
        public void onMixedRealityError(IMixedReality mr, String errmsg) {
        }
    };

    private SXRNode createPlaneNode() {
        Log.d(TAG, "create plane node");
        SXRMaterial mat = new SXRMaterial(mContext, SXRMaterial.SXRShaderType.Phong.ID);
        mat.setDiffuseColor(0, 1, 0, 0.5f);

        // Quad mesh to represent a plane
        SXRMesh mesh = SXRMesh.createQuad(mContext, "float3 a_position", 1, 1);

        // This object represents a plane in ARCore World
        SXRNode planeAR = new SXRNode(mContext, mesh, mat);
        planeAR.setName("Plane");
        planeAR.getRenderData().disableLight();
        planeAR.getRenderData().setAlphaBlend(true);
        // The plane should be rotated once the ARCore initial position is different from SXR
        planeAR.getTransform().setRotationByAxis(-90, 1, 0, 0);

        // This is the plane that is visualized
        SXRNode plane = new SXRNode(mContext);
        plane.addChildObject(planeAR);
        return plane;
    }

    @Override
    public boolean onBackPress() {
        mContext.getActivity().onBackPressed();
        return super.onBackPress();
    }
}

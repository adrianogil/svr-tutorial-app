package com.example.svrtutorialapp;

import android.opengl.GLES30;
import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRBoxCollider;
import com.samsungxr.SXRCameraRig;
import com.samsungxr.SXRContext;
import com.samsungxr.SXREventListeners;
import com.samsungxr.SXRMain;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRPerspectiveCamera;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRRenderData.SXRRenderingOrder;
import com.samsungxr.SXRShaderId;
import com.samsungxr.SXRTexture;
import com.samsungxr.SXRTextureParameters;
import com.samsungxr.io.SXRCursorController;
import com.samsungxr.io.SXRGazeCursorController;
import com.samsungxr.io.SXRInputManager;
import com.samsungxr.mixedreality.IMixedReality;
import com.samsungxr.mixedreality.IMixedRealityEvents;
import com.samsungxr.mixedreality.IPlaneEvents;
import com.samsungxr.mixedreality.SXRHitResult;
import com.samsungxr.mixedreality.SXRMixedReality;
import com.samsungxr.mixedreality.SXRPlane;
import com.samsungxr.mixedreality.SXRTrackingState;
import com.samsungxr.nodes.SXRSphereNode;
import com.samsungxr.nodes.SXRCubeNode;
import com.samsungxr.utility.Log;
import org.joml.Vector3f;

import org.joml.Vector3f;

import java.util.EnumSet;

public class Main extends SXRMain {
    private final String TAG = Main.class.getSimpleName();
    private SXRContext mContext;
    private PointCloud mPointCloud;
    private SXRMixedReality mMixedReality;
    private SXRCursorController mCursorController = null;
    private TouchHandler mTouchHandler;
    private boolean mIsMono = true;
    private Vector3f[] selectedPoints;
    private int currentTotalPoints;
    private final int TOTAL_SELECTED_POINTS = 6;

    @Override
    public void onInit(SXRContext sxrContext) {

        selectedPoints = new Vector3f[TOTAL_SELECTED_POINTS];
        currentTotalPoints = 0;

        mContext = sxrContext;

        mPointCloud = new PointCloud(sxrContext);
        mTouchHandler = new TouchHandler();

        mMixedReality = new SXRMixedReality(sxrContext.getMainScene());
        mMixedReality.getEventReceiver().addListener(planeEventsListener);
        mMixedReality.getEventReceiver().addListener(mixedRealityEventsListener);
        mMixedReality.getEventReceiver().addListener(mPointCloud);

        SXRCameraRig rig = mContext.getMainScene().getMainCameraRig();
        final SXRPerspectiveCamera centerCam = rig.getCenterCamera();
        final float aspect = centerCam.getAspectRatio();

        mIsMono = Math.abs(1.0f - aspect) > 0.0001f;

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

    private IMixedRealityEvents mixedRealityEventsListener = new IMixedRealityEvents() {
        @Override
        public void onMixedRealityStart(IMixedReality mr) {

            float screenDepth = mIsMono ?  mr.getScreenDepth() : 0;

            SXRInputManager inputManager = mContext.getInputManager();
            final int cursorDepth = 1;
            final EnumSet<SXRPicker.EventOptions> eventOptions = EnumSet.of(
                    SXRPicker.EventOptions.SEND_TOUCH_EVENTS,
                    SXRPicker.EventOptions.SEND_TO_LISTENERS,
                    SXRPicker.EventOptions.SEND_TO_HIT_OBJECT);

            inputManager.selectController((newController, oldController) -> {
                if (mCursorController != null) {
                    mCursorController.removePickEventListener(mTouchHandler);
                }
                newController.addPickEventListener(mTouchHandler);
                newController.setCursorDepth(cursorDepth);
                newController.setCursorControl(SXRCursorController.CursorControl.PROJECT_CURSOR_ON_SURFACE);
                newController.getPicker().setEventOptions(eventOptions);
                if ((screenDepth > 0) && (newController instanceof SXRGazeCursorController)) {
                    ((SXRGazeCursorController) newController).setTouchScreenDepth(screenDepth);
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

    private SXRNode createCustomMesh() {
        Log.d(TAG, "create custom mesh node");
        final float width = 2.0f;
        final float height = 2.0f;

        Vector3f meanVector = new Vector3f(0f, 0f, 0f);
        for (int i = 0; i < TOTAL_SELECTED_POINTS; i++) {
            meanVector.add(selectedPoints[i]);
        }
        meanVector.div(TOTAL_SELECTED_POINTS);

        final float[] vertices = new float[]{
                selectedPoints[0].x(), selectedPoints[0].y(), selectedPoints[0].z(),
                meanVector.x(), meanVector.y(), meanVector.z(),
                selectedPoints[1].x(), selectedPoints[1].y(), selectedPoints[1].z(),
        };

        SXRNode customMesh = new SXRNode(mContext);

        SXRMaterial mat = new SXRMaterial(mContext, SXRMaterial.SXRShaderType.Phong.ID);
        mat.setDiffuseColor(1.0f, 0.0f, 1.0f, 0.5f);
        final SXRRenderData renderData = new SXRRenderData(mContext);
        final SXRMesh mesh = new SXRMesh(mContext, "float3 a_position");
        mesh.setVertices(vertices);
        renderData.setMesh(mesh);
        renderData.setMaterial(mat);
        renderData.setDrawMode(GLES30.GL_TRIANGLE_FAN);

        customMesh.attachComponent(renderData);
        customMesh.setName("Custom Mesh");

        return customMesh;
    }

    private SXRNode createPositionSphereMesh(Vector3f position) {
        Log.d(TAG, "create plane node");
        SXRNode sphere = new SXRSphereNode(mContext, true, 0.01f);
        SXRRenderData rdata = sphere.getRenderData();
        SXRMaterial mtl = new SXRMaterial(mContext, SXRMaterial.SXRShaderType.Phong.ID);
        mtl.setDiffuseColor(1.0f, 0.0f, 1.0f, 0.5f);
        sphere.setName("balloon");
        rdata.setAlphaBlend(true);
        rdata.setMaterial(mtl);
        rdata.setRenderingOrder(SXRRenderingOrder.TRANSPARENT);
        sphere.getTransform().setPosition(
                position.x(),
                position.y(),
                position.z()
        );
        return sphere;
    }

    private SXRNode createCube(float[] pose) {
        Log.d(TAG, "create cube node");
        SXRCubeNode cube = new SXRCubeNode(mContext, true, new Vector3f(0.2f));


        SXRBoxCollider collider = new SXRBoxCollider(mContext);
        collider.setHalfExtents(0.05f,0.05f,0.05f);


        cube.setName("Cube");
        cube.attachCollider(collider);
        cube.getRenderData().setMaterial(createBuildingMaterial());
        cube.getRenderData().disableLight();
        cube.getRenderData().setAlphaBlend(false);
        cube.getTransform().setPosition(pose[12], pose[13] + 0.2f , pose[14]);

        // This is the plane that is visualized
        return cube;
    }

    private SXRMaterial createBuildingMaterial() {
        SXRMaterial mat = new SXRMaterial(mContext, new SXRShaderId(BuildingTilingShader.class));

        SXRTexture buildingTexture = mContext.getAssetLoader().loadTexture(new SXRAndroidResource(mContext,
                R.drawable.generic_bldng));

        SXRTextureParameters textureParameters = new SXRTextureParameters(mContext);
        textureParameters.setWrapSType(SXRTextureParameters.TextureWrapType.GL_REPEAT);
        textureParameters.setWrapTType(SXRTextureParameters.TextureWrapType.GL_REPEAT);

        buildingTexture.updateTextureParameters(textureParameters);

        mat.setTexture(BuildingTilingShader.TEXTURE_KEY, buildingTexture);
        mat.setVec3(BuildingTilingShader.SCALE_KEY, 1.f, 1.f, 1.f);

        return  mat;
    }

    @Override
    public boolean onBackPress() {
        mContext.getActivity().onBackPressed();
        return super.onBackPress();
    }

    public class TouchHandler extends SXREventListeners.TouchEvents {
        @Override
        public void onTouchEnd(SXRNode sceneObj, SXRPicker.SXRPickedObject pickInfo) {
            SXRNode.BoundingVolume bv = sceneObj.getBoundingVolume();

            if (pickInfo.hitDistance < bv.radius) {
                pickInfo.hitLocation[2] -= 1.5f * bv.radius;
            }

            SXRHitResult hit = mMixedReality.hitTest(pickInfo);

            //if (hit != null) {
            //    Log.d(TAG, "position x: " + hit.getPose()[12] + " y: " + hit.getPose()[13] + " z: " + hit.getPose()[14]);
            //    mContext.getMainScene().addNode(createCube(hit.getPose()));
            //}

            if (hit != null && currentTotalPoints < TOTAL_SELECTED_POINTS) {
                Log.d(TAG, "onTouchEnd - position x: " + hit.getPose()[12] + " y: " + hit.getPose()[13] + " z: " + hit.getPose()[14]);
                selectedPoints[currentTotalPoints] = new Vector3f(hit.getPose()[12], hit.getPose()[13], hit.getPose()[14]);

                SXRNode newSpherePointNode = createPositionSphereMesh(selectedPoints[currentTotalPoints]);
                mContext.getMainScene().addNode(newSpherePointNode);

                currentTotalPoints++;

                if (currentTotalPoints >= TOTAL_SELECTED_POINTS)
                {
                    Log.d(TAG, "onTouchEnd - generating custom mesh");
                    // Let's generate a custom mesh here
                    SXRNode customMeshNode = createCustomMesh();
                    mContext.getMainScene().addNode(customMeshNode);
                }
            }
        }
    };
}

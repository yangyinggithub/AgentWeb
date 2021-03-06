/*
 * Copyright © Yan Zhenjie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.just.library;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

import static android.provider.MediaStore.EXTRA_OUTPUT;
import static com.just.library.ActionActivity.Action.ACTION_CAMERA;
import static com.just.library.FileUpLoadChooserImpl.REQUEST_CODE;

/**
 * <p>
 * Created by cenxiaozhong on 2017.
 * </p>
 */
public final class ActionActivity extends Activity {

    public static final String KEY_ACTION = "KEY_ACTION";
    public static final String KEY_URI = "KEY_URI";
    private static RationaleListener mRationaleListener;
    private static PermissionListener mPermissionListener;
    private static FileDataListener mFileDataListener;
    private static final String TAG = ActionActivity.class.getSimpleName();


    static void start(Activity activity, Action action) {

        Intent mIntent = new Intent(activity, ActionActivity.class);
        mIntent.putExtra(KEY_ACTION, action);
        activity.startActivity(mIntent);

    }

    public static void setFileDataListener(FileDataListener fileDataListener) {
        mFileDataListener = fileDataListener;
    }

    public static void setPermissionListener(PermissionListener permissionListener) {
        mPermissionListener = permissionListener;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Action mAction = intent.getParcelableExtra(KEY_ACTION);
        if (mAction.action == Action.ACTION_PERMISSION) {
            permission(mAction);
        } else if (mAction.action == ACTION_CAMERA) {
            realOpenCamera();
        } else {
            fetchFile(mAction);
        }

    }

    private void fetchFile(Action action) {

        if (mFileDataListener == null)
            finish();

        openRealFileChooser();
    }

    private void openRealFileChooser() {

        try {
            if (mFileDataListener == null)
                return;

            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            this.startActivityForResult(Intent.createChooser(i,
                    "File Chooser"), REQUEST_CODE);
        } catch (Throwable throwable) {
            LogUtils.i(TAG, "找不到文件选择器");
            mFileDataListener.onFileDataResult(REQUEST_CODE, -1, null);
            mFileDataListener = null;
            finish();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


        LogUtils.i(TAG, "mFileDataListener:" + mFileDataListener);
        if (requestCode == REQUEST_CODE) {
//            if (mFileDataListener != null)
            mFileDataListener.onFileDataResult(requestCode, resultCode, mUri != null ? new Intent().putExtra(KEY_URI, mUri) : data);
            mFileDataListener = null;
            finish();
        }
    }

    private void permission(Action action) {
        String[] permissions = action.permissions;

        if (permissions == null) {
            mPermissionListener = null;
            mRationaleListener = null;
            finish();
            return;
        }

        if (mRationaleListener != null) {
            boolean rationale = false;
            for (String permission : permissions) {
                rationale = shouldShowRequestPermissionRationale(permission);
                if (rationale) break;
            }
            mRationaleListener.onRationaleResult(rationale);
            mRationaleListener = null;
            finish();
            return;
        }

        if (mPermissionListener != null)
            requestPermissions(permissions, 1);
    }

    private Uri mUri;

    private void realOpenCamera() {

        try {
            if (mFileDataListener == null)
                finish();
            File mFile=null;
            Intent intent = AgentWebUtils.getIntentCaptureCompat(this,mFile= AgentWebUtils.createFile());
            LogUtils.i(TAG, "listener:" + mFileDataListener+"  file:"+mFile.getAbsolutePath());
            // 指定开启系统相机的Action
            mUri = intent.getParcelableExtra(EXTRA_OUTPUT);
            this.startActivityForResult(intent, REQUEST_CODE);
        } catch (Throwable ignore) {
            LogUtils.i(TAG, "找不到系统相机");
            mFileDataListener.onFileDataResult(REQUEST_CODE, Activity.RESULT_CANCELED, null);
            mFileDataListener = null;
            if (LogUtils.isDebug())
                ignore.printStackTrace();
        }


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (mPermissionListener != null)
            mPermissionListener.onRequestPermissionsResult(permissions, grantResults);
        mPermissionListener = null;
        finish();
    }

    interface RationaleListener {
        void onRationaleResult(boolean showRationale);
    }

    interface PermissionListener {
        void onRequestPermissionsResult(@NonNull String[] permissions, @NonNull int[] grantResults);
    }

    interface FileDataListener {
        void onFileDataResult(int requestCode, int resultCode, Intent data);
    }

    public static final class Action implements Parcelable {

        public transient static final int ACTION_PERMISSION = 1;
        public transient static final int ACTION_FILE = 2;
        public transient static final int ACTION_CAMERA = 3;
        private String[] permissions;
        private int action;


        public Action() {

        }

        protected Action(Parcel in) {
            permissions = in.createStringArray();
            action = in.readInt();
        }

        public static final Creator<Action> CREATOR = new Creator<Action>() {
            @Override
            public Action createFromParcel(Parcel in) {
                return new Action(in);
            }

            @Override
            public Action[] newArray(int size) {
                return new Action[size];
            }
        };

        public String[] getPermissions() {
            return permissions;
        }

        public void setPermissions(String[] permissions) {
            this.permissions = permissions;
        }

        public int getAction() {
            return action;
        }

        public void setAction(int action) {
            this.action = action;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeStringArray(permissions);
            dest.writeInt(action);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

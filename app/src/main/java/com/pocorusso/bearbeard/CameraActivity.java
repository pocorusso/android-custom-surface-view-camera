package com.pocorusso.bearbeard;

import android.support.v4.app.Fragment;

public class CameraActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new CameraFragment();
    }
}

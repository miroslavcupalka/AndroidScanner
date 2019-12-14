package com.scanlibrary;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class ProgressDialogFragment extends DialogFragment {
    public static final String EXTRA_MESSAGE = "message";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        if (getArguments() != null && getArguments().containsKey(EXTRA_MESSAGE)) {
            dialog.setMessage(getArguments().getString(EXTRA_MESSAGE));
        }

        return dialog;
    }
}

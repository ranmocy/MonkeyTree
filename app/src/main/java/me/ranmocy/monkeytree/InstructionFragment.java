package me.ranmocy.monkeytree;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;


/**
 * Fragment contains the main layout.
 * <p>
 * <p>Activities containing this fragment MUST implement {@link OnActionSelected}.</p>
 */
public final class InstructionFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "InstructionFragment";

    public interface OnActionSelected {

        void onActionSelected(Action action);
    }

    public static InstructionFragment create() {
        return new InstructionFragment();
    }

    private OnActionSelected callback;
    private AlertDialog alertDialog;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_instruction, container, false);
        rootView.findViewById(R.id.btn_update_all).setOnClickListener(this);
        rootView.findViewById(R.id.btn_clear_all).setOnClickListener(this);
        rootView.findViewById(R.id.btn_learn_more).setOnClickListener(this);

        // job that depends on contact changes requires sdk 24
        rootView.findViewById(R.id.monitoring_container).setVisibility(View.VISIBLE);
        final SwitchCompat monitoringSwitch = rootView.findViewById(R.id.switch_monitoring);
        final SwitchCompat backgroundSwitch = rootView.findViewById(R.id.switch_background);

        boolean monitoringEnabled = SharedPreferencesUtil.getMonitoringEnabled(getContext());
        monitoringSwitch.setChecked(monitoringEnabled);
        backgroundSwitch.setEnabled(monitoringEnabled);
        MonkeyService.updateJob(getContext());

        boolean autoUpdateEnabled = SharedPreferencesUtil.getAutoUpdateEnabled(getContext());
        backgroundSwitch.setChecked(autoUpdateEnabled);

        monitoringSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            backgroundSwitch.setEnabled(isChecked);
            SharedPreferencesUtil.setKeyMonitoring(getContext(), isChecked);
            MonkeyService.updateJob(getContext());

        });
        backgroundSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                SharedPreferencesUtil.setAutoUpdateEnabled(getContext(), isChecked));


        return rootView;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        callback = (OnActionSelected) context;
    }

    @Override
    public void onClick(View v) {
        try {
            int id = v.getId();
            if (id == R.id.btn_update_all) {
                callback.onActionSelected(Action.UPDATE_ALL_DATA);
            } else if (id == R.id.btn_clear_all) {
                callback.onActionSelected(Action.CLEAR_ALL_DATA);
            } else if (id == R.id.btn_learn_more) {
                alertDialog = new AlertDialog.Builder(getContext())
                        .setMessage(R.string.main_description)
                        .show();
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Transliterator data is missing");
            Toast.makeText(getActivity(), R.string.toast_data_missing_message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog = null;
        }
    }
}

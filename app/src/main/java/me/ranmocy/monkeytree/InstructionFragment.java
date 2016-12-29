package me.ranmocy.monkeytree;


import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;


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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            rootView.findViewById(R.id.monitoring_container).setVisibility(View.VISIBLE);
            final SwitchCompat monitoringSwitch = (SwitchCompat) rootView.findViewById(R.id.switch_monitoring);
            final SwitchCompat backgroundSwitch = (SwitchCompat) rootView.findViewById(R.id.switch_background);

            boolean monitoringEnabled = SharedPreferencesUtil.getMonitoringEnabled(getContext());
            monitoringSwitch.setChecked(monitoringEnabled);
            backgroundSwitch.setEnabled(monitoringEnabled);
            MonkeyService.updateJob(getContext());

            boolean autoUpdateEnabled = SharedPreferencesUtil.getAutoUpdateEnabled(getContext());
            backgroundSwitch.setChecked(autoUpdateEnabled);

            monitoringSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    backgroundSwitch.setEnabled(isChecked);
                    SharedPreferencesUtil.setKeyMonitoring(getContext(), isChecked);
                    MonkeyService.updateJob(getContext());

                }
            });
            backgroundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SharedPreferencesUtil.setAutoUpdateEnabled(getContext(), isChecked);
                }
            });
        }

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        callback = (OnActionSelected) context;
    }

    @Override
    public void onClick(View v) {
        try {
            switch (v.getId()) {
                case R.id.btn_update_all:
                    callback.onActionSelected(Action.UPDATE_ALL_DATA);
                    break;
                case R.id.btn_clear_all:
                    callback.onActionSelected(Action.CLEAR_ALL_DATA);
                    break;
                case R.id.btn_learn_more:
                    alertDialog = new AlertDialog.Builder(getContext())
                            .setMessage(R.string.main_description)
                            .show();
                    break;
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

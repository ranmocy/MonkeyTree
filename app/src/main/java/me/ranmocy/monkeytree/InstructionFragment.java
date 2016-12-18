package me.ranmocy.monkeytree;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_instruction, container, false);
        rootView.findViewById(R.id.btn_update_all).setOnClickListener(this);
        rootView.findViewById(R.id.btn_clear_all).setOnClickListener(this);
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
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Transliterator data is missing");
            Toast.makeText(getActivity(), R.string.toast_data_missing_message, Toast.LENGTH_SHORT).show();
        }
    }
}

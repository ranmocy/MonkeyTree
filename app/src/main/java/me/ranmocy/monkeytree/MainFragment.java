package me.ranmocy.monkeytree;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


/**
 * Fragment contains the main layout.
 */
public class MainFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "MainFragment";

    public static MainFragment create() {
        return new MainFragment();
    }

    private ContactFixer contactFixer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contactFixer = new ContactFixer(getContext());
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        rootView.findViewById(R.id.btn_fix_latin).setOnClickListener(this);
        rootView.findViewById(R.id.btn_fix_chinese).setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onClick(View v) {
        try {
            switch (v.getId()) {
                case R.id.btn_fix_latin:
                    contactFixer.fixLatinContacts();
                    break;
                case R.id.btn_fix_chinese:
                    contactFixer.fixChineseContacts();
                    break;
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Transliterator data is missing");
            Toast.makeText(getActivity(), R.string.toast_data_missing_message, Toast.LENGTH_SHORT).show();
        }
    }
}

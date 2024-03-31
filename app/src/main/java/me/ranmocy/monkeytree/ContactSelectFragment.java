package me.ranmocy.monkeytree;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * A fragment representing a list of contacts.
 * <p>
 * <p>Activities containing this fragment MUST implement {@link OnContactsConfirmed}.</p>
 */
public final class ContactSelectFragment extends Fragment {

    private static final String ARG_ACTION = "arg_action";
    private static final String ARG_CONTACTS = "arg_contacts";

    interface OnContactsConfirmed {
        void onContactsConfirmed(@Nullable Action action, @Nullable Set<ContactLite> contacts);
    }

    public static ContactSelectFragment create(Action action, Set<ContactLite> contacts) {
        ArrayList<ContactLite> contactList = new ArrayList<>(contacts);
        Collections.sort(contactList);

        ContactSelectFragment fragment = new ContactSelectFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ACTION, action);
        args.putParcelableArrayList(ARG_CONTACTS, contactList);
        fragment.setArguments(args);
        return fragment;
    }

    private Action action;
    private ContactSelectRecyclerViewAdapter adapter;
    private OnContactsConfirmed callback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            action = (Action) getArguments().getSerializable(ARG_ACTION);
            ArrayList<ContactLite> contacts = getArguments().getParcelableArrayList(ARG_CONTACTS);
            adapter = new ContactSelectRecyclerViewAdapter(contacts);
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contact_select_list, container, false);
        Button confirmBtn = rootView.findViewById(R.id.btn_confirm);

        if (adapter.getItemCount() > 0) {
            RecyclerView recyclerView = rootView.findViewById(R.id.fragment_contact_list);
            recyclerView.setAdapter(adapter);

            confirmBtn.setOnClickListener(view -> callback.onContactsConfirmed(action, adapter.getSelectedContacts()));
        } else {
            TextView descriptionView = rootView.findViewById(R.id.contact_list_description);
            descriptionView.setText(R.string.contact_list_empty);

            confirmBtn.setText(R.string.btn_done);
            confirmBtn.setOnClickListener(view -> callback.onContactsConfirmed(null, null));
        }
        return rootView;
    }


    @Override
    public void onAttach(@Nonnull Context context) {
        super.onAttach(context);
        callback = (OnContactsConfirmed) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }
}

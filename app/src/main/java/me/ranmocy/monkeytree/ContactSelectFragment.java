package me.ranmocy.monkeytree;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Set;

/**
 * A fragment representing a list of contacts.
 * <p>
 * <p>Activities containing this fragment MUST implement {@link OnContactsConfirmed}.</p>
 */
public final class ContactSelectFragment extends Fragment {

    private static final String ARG_ACTION = "arg_action";
    private static final String ARG_CONTACTS = "arg_contacts";

    public interface OnContactsConfirmed {
        void onContactsConfirmed(Action action, Set<ContactLite> contacts);
    }

    public static ContactSelectFragment create(Action action, Set<ContactLite> contacts) {
        ContactSelectFragment fragment = new ContactSelectFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ACTION, action);
        args.putParcelableArrayList(ARG_CONTACTS, new ArrayList<>(contacts));
        fragment.setArguments(args);
        return fragment;
    }

    private Action action;
    private ArrayList<ContactLite> contacts = new ArrayList<>();
    private ContactSelectRecyclerViewAdapter adapter;
    private OnContactsConfirmed callback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            action = (Action) getArguments().getSerializable(ARG_ACTION);
            contacts = getArguments().getParcelableArrayList(ARG_CONTACTS);
            adapter = new ContactSelectRecyclerViewAdapter(contacts);
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contact_select_list, container, false);

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.fragment_contact_list);
        recyclerView.setAdapter(adapter);

        rootView.findViewById(R.id.btn_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.onContactsConfirmed(action, adapter.getSelectedContacts());
            }
        });
        return rootView;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        callback = (OnContactsConfirmed) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }
}

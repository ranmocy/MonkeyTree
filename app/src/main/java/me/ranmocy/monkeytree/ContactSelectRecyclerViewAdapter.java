package me.ranmocy.monkeytree;

import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A {@link RecyclerView.Adapter} that can display a list of {@link ContactLite}.
 */
final class ContactSelectRecyclerViewAdapter
        extends RecyclerView.Adapter<ContactSelectRecyclerViewAdapter.ViewHolder> {

    private final ArrayList<ContactLite> contacts;
    private final Map<ContactLite, Boolean> selected;

    ContactSelectRecyclerViewAdapter(ArrayList<ContactLite> contacts) {
        this.contacts = contacts;
        this.selected = new HashMap<>(contacts.size());
        for (ContactLite contact : contacts) {
            this.selected.put(contact, true);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_contact_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.contact = contacts.get(position);
        holder.checkBox.setChecked(selected.get(holder.contact));
        holder.givenNameView.setText(holder.contact.givenName);
        holder.middleNameView.setText(holder.contact.middleName);
        holder.familyNameView.setText(holder.contact.familyName);
        holder.phoneticGivenNameView.setText(holder.contact.phoneticGivenName);
        holder.phoneticMiddleNameView.setText(holder.contact.phoneticMiddleName);
        holder.phoneticFamilyNameView.setText(holder.contact.phoneticFamilyName);

        if (ContactsContract.PhoneticNameStyle.PINYIN == holder.contact.phoneticNameStyle) {
            // Swap first name and family name order
            holder.familyNameView.setText(holder.contact.givenName);
            holder.givenNameView.setText(holder.contact.familyName);
            holder.phoneticFamilyNameView.setText(holder.contact.phoneticGivenName);
            holder.phoneticGivenNameView.setText(holder.contact.phoneticFamilyName);
        }

        holder.view.setOnClickListener(v -> {
            holder.checkBox.setSelected(!holder.checkBox.isSelected());
            selected.put(holder.contact, holder.checkBox.isSelected());
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    Set<ContactLite> getSelectedContacts() {
        HashSet<ContactLite> selectedContacts = new HashSet<>();
        for (Map.Entry<ContactLite, Boolean> entry : selected.entrySet()) {
            if (entry.getValue()) {
                selectedContacts.add(entry.getKey());
            }
        }
        return selectedContacts;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View view;
        private final AppCompatCheckBox checkBox;
        private final TextView givenNameView;
        private final TextView middleNameView;
        private final TextView familyNameView;
        private final TextView phoneticGivenNameView;
        private final TextView phoneticMiddleNameView;
        private final TextView phoneticFamilyNameView;
        private ContactLite contact;

        private ViewHolder(View view) {
            super(view);
            this.view = view;
            this.checkBox = view.findViewById(R.id.contact_checkbox);
            this.givenNameView = view.findViewById(R.id.contact_given_name);
            this.middleNameView = view.findViewById(R.id.contact_middle_name);
            this.familyNameView = view.findViewById(R.id.contact_family_name);
            this.phoneticGivenNameView = view.findViewById(R.id.contact_phonetic_given_name);
            this.phoneticMiddleNameView = view.findViewById(R.id.contact_phonetic_middle_name);
            this.phoneticFamilyNameView = view.findViewById(R.id.contact_phonetic_family_name);
        }

        @NonNull
        @Override
        public String toString() {
            return super.toString() + " '" + middleNameView.getText() + "'";
        }
    }
}

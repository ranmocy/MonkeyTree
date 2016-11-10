package me.ranmocy.monkeytree;

/**
 * Lite version of a contact name.
 */
final class ContactLite {

    final int dataId;
    final String displayName;
    final String givenName;
    final String middleName;
    final String familyName;

    ContactLite(int dataId,
                String displayName,
                String givenName,
                String middleName,
                String familyName) {
        this.dataId = dataId;
        this.displayName = displayName;
        this.givenName = givenName;
        this.middleName = middleName;
        this.familyName = familyName;
    }
}

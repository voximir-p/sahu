package org.voximir.sahu;

public enum FireMode {

    FULL_AUTO(0, "Full Auto"),
    SINGLE(1, "Single");

    private final int id;
    private final String displayName;

    FireMode(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public FireMode next() {
        return this == FULL_AUTO ? SINGLE : FULL_AUTO;
    }

    public static FireMode fromId(int id) {
        return switch (id) {
            case 1 -> SINGLE;
            default -> FULL_AUTO;
        };
    }
}

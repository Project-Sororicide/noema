package dev.pupwired.noema.message;

public enum NoemaMsgColor {
    ERROR("#fb2e01"),
    PASS("#6fcb9f"),
    IMPLICIT("#666547"),
    WARNING("#ffe28a"),
    INFO("#fffeb3");

    private final String string;
    NoemaMsgColor(String string)
    {
        this.string = string;
    }

    @Override
    public String toString() {
        return string;
    }
}

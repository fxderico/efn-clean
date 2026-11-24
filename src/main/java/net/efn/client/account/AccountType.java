package net.efn.client.account;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum AccountType {
    CRACKED("Cracked"),
    SESSION("Session"),
    MICROSOFT("Microsoft");

    private final String label;

    private AccountType(String label2) {
        this.label = label2;
    }

    public String label() {
        return this.label;
    }
}

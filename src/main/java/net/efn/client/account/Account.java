package net.efn.client.account;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Account {

    private final String id;

    private final AccountType type;

    private final String name;

    private final String uuid;

    private final String secret;

    Account(String id2, AccountType type2, String name2, String uuid2, String secret2) {
        this.id = id2;
        this.type = type2;
        this.name = name2;
        this.uuid = uuid2;
        this.secret = secret2;
    }

    public AccountType type() {
        return this.type;
    }

    public String name() {
        return this.name;
    }

    public String uuid() {
        return this.uuid;
    }

    public String id() {
        return this.id;
    }

    String secret() {
        return this.secret;
    }

    Account withLogin(String string, String string2, String string3) {
        return new Account(this.id, this.type, string, string2, string3);
    }

    @Override
    public boolean equals(Object object) {
        return !(object instanceof Account account) ? false : account.id.equals(this.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return this.type.label() + " " + this.name;
    }
}

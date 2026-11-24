package net.efn.client.account;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.concurrent.CompletionException;

@Environment(EnvType.CLIENT)
final class AccountException extends CompletionException {

    AccountException(String string) {
        super(string);
    }

    AccountException(String string, Throwable throwable) {
        super(string, throwable);
    }
}

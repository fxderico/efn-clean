package net.efn.client.module.impl.donutmacros;

import net.efn.client.util.CompletionChannel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.List;

@Environment(EnvType.CLIENT)
final class CompletionRequests {

    private CompletionChannel.Form form = CompletionChannel.Form.BOTH;

    CompletionRequests() {}

    CompletionChannel.Form form() {
        return this.form;
    }

    void form(CompletionChannel.Form form2) {
        this.form = form2;
    }

    int fire(String string) {
        return this.fire(string, this.form);
    }

    int fire(String string, CompletionChannel.Form form2) {
        return CompletionChannel.INSTANCE.fire(this, string, form2);
    }

    List<String> poll(int i) {
        return CompletionChannel.INSTANCE.poll(i);
    }

    void forget(int i) {
        CompletionChannel.INSTANCE.forget(i);
    }

    void prune(long l) {
        CompletionChannel.INSTANCE.prune(l);
    }

    void clear() {
        CompletionChannel.INSTANCE.clear(this);
        this.form = CompletionChannel.Form.BOTH;
    }
}

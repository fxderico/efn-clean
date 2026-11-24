package net.efn.client.module.impl.donutmacros;

import com.example.client.mixin.AccessorOrderDialogScreen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.dialog.DialogScreen;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.action.DialogAction.ValueGetter;
import net.minecraft.dialog.body.DialogBody;
import net.minecraft.dialog.body.ItemDialogBody;
import net.minecraft.dialog.body.PlainMessageDialogBody;
import net.minecraft.dialog.input.BooleanInputControl;
import net.minecraft.dialog.input.TextInputControl;
import net.minecraft.dialog.type.ColumnsDialog;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.dialog.type.DialogInput;
import net.minecraft.dialog.type.MultiActionDialog;
import net.minecraft.dialog.type.SimpleDialog;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.ObjectTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
final class Dialogs {

    private Dialogs() {}

    static Dialogs.View current(MinecraftClient client) {
        if (!(client.currentScreen instanceof DialogScreen dialogScreen)) {
            return null;
        } else {
            Dialog dialog2 = ((AccessorOrderDialogScreen) dialogScreen).efn$dialog();
            if (dialog2 == null) {
                return null;
            } else {
                ArrayList list = new ArrayList();
                if (dialog2 instanceof MultiActionDialog multiActionDialog) {
                    list.addAll(multiActionDialog.actions());
                }
                if (dialog2 instanceof SimpleDialog simpleDialog) {
                    list.addAll(simpleDialog.getButtons());
                }
                if (dialog2 instanceof ColumnsDialog columnsDialog) {
                    columnsDialog.exitAction().ifPresent(list::add);
                }
                ArrayList list2 = new ArrayList(list.size());
                for (DialogActionButtonData dialogActionButtonData :
                        (Iterable<DialogActionButtonData>) list) {
                    Text text = dialogActionButtonData.data().label();
                    String visibleText = visibleText(text);
                    list2.add(
                            new Dialogs.Button(
                                    dialogActionButtonData,
                                    ServerText.normalize(visibleText),
                                    ServerText.key(visibleText),
                                    text));
                }
                ArrayList list3 = new ArrayList();
                ArrayList list4 = new ArrayList();
                for (DialogBody dialogBody : dialog2.common().body()) {
                    if (dialogBody instanceof PlainMessageDialogBody plainMessageDialogBody2) {
                        list3.add(
                                ServerText.normalize(
                                        visibleText(plainMessageDialogBody2.contents())));
                    }
                    if (dialogBody instanceof ItemDialogBody itemDialogBody) {
                        list4.add(itemDialogBody.item());
                        itemDialogBody
                                .description()
                                .ifPresent(
                                        plainMessageDialogBody ->
                                                list3.add(
                                                        ServerText.normalize(
                                                                visibleText(
                                                                        plainMessageDialogBody
                                                                                .contents()))));
                    }
                }
                ArrayList list5 = new ArrayList();
                for (DialogInput input : dialog2.common().inputs()) {
                    if (input.control() instanceof TextInputControl) {
                        list5.add(input.key());
                    }
                }
                return new Dialogs.View(
                        dialog2,
                        ServerText.normalize(visibleText(dialog2.common().title())),
                        list2,
                        list3,
                        list5,
                        list4);
            }
        }
    }

    static String visibleText(Text text) {
        StringBuilder sb = new StringBuilder();
        appendVisible(text, sb);
        return sb.toString();
    }

    private static void appendVisible(Text text, StringBuilder sb) {
        if (!(text.getContent() instanceof ObjectTextContent)) {
            text.getContent()
                    .visit(
                            value -> {
                                sb.append(value);
                                return Optional.empty();
                            });
        }
        for (Text sibling : text.getSiblings()) {
            appendVisible(sibling, sb);
        }
    }

    static boolean press(
            MinecraftClient client,
            Dialogs.View view,
            Dialogs.Button button,
            Map<String, String> map) {
        if (!(client.currentScreen instanceof DialogScreen dialogScreen)) {
            return false;
        } else if (((AccessorOrderDialogScreen) dialogScreen).efn$dialog() != view.dialog()) {
            return false;
        } else {
            HashMap map2 = new HashMap();
            for (DialogInput input : view.dialog().common().inputs()) {
                if (input.control() instanceof TextInputControl textInputControl) {
                    map2.put(
                            input.key(),
                            ValueGetter.of(
                                    map.getOrDefault(input.key(), textInputControl.initial())));
                } else if (input.control() instanceof BooleanInputControl booleanInputControl) {
                    map2.put(input.key(), booleanValue(booleanInputControl));
                }
            }
            Optional optional =
                    button.raw()
                            .action()
                            .flatMap(dialogAction -> dialogAction.createClickEvent(map2));
            dialogScreen.runAction(optional);
            return true;
        }
    }

    static void close(MinecraftClient client) {
        if (client.currentScreen instanceof DialogScreen dialogScreen) {
            dialogScreen.close();
        }
    }

    private static ValueGetter booleanValue(BooleanInputControl booleanInputControl) {
        final boolean initial = booleanInputControl.initial();
        return new ValueGetter() {

            @Override
            public String get() {
                return initial ? booleanInputControl.onTrue() : booleanInputControl.onFalse();
            }

            @Override
            public NbtElement getAsNbt() {
                return NbtByte.of(initial);
            }
        };
    }

    private static boolean isGreen(int i) {
        int j = i >> 16 & 0xFF;
        int k = i >> 8 & 0xFF;
        int m = i & 0xFF;
        return k >= 192 && j < 144 && m < 144;
    }

    @Environment(EnvType.CLIENT)
    record Button(DialogActionButtonData raw, String label, String key, Text component) {

        boolean green() {
            boolean[] booleans = new boolean[] {false};
            this.component.visit(
                    (style, value) -> {
                        TextColor color = style.getColor();
                        if (!value.isBlank() && color != null && Dialogs.isGreen(color.getRgb())) {
                            booleans[0] = true;
                        }
                        return Optional.empty();
                    },
                    Style.EMPTY);
            return booleans[0];
        }
    }

    @Environment(EnvType.CLIENT)
    record View(
            Dialog dialog,
            String title,
            List<Dialogs.Button> buttons,
            List<String> body,
            List<String> textInputs,
            List<ItemStack> items) {

        Dialogs.Button find(Predicate<Dialogs.Button> predicate) {
            for (Dialogs.Button button : this.buttons) {
                if (predicate.test(button)) {
                    return button;
                }
            }
            return null;
        }

        Dialogs.Button labelled(String... string) {
            for (String string2 : string) {
                String key2 = ServerText.key(string2);
                Dialogs.Button button2 = this.find(button -> button.key().equals(key2));
                if (button2 != null) {
                    return button2;
                }
            }
            for (String string3 : string) {
                String key3 = ServerText.key(string3);
                Dialogs.Button button3 = this.find(button -> button.key().contains(key3));
                if (button3 != null) {
                    return button3;
                }
            }
            return null;
        }

        boolean titled(String string) {
            return this.title.contains(string);
        }

        String describe() {
            ArrayList list = new ArrayList();
            for (Dialogs.Button button : this.buttons) {
                if (!button.label().isBlank()) {
                    list.add(button.label());
                }
                if (list.size() >= 12) {
                    break;
                }
            }
            return "'"
                    + this.title
                    + "' buttons: "
                    + (list.isEmpty() ? "(none)" : String.join(", ", list));
        }
    }
}

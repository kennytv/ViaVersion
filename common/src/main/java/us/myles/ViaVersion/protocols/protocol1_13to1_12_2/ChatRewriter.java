package us.myles.ViaVersion.protocols.protocol1_13to1_12_2;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import us.myles.ViaVersion.api.rewriters.ComponentRewriter;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.ComponentRewriter1_13;
import us.myles.ViaVersion.util.ChatColorUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatRewriter {
    private static final Pattern URL = Pattern.compile("^(?:(https?)://)?([-\\w_.]{2,}\\.[a-z]{2,4})(/\\S*)?$");
    private static final ComponentRewriter COMPONENT_REWRITER = new ComponentRewriter1_13();

    public static JsonElement fromLegacyText(String message, char defaultColor) {
        JsonArray components = new JsonArray();
        StringBuilder builder = new StringBuilder();
        TextComponent component = new TextComponent();
        Matcher matcher = URL.matcher(message);
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == ChatColorUtil.COLOR_CHAR) {
                if (++i >= message.length()) {
                    break;
                }
                c = message.charAt(i);
                if (c >= 'A' && c <= 'Z') {
                    c += 32;
                }
                if (!ChatColorUtil.isColorCode(c)) {
                    continue;
                }
                if (builder.length() > 0) {
                    TextComponent old = component;
                    component = new TextComponent(old);
                    old.setText(builder.toString());
                    builder = new StringBuilder();
                    components.add(old);
                }

                switch (c) {
                    case 'k':
                        // magic
                        break;
                    case 'l':
                        // bold
                        break;
                    case 'm':
                        // strikethrough
                        break;
                    case 'n':
                        // underlined
                        break;
                    case 'o':
                        // italic
                        break;
                    case 'r':
                        c = defaultColor;
                        // setcolor
                        break;
                    default:
                        // set color
                        break;
                }
                continue;
            }
            int pos = message.indexOf(' ', i);
            if (pos == -1) {
                pos = message.length();
            }
            if (matcher.region(i, pos).find()) { //Web link handling
                if (builder.length() > 0) {
                    TextComponent old = component;
                    component = new TextComponent(old);
                    old.setText(builder.toString());
                    builder = new StringBuilder();
                    components.add(old);
                }

                TextComponent old = component;
                component = new TextComponent(old);
                String urlString = message.substring(i, pos);
                component.setText(urlString);
                component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                        urlString.startsWith("http") ? urlString : "http://" + urlString));
                components.add(component);
                i += pos - i - 1;
                component = old;
                continue;
            }
            builder.append(c);
        }

        component.setText(builder.toString());
        components.add(component);

        final String serializedComponents = ComponentSerializer.toString(components.toArray(EMPTY_COMPONENTS));
        return GsonUtil.getJsonParser().parse(serializedComponents);
        return null;
    }

    public static JsonElement legacyTextToJson(String legacyText) {
        return fromLegacyText(legacyText, 'f');
    }

    public static String jsonTextToLegacy(String value) {
        //return TextComponent.toLegacyText(ComponentSerializer.parse(value));
        return null;
    }

    public static void processTranslate(JsonElement value) {
        COMPONENT_REWRITER.processText(value);
    }
}

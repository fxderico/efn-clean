package net.efn.client.easyplace.schematic;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public record SchematicBlockState(
        BlockState state, Identifier blockId, Map<String, String> properties) {

    public static SchematicBlockState of(BlockState state2) {
        LinkedHashMap map = new LinkedHashMap();
        state2.getEntries()
                .forEach(
                        (property, comparable) ->
                                map.put(property.getName(), comparable.toString()));
        return new SchematicBlockState(
                state2,
                Registries.BLOCK.getId(state2.getBlock()),
                Collections.unmodifiableMap(map));
    }

    public String debugString() {
        if (this.properties.isEmpty()) {
            return this.blockId.toString();
        } else {
            StringBuilder sb = new StringBuilder(this.blockId.toString()).append('[');
            this.properties.forEach(
                    (value, value2) -> sb.append(value).append('=').append(value2).append(','));
            sb.setCharAt(sb.length() - 1, ']');
            return sb.toString();
        }
    }
}

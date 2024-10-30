/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2016-2024 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viaversion.protocols.v1_21_2to1_21_4.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_2;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_21_2;
import com.viaversion.viaversion.api.type.types.version.Types1_21_4;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundConfigurationPackets1_21;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.Protocol1_21_2To1_21_4;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2;
import com.viaversion.viaversion.rewriter.EntityRewriter;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;
import com.viaversion.viaversion.util.Key;

public final class EntityPacketRewriter1_21_4 extends EntityRewriter<ClientboundPacket1_21_2, Protocol1_21_2To1_21_4> {

    public EntityPacketRewriter1_21_4(final Protocol1_21_2To1_21_4 protocol) {
        super(protocol);
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_19(ClientboundPackets1_21_2.ADD_ENTITY, EntityTypes1_21_2.FALLING_BLOCK);
        registerSetEntityData(ClientboundPackets1_21_2.SET_ENTITY_DATA, Types1_21_2.ENTITY_DATA_LIST, Types1_21_4.ENTITY_DATA_LIST); // Specify old and new entity data list if changed
        registerRemoveEntities(ClientboundPackets1_21_2.REMOVE_ENTITIES);

        final RegistryDataRewriter registryDataRewriter = new RegistryDataRewriter(protocol) {
            @Override
            public RegistryEntry[] handle(final UserConnection connection, final String key, final RegistryEntry[] entries) {
                if (Key.stripMinecraftNamespace(key).equals("worldgen/biome")) {
                    for (final RegistryEntry entry : entries) {
                        if (entry.tag() == null) {
                            continue;
                        }

                        final CompoundTag effectsTag = ((CompoundTag) entry.tag()).getCompoundTag("effects");
                        final CompoundTag musicTag = effectsTag.getCompoundTag("music");
                        if (musicTag == null) {
                            continue;
                        }

                        // Wrap music
                        final ListTag<CompoundTag> weightedMusicTags = new ListTag<>(CompoundTag.class);
                        final CompoundTag weightedMusicTag = new CompoundTag();
                        weightedMusicTag.put("data", musicTag);
                        weightedMusicTag.putInt("weight", 1);
                        weightedMusicTags.add(weightedMusicTag);
                        effectsTag.put("music", weightedMusicTags);
                    }
                }

                return super.handle(connection, key, entries);
            }
        };
        protocol.registerClientbound(ClientboundConfigurationPackets1_21.REGISTRY_DATA, registryDataRewriter::handle);

        protocol.registerClientbound(ClientboundPackets1_21_2.LOGIN, wrapper -> {
            final int entityId = wrapper.passthrough(Types.INT); // Entity id
            wrapper.passthrough(Types.BOOLEAN); // Hardcore
            wrapper.passthrough(Types.STRING_ARRAY); // World List
            wrapper.passthrough(Types.VAR_INT); // Max players
            wrapper.passthrough(Types.VAR_INT); // View distance
            wrapper.passthrough(Types.VAR_INT); // Simulation distance
            wrapper.passthrough(Types.BOOLEAN); // Reduced debug info
            wrapper.passthrough(Types.BOOLEAN); // Show death screen
            wrapper.passthrough(Types.BOOLEAN); // Limited crafting

            final int dimensionId = wrapper.passthrough(Types.VAR_INT);
            final String world = wrapper.passthrough(Types.STRING);
            trackWorldDataByKey1_20_5(wrapper.user(), dimensionId, world);

            trackPlayer(wrapper.user(), entityId);
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.RESPAWN, wrapper -> {
            final int dimensionId = wrapper.passthrough(Types.VAR_INT);
            final String world = wrapper.passthrough(Types.STRING);
            trackWorldDataByKey1_20_5(wrapper.user(), dimensionId, world); // Tracks world height and name for chunk data and entity (un)tracking
        });
    }

    @Override
    protected void registerRewrites() {
        filter().mapDataType(Types1_21_4.ENTITY_DATA_TYPES::byId);

        registerEntityDataTypeHandler(
            Types1_21_4.ENTITY_DATA_TYPES.itemType,
            Types1_21_4.ENTITY_DATA_TYPES.blockStateType,
            Types1_21_4.ENTITY_DATA_TYPES.optionalBlockStateType,
            Types1_21_4.ENTITY_DATA_TYPES.particleType,
            Types1_21_4.ENTITY_DATA_TYPES.particlesType,
            Types1_21_4.ENTITY_DATA_TYPES.componentType,
            Types1_21_4.ENTITY_DATA_TYPES.optionalComponentType
        );
        registerBlockStateHandler(EntityTypes1_21_2.ABSTRACT_MINECART, 11);

        filter().type(EntityTypes1_21_2.CREAKING_TRANSIENT).addIndex(18); // Is tearing down
        filter().type(EntityTypes1_21_2.SALMON).index(17).handler((data, event) -> {
            final String type = event.value();
            final int typeId = switch (type) {
                case "small" -> 0;
                case "large" -> 2;
                default -> 1; // medium
            };
            event.setTypeAndValue(Types1_21_4.ENTITY_DATA_TYPES.varIntType, typeId);
        });
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_21_2.getTypeFromId(type);
    }
}
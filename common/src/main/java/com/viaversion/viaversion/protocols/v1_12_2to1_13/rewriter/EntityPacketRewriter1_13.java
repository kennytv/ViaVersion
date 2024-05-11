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
package com.viaversion.viaversion.protocols.v1_12_2to1_13.rewriter;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_12;
import com.viaversion.viaversion.api.type.types.version.Types1_13;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.Protocol1_12_2To1_13;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.metadata.MetadataRewriter1_13To1_12_2;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1;

public class EntityPacketRewriter1_13 {

    public static void register(Protocol1_12_2To1_13 protocol) {
        MetadataRewriter1_13To1_12_2 metadataRewriter = protocol.get(MetadataRewriter1_13To1_12_2.class);

        protocol.registerClientbound(ClientboundPackets1_12_1.ADD_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity id
                map(Types.UUID); // 1 - UUID
                map(Types.BYTE); // 2 - Type
                map(Types.DOUBLE); // 3 - X
                map(Types.DOUBLE); // 4 - Y
                map(Types.DOUBLE); // 5 - Z
                map(Types.BYTE); // 6 - Pitch
                map(Types.BYTE); // 7 - Yaw
                map(Types.INT); // 8 - Data

                // Track Entity
                handler(wrapper -> {
                    int entityId = wrapper.get(Types.VAR_INT, 0);
                    byte type = wrapper.get(Types.BYTE, 0);
                    EntityTypes1_13.EntityType entType = EntityTypes1_13.getTypeFromId(type, true);
                    if (entType == null) return;

                    // Register Type ID
                    wrapper.user().getEntityTracker(Protocol1_12_2To1_13.class).addEntity(entityId, entType);

                    if (entType.is(EntityTypes1_13.EntityType.FALLING_BLOCK)) {
                        int oldId = wrapper.get(Types.INT, 0);
                        int combined = (((oldId & 4095) << 4) | (oldId >> 12 & 15));
                        wrapper.set(Types.INT, 0, WorldPacketRewriter1_13.toNewId(combined));
                    }

                    // Fix ItemFrame hitbox
                    if (entType.is(EntityTypes1_13.EntityType.ITEM_FRAME)) {
                        int data = wrapper.get(Types.INT, 0);
                        switch (data) {
                            case 0 -> data = 3; // South
                            case 1 -> data = 4; // West
                            // North is the same
                            case 3 -> data = 5; // East
                        }

                        wrapper.set(Types.INT, 0, data);
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_12_1.ADD_MOB, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity ID
                map(Types.UUID); // 1 - Entity UUID
                map(Types.VAR_INT); // 2 - Entity Type
                map(Types.DOUBLE); // 3 - X
                map(Types.DOUBLE); // 4 - Y
                map(Types.DOUBLE); // 5 - Z
                map(Types.BYTE); // 6 - Yaw
                map(Types.BYTE); // 7 - Pitch
                map(Types.BYTE); // 8 - Head Pitch
                map(Types.SHORT); // 9 - Velocity X
                map(Types.SHORT); // 10 - Velocity Y
                map(Types.SHORT); // 11 - Velocity Z
                map(Types1_12.METADATA_LIST, Types1_13.METADATA_LIST); // 12 - Metadata

                handler(metadataRewriter.trackerAndRewriterHandler(Types1_13.METADATA_LIST));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_12_1.ADD_PLAYER, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity ID
                map(Types.UUID); // 1 - Player UUID
                map(Types.DOUBLE); // 2 - X
                map(Types.DOUBLE); // 3 - Y
                map(Types.DOUBLE); // 4 - Z
                map(Types.BYTE); // 5 - Yaw
                map(Types.BYTE); // 6 - Pitch
                map(Types1_12.METADATA_LIST, Types1_13.METADATA_LIST); // 7 - Metadata

                handler(metadataRewriter.trackerAndRewriterHandler(Types1_13.METADATA_LIST, EntityTypes1_13.EntityType.PLAYER));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_12_1.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // 0 - Entity ID
                map(Types.UNSIGNED_BYTE); // 1 - Gamemode
                map(Types.INT); // 2 - Dimension

                handler(wrapper -> {
                    ClientWorld clientChunks = wrapper.user().get(ClientWorld.class);
                    int dimensionId = wrapper.get(Types.INT, 1);
                    clientChunks.setEnvironment(dimensionId);
                });
                handler(metadataRewriter.playerTrackerHandler());
                handler(Protocol1_12_2To1_13.SEND_DECLARE_COMMANDS_AND_TAGS);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_12_1.UPDATE_MOB_EFFECT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Entity id
                map(Types.BYTE); // Effect id
                map(Types.BYTE); // Amplifier
                map(Types.VAR_INT); // Duration

                handler(packetWrapper -> {
                    byte flags = packetWrapper.read(Types.BYTE); // Input Flags

                    if (Via.getConfig().isNewEffectIndicator())
                        flags |= 0x04;

                    packetWrapper.write(Types.BYTE, flags);
                });
            }
        });

        metadataRewriter.registerRemoveEntities(ClientboundPackets1_12_1.REMOVE_ENTITIES);
        metadataRewriter.registerMetadataRewriter(ClientboundPackets1_12_1.SET_ENTITY_DATA, Types1_12.METADATA_LIST, Types1_13.METADATA_LIST);
    }
}
/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2016-2021 ViaVersion and contributors
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
package us.myles.ViaVersion.protocols.protocol1_11to1_10.storage;

import com.google.common.collect.Sets;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.entities.Entity1_11Types.EntityType;
import us.myles.ViaVersion.api.storage.EntityTracker;

import java.util.Set;

public class EntityTracker1_11 extends EntityTracker {
    private final Set<Integer> holograms = Sets.newConcurrentHashSet();

    public EntityTracker1_11(UserConnection user) {
        super(user, EntityType.PLAYER);
    }

    @Override
    public void removeEntity(int entityId) {
        super.removeEntity(entityId);

        if (isHologram(entityId))
            removeHologram(entityId);
    }

    public void addHologram(int entId) {
        holograms.add(entId);
    }

    public boolean isHologram(int entId) {
        return holograms.contains(entId);
    }

    public void removeHologram(int entId) {
        holograms.remove(entId);
    }
}
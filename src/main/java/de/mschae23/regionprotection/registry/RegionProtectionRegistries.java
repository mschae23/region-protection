/*
 * Copyright (C) 2026  mschae23
 *
 * This file is part of Region protection.
 *
 * Region protection is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.mschae23.regionprotection.registry;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import de.mschae23.regionprotection.RegionProtectionMod;
import de.mschae23.regionprotection.region.shape.ProtectionShapeType;

public class RegionProtectionRegistries {
    public static final Registry<ProtectionShapeType<?>> PROTECTION_SHAPE = FabricRegistryBuilder.<ProtectionShapeType<?>>createDefaulted(RegistryKey.ofRegistry(RegionProtectionMod.id("protection_shape")), RegionProtectionMod.id("universal")).buildAndRegister();

    private RegionProtectionRegistries() {
    }

    public static void init() {
    }
}

/*
 * This file is part of Terra.
 *
 * Terra is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Terra is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Terra.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dfsek.terra.bukkit.util;

import java.util.Locale;


public final class MinecraftUtils {
    public static String stripMinecraftNamespace(String in) {
        if(in.startsWith("minecraft:")) return in.substring("minecraft:".length());
        return in;
    }

    public static String normalizeBlockData(String data) {
        int blockStateIndex = data.indexOf('[');
        if(blockStateIndex < 0) {
            return normalizeBlockIdentifier(data);
        }
        return normalizeBlockIdentifier(data.substring(0, blockStateIndex)) + data.substring(blockStateIndex);
    }

    public static String normalizeBlockIdentifier(String identifier) {
        int namespaceSeparator = identifier.indexOf(':');
        if(namespaceSeparator < 0) {
            return normalizeMinecraftBlockPath(identifier);
        }

        String namespace = identifier.substring(0, namespaceSeparator);
        if(!namespace.equals("minecraft")) {
            return identifier;
        }

        String path = identifier.substring(namespaceSeparator + 1);
        String normalized = normalizeMinecraftBlockPath(path);
        if(normalized.equals(path)) {
            return identifier;
        }
        return namespace + ":" + normalized;
    }

    private static String normalizeMinecraftBlockPath(String path) {
        return switch(path.toLowerCase(Locale.ROOT)) {
            case "chain" -> "iron_chain";
            default -> path;
        };
    }
}

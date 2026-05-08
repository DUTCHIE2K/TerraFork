/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package com.dfsek.terra.addons.terrascript.tokenizer;

import java.io.Serial;
import java.io.Serializable;


public class Position implements Serializable {
    @Serial
    private static final long serialVersionUID = -7884369854835617574L;

    private final int line;
    private final int index;

    public Position(int line, int index) {
        this.line = line;
        this.index = index;
    }

    @Override
    public String toString() {
        return (line + 1) + ":" + index;
    }
}

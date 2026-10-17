/*
 * This file is part of Gottsch Books.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
 *
 * Gottsch Books is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Gottsch Books is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Gottsch Books.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.forge.gottschbooks.core.text;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A parsed book file: its front matter ({@code key: value} lines between {@code ---}) and its blocks.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public record BookText(Map<String, String> front, List<Block> blocks) {

    public Optional<String> get(String key) {
        return Optional.ofNullable(front.get(key)).filter(value -> !value.isEmpty());
    }
}

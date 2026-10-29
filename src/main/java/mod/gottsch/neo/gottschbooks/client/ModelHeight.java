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
package mod.gottsch.neo.gottschbooks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.neo.gottschbooks.GottschBooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.LivingEntity;

/**
 * How far a mob's model reaches above and below its feet, in blocks. A hitbox says nothing about antlers, branches or
 * a tall hat, so the mob is rendered once into a buffer that draws nothing and only notes each vertex's height.
 *
 * @param bottom the lowest point, 0 or less
 * @param top the highest point, at least the hitbox's height
 *
 * @author Mark Gottschling on 10/28/2026
 */
record ModelHeight(float bottom, float top) {

    float span() {
        return top - bottom;
    }

    static ModelHeight of(LivingEntity entity) {
        Heights heights = new Heights();
        try {
            Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity)
                    .render(entity, 0F, 1F, new PoseStack(), type -> heights, LightTexture.FULL_BRIGHT);
        } catch (RuntimeException e) {
            // a renderer that wants a real buffer: the hitbox will do
            GottschBooks.LOGGER.debug("couldn't measure {}'s model", entity.getType(), e);
        }
        if (heights.min > heights.max) {
            return new ModelHeight(0F, entity.getBbHeight());
        }
        return new ModelHeight(Math.min(0F, heights.min), Math.max(entity.getBbHeight(), heights.max));
    }

    /** a buffer that keeps only the lowest and highest vertex */
    private static final class Heights implements VertexConsumer {
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            min = Math.min(min, y);
            max = Math.max(max, y);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            return this;
        }
    }
}

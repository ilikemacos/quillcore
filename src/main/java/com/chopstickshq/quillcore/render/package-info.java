/**
 * Quill Render: client side visual optimizations.
 *
 * <p>0.2.0 implements:
 * <ul>
 *   <li>Entity render-distance cutoff ({@link com.chopstickshq.quillcore.render.EntityRenderCutoff})</li>
 *   <li>Static block-entity cutoff for signs/skulls ({@link com.chopstickshq.quillcore.render.BlockEntityRenderCutoff})</li>
 * </ul>
 * Purely visual; never changes simulation. Upscaling backends live under {@code upscale/}.
 */
package com.chopstickshq.quillcore.render;

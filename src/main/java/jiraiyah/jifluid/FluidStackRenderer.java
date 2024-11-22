/***********************************************************************************
 * Copyright (c) 2024 Alireza Khodakarami (Jiraiyah)                               *
 * ------------------------------------------------------------------------------- *
 * MIT License                                                                     *
 * =============================================================================== *
 * Permission is hereby granted, free of charge, to any person obtaining a copy    *
 * of this software and associated documentation files (the "Software"), to deal   *
 * in the Software without restriction, including without limitation the rights    *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell       *
 * copies of the Software, and to permit persons to whom the Software is           *
 * furnished to do so, subject to the following conditions:                        *
 * ------------------------------------------------------------------------------- *
 * The above copyright notice and this permission notice shall be included in all  *
 * copies or substantial portions of the Software.                                 *
 * ------------------------------------------------------------------------------- *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR      *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,        *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE     *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER          *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,   *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE   *
 * SOFTWARE.                                                                       *
 ***********************************************************************************/

package jiraiyah.jifluid;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.systems.RenderSystem;
import jiraiyah.jiralib.client.MouseHelper;
import jiraiyah.jiralib.client.interfaces.IIngredientRenderer;
import jiraiyah.jiralib.record.FluidStack;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * The `FluidStackRenderer` class is a specialized renderer for displaying fluid stacks
 * within a graphical user interface in a Minecraft mod. This class implements the
 * `IIngredientRenderer` interface for `FluidStack` objects, allowing it to render
 * fluid quantities and provide detailed tooltips about the fluid's properties.
 *
 * <p>The renderer is highly configurable, supporting various tooltip display modes
 * through the `FluidTooltipMode` enum. It can render fluid amounts in both Fabric
 * conventions and milli-buckets (mB), with options to show or hide the fluid's capacity.</p>
 *
 * <p>Key features of this class include:</p>
 * <ul>
 *   <li>Rendering fluid stacks with customizable dimensions and z-order for depth management.</li>
 *   <li>Generating tooltips that display fluid information, such as type, amount, and capacity.</li>
 *   <li>Utilizing the Fabric API for fluid rendering, ensuring compatibility with the modding platform.</li>
 * </ul>
 *
 * <p>The class relies on several internal and external libraries, including the Fabric API
 * for fluid rendering and Minecraft's rendering systems. It also uses organization-specific
 * utilities like `FluidHelper` for unit conversions.</p>
 *
 * <p>Usage of this class involves creating an instance with the desired configuration
 * and invoking its methods to render fluids and generate tooltips as needed.</p>
 */
@SuppressWarnings("unused")
public class FluidStackRenderer implements IIngredientRenderer<FluidStack>
{
    /**
     * A `NumberFormat` instance used for formatting fluid amounts in tooltips.
     * This ensures that fluid quantities are displayed in a human-readable format.
     */
    private static final NumberFormat nf = NumberFormat.getIntegerInstance();

    /**
     * The maximum capacity of the fluid stack in milli-buckets (mB).
     * This value is used to determine the proportion of the fluid that should be rendered
     * and displayed in tooltips.
     */
    private final long capacityMb;

    /**
     * The mode for displaying tooltips, defined by the `FluidTooltipMode` enum.
     * This determines how fluid amounts and capacities are presented in the user interface.
     */
    private final FluidTooltipMode tooltipMode;

    /**
     * The width of the rendered fluid stack in pixels.
     * This defines the horizontal size of the fluid representation in the UI.
     */
    private final int width;

    /**
     * The height of the rendered fluid stack in pixels.
     * This defines the vertical size of the fluid representation in the UI.
     */
    private final int height;

    /**
     * The z-order for rendering, which affects the rendering depth.
     * A higher z-order value means the fluid stack will be rendered on top of other elements.
     */
    private int zOrder;

    /**
     * Constructs a `FluidStackRenderer` with default settings.
     * This constructor initializes the renderer with a default fluid capacity equivalent to one bucket,
     * measured in milli-buckets (mB), and sets the default rendering dimensions to 16x16 pixels.
     *
     * <p>The default configuration is designed to provide a standard rendering setup that can be used
     * immediately for displaying fluid stacks in a user interface. It assumes that the user wants to
     * display both the fluid amount and its capacity in the tooltip, using milli-buckets as the unit of measurement.</p>
     *
     * <p>This constructor is particularly useful for quick instantiation when the default settings
     * are sufficient for the rendering requirements, allowing for rapid development and testing.</p>
     */
    public FluidStackRenderer()
    {
        this(FluidHelper.convertDropletsToMb(FluidConstants.BUCKET), true, true, 16, 16);
    }

    /**
     * Constructs a `FluidStackRenderer` with specified capacity, tooltip display options, and dimensions.
     * This constructor allows for customization of the fluid rendering by specifying the maximum capacity
     * of the fluid stack, whether the capacity should be displayed in the tooltip, and the dimensions
     * of the rendered fluid stack in the user interface.
     *
     * <p>The `capacity` parameter defines the maximum amount of fluid, in milli-buckets (mB), that the
     * renderer can display. This is crucial for calculating the proportion of the fluid to render
     * visually and for displaying accurate information in the tooltip.</p>
     *
     * <p>The `showCapacity` parameter determines whether the tooltip should include the fluid's capacity
     * alongside its current amount. This provides users with a clearer understanding of how much fluid
     * is present relative to the maximum capacity.</p>
     *
     * <p>The `width` and `height` parameters specify the dimensions of the fluid stack's visual representation
     * in pixels. These dimensions allow for flexibility in how the fluid is displayed, accommodating
     * different UI layouts and design requirements.</p>
     *
     * @param capacity     The capacity of the fluid stack in milli-buckets.
     * @param showCapacity Whether to show the capacity in the tooltip.
     * @param width        The width of the rendered fluid stack in pixels.
     * @param height       The height of the rendered fluid stack in pixels.
     */
    public FluidStackRenderer(long capacity, boolean showCapacity, int width, int height)
    {
        this(capacity, showCapacity ? FluidTooltipMode.SHOW_AMOUNT_AND_CAPACITY_MB : FluidTooltipMode.SHOW_AMOUNT_MB, width, height);
    }

    /**
     * Constructs a `FluidStackRenderer` with specified capacity, tooltip display options, unit preference, and dimensions.
     * This constructor provides extensive customization for rendering fluid stacks by allowing the specification
     * of the fluid's maximum capacity, tooltip display preferences, unit of measurement, and the dimensions
     * of the rendered fluid stack in the user interface.
     *
     * <p>The `capacity` parameter sets the maximum amount of fluid, in milli-buckets (mB), that the renderer
     * can handle. This is essential for determining the visual representation of the fluid level and for
     * displaying accurate information in the tooltip.</p>
     *
     * <p>The `showCapacity` parameter indicates whether the tooltip should display the fluid's capacity
     * alongside its current amount. This option enhances the user's understanding of the fluid's status
     * relative to its maximum capacity.</p>
     *
     * <p>The `useMilliBuckets` parameter specifies whether the fluid amounts should be displayed in milli-buckets
     * or in the default unit used by the Fabric API. This allows for flexibility in unit representation
     * based on user preference or application requirements.</p>
     *
     * <p>The `width` and `height` parameters define the dimensions of the fluid stack's visual representation
     * in pixels. These dimensions provide adaptability for different UI layouts and design specifications.</p>
     *
     * @param capacity       The capacity of the fluid stack in milli-buckets.
     * @param showCapacity   Whether to show the capacity in the tooltip.
     * @param useMilliBuckets Whether to use milli-buckets for display.
     * @param width          The width of the rendered fluid stack in pixels.
     * @param height         The height of the rendered fluid stack in pixels.
     */
    public FluidStackRenderer(long capacity, boolean showCapacity, boolean useMilliBuckets, int width, int height)
    {
        this(capacity, showCapacity && useMilliBuckets
                       ? FluidTooltipMode.SHOW_AMOUNT_AND_CAPACITY_MB
                       : showCapacity
                         ? FluidTooltipMode.SHOW_AMOUNT_AND_CAPACITY_FABRIC
                         : useMilliBuckets
                            ? FluidTooltipMode.SHOW_AMOUNT_MB
                            : FluidTooltipMode.SHOW_AMOUNT_FABRIC
                , width, height);
    }

    /**
     * Constructs a `FluidStackRenderer` with specified capacity, tooltip mode, and dimensions.
     * This constructor provides detailed customization for rendering fluid stacks by allowing
     * the specification of the fluid's maximum capacity, the mode for displaying tooltips,
     * and the dimensions of the rendered fluid stack in the user interface.
     *
     * <p>The `capacity` parameter sets the maximum amount of fluid, in milli-buckets (mB),
     * that the renderer can handle. This is crucial for determining the visual representation
     * of the fluid level and for displaying accurate information in the tooltip.</p>
     *
     * <p>The `tooltipMode` parameter, defined by the `FluidTooltipMode` enum, specifies how
     * the fluid's amount and capacity should be displayed in the tooltip. This allows for
     * flexibility in presenting fluid information according to user preferences or application
     * requirements. The available modes include displaying amounts in both Fabric conventions
     * and milli-buckets, with options to include or exclude capacity information.</p>
     *
     * <p>The `width` and `height` parameters define the dimensions of the fluid stack's visual
     * representation in pixels. These dimensions provide adaptability for different UI layouts
     * and design specifications, ensuring that the fluid stack is rendered appropriately within
     * the available space.</p>
     *
     * <p>This constructor is intended for advanced use cases where precise control over the
     * rendering and tooltip display is required, making it suitable for complex UI designs
     * and custom modding scenarios.</p>
     *
     * @param capacity   The capacity of the fluid stack in milli-buckets.
     * @param tooltipMode The mode for displaying tooltips, as defined by the `FluidTooltipMode` enum.
     * @param width      The width of the rendered fluid stack in pixels.
     * @param height     The height of the rendered fluid stack in pixels.
     */
    private FluidStackRenderer(long capacity, FluidTooltipMode tooltipMode, int width, int height)
    {
        Preconditions.checkArgument(capacity > 0, "capacity must be > 0");
        Preconditions.checkArgument(width > 0, "width must be > 0");
        Preconditions.checkArgument(height > 0, "height must be > 0");
        this.capacityMb = capacity;
        this.tooltipMode = tooltipMode;
        this.width = width;
        this.height = height;
        this.zOrder = 1;
    }

    /**
     * Retrieves the maximum capacity of the fluid stack in milli-buckets (mB).
     * This method returns the capacity value that was set during the instantiation
     * of the `FluidStackRenderer`. The capacity is used to determine the proportion
     * of the fluid that should be rendered and displayed in tooltips.
     *
     * <p>The capacity is a crucial parameter for rendering fluid stacks, as it defines
     * the upper limit of fluid that can be visually represented. This allows for accurate
     * scaling of the fluid's visual representation and ensures that the tooltip information
     * is consistent with the actual fluid content.</p>
     *
     * <p>By providing access to the capacity value, this method enables other components
     * or systems to query and utilize the fluid stack's capacity for various purposes,
     * such as calculations, comparisons, or display adjustments.</p>
     *
     * @return The capacity of the fluid stack in milli-buckets.
     */
    public long getCapacityMb()
    {
        return capacityMb;
    }

    /**
     * Sets the z-order for rendering the fluid stack, which affects the rendering depth.
     * The z-order determines the stacking order of rendered elements, with higher values
     * indicating that the fluid stack should be rendered on top of other elements with lower z-order values.
     *
     * <p>Adjusting the z-order is useful in complex user interfaces where multiple elements
     * are rendered in overlapping layers. By setting the z-order, you can control which elements
     * appear in front of or behind others, ensuring that the fluid stack is displayed correctly
     * within the intended visual hierarchy.</p>
     *
     * <p>This method allows for dynamic changes to the rendering order, enabling responsive
     * UI designs that can adapt to different contexts or user interactions.</p>
     *
     * @param index The z-order index to set for the fluid stack rendering. A higher index
     *              means the fluid stack will be rendered above elements with lower indices.
     */
    public void setZOrder(int index)
    {
        this.zOrder = index;
    }

    /**
     * Renders the specified `FluidStack` at a given position and with specified dimensions.
     * This method is responsible for visually representing the fluid stack within the user interface,
     * taking into account the fluid's current amount relative to its maximum capacity.
     *
     * <p>The rendering process involves setting the appropriate texture and color for the fluid,
     * and drawing the fluid's sprite in a vertical manner to accurately depict the fluid level.
     * The method ensures that the fluid is rendered with the correct proportions and visual style,
     * adhering to the game's graphical standards.</p>
     *
     * <p>The `context` parameter provides the drawing context, which includes the necessary
     * rendering tools and settings. The `fluid` parameter specifies the `FluidStack` to be rendered,
     * containing both the fluid type and its current amount.</p>
     *
     * <p>The `x` and `y` parameters define the top-left corner of the rendering area, while the
     * `width` and `height` parameters specify the dimensions of the rendered fluid stack in pixels.
     * These dimensions allow for flexibility in how the fluid is displayed, accommodating different
     * UI layouts and design requirements.</p>
     *
     * <p>The `maxCapacity` parameter represents the maximum capacity of the fluid stack, in milli-buckets (mB),
     * and is used to calculate the proportion of the fluid to render. This ensures that the visual
     * representation accurately reflects the fluid's current state.</p>
     *
     * @param context     The drawing context used for rendering.
     * @param fluid       The `FluidStack` to render, containing the fluid type and amount.
     * @param x           The x-coordinate for the top-left corner of the rendering area.
     * @param y           The y-coordinate for the top-left corner of the rendering area.
     * @param width       The width of the rendered fluid stack in pixels.
     * @param height      The height of the rendered fluid stack in pixels.
     * @param maxCapacity The maximum capacity of the fluid stack in milli-buckets.
     */
    public void drawFluid(DrawContext context, FluidStack fluid, int x, int y, int width, int height, long maxCapacity)
    {
        if (fluid.fluid().getFluid() == Fluids.EMPTY)
            return;

        RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        y += height;
        final Sprite sprite = FluidVariantRendering.getSprite(fluid.fluid());

        if(sprite == null)
            return;

        int color = FluidVariantRendering.getColor(fluid.fluid());

        final int drawHeight = (int) (fluid.amount() / (maxCapacity * 1F) * height);
        final int iconHeight = sprite.getY();
        int offsetHeight = drawHeight;

        RenderSystem.setShaderColor((color >> 16 & 255) / 255.0F, (float) (color >> 8 & 255) / 255.0F, (float) (color & 255) / 255.0F, 1F);

        FluidState fluidState = fluid.fluid().getFluid().getDefaultState();

        int iteration = 0;
        while (offsetHeight != 0)
        {
            final int curHeight = Math.min(offsetHeight, iconHeight);

            context.getMatrices().push();
            context.getMatrices().translate(0f, 0f, 0.01f * zOrder);
            context.drawSpriteStretched(RenderLayer::getGuiTextured, sprite, x, y - offsetHeight, width, curHeight);
            //context.drawSprite(x, y - offsetHeight, 0, width, curHeight, sprite);
            context.getMatrices().pop();
            offsetHeight -= curHeight;
            iteration++;
            if (iteration > 50)
                break;
        }
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        FluidRenderHandler renderHandler = FluidRenderHandlerRegistry.INSTANCE.get(fluid.fluid().getFluid());

        if(renderHandler == null)
            return;

        ClientWorld world = MinecraftClient.getInstance().world;
        Identifier id = renderHandler.getFluidSprites(world, null, fluidState)[0].getAtlasId();
        RenderSystem.setShaderTexture(0, id);
    }

    /**
     * Generates a tooltip for the specified `FluidStack`, providing detailed information
     * about the fluid's type, amount, and optionally its capacity, formatted according to
     * the current tooltip mode.
     *
     * <p>This method constructs a list of `Text` components that represent the tooltip
     * for a given `FluidStack`. The tooltip includes the fluid's display name and its
     * amount, with the option to also display the capacity based on the `FluidTooltipMode`
     * configuration. The tooltip is designed to be informative and user-friendly, aiding
     * players in understanding the fluid's properties at a glance.</p>
     *
     * <p>The `fluidStack` parameter specifies the `FluidStack` for which the tooltip is
     * generated, containing both the fluid type and its current amount. The `tooltipFlag`
     * parameter provides context for the tooltip, such as whether advanced tooltips are
     * enabled. The `modid` parameter is used to localize the tooltip text, ensuring that
     * it is consistent with the mod's language settings.</p>
     *
     * <p>This method leverages the organization's internal localization and formatting
     * utilities to ensure that the tooltip is displayed correctly across different
     * languages and configurations.</p>
     *
     * @param fluidStack  The `FluidStack` for which the tooltip is requested, containing the fluid type and amount.
     * @param tooltipFlag The context for the tooltip, indicating whether advanced tooltips are enabled.
     * @param modid       The mod identifier used for localizing the tooltip text.
     * @return A list of `Text` components representing the tooltip, formatted according to the current settings.
     */
    @Override
    public List<Text> getTooltip(FluidStack fluidStack, Item.TooltipContext tooltipFlag, String modid)
    {
        List<Text> tooltip = new ArrayList<>();
        FluidVariant fluidType = fluidStack.fluid();
        if (fluidType == null)
            return tooltip;

        MutableText displayName = Text.translatable("block." + Registries.FLUID.getId(fluidStack.fluid().getFluid()).toTranslationKey());
        tooltip.add(displayName);

        long amount = fluidStack.amount();
        if (tooltipMode == FluidTooltipMode.SHOW_AMOUNT_AND_CAPACITY_MB)
        {
            MutableText amountString = Text.translatable(modid + ".tooltip.liquid.amount.with.capacity", nf.format(FluidHelper.convertDropletsToMb(amount)), nf.format(FluidHelper.convertDropletsToMb(capacityMb)));
            tooltip.add(amountString.fillStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
        }
        else if (tooltipMode == FluidTooltipMode.SHOW_AMOUNT_MB)
        {
            MutableText amountString = Text.translatable(modid + ".tooltip.liquid.amount", nf.format(FluidHelper.convertDropletsToMb(amount)));
            tooltip.add(amountString.fillStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
        }
        else if (tooltipMode == FluidTooltipMode.SHOW_AMOUNT_AND_CAPACITY_FABRIC)
        {
            MutableText amountString = Text.translatable(modid + ".tooltip.liquid.amount.with.capacity", nf.format(amount), nf.format(capacityMb));
            tooltip.add(amountString.fillStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
        }
        else if (tooltipMode == FluidTooltipMode.SHOW_AMOUNT_FABRIC)
        {
            MutableText amountString = Text.translatable(modid + ".tooltip.liquid.amount", nf.format(amount));
            tooltip.add(amountString.fillStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
        }

        return tooltip;
    }

    /**
     * Retrieves the width of the rendered fluid stack in pixels.
     * This method returns the width dimension that was set during the instantiation
     * of the `FluidStackRenderer`. The width is a critical parameter for determining
     * how the fluid stack is visually represented within the user interface.
     *
     * <p>The width value is used to define the horizontal size of the fluid's visual
     * representation, ensuring that it fits appropriately within the designated UI space.
     * This allows for consistent and accurate rendering of the fluid stack, aligning with
     * the overall design and layout of the interface.</p>
     *
     * <p>By providing access to the width value, this method enables other components
     * or systems to query and utilize the fluid stack's width for various purposes,
     * such as layout calculations, rendering adjustments, or collision detection.</p>
     *
     * @return The width of the rendered fluid stack in pixels.
     */
    @Override
    public int getWidth()
    {
        return width;
    }

    /**
     * Retrieves the height of the rendered fluid stack in pixels.
     * This method returns the height dimension that was set during the instantiation
     * of the `FluidStackRenderer`. The height is a crucial parameter for determining
     * how the fluid stack is visually represented within the user interface.
     *
     * <p>The height value is used to define the vertical size of the fluid's visual
     * representation, ensuring that it fits appropriately within the designated UI space.
     * This allows for consistent and accurate rendering of the fluid stack, aligning with
     * the overall design and layout of the interface.</p>
     *
     * <p>By providing access to the height value, this method enables other components
     * or systems to query and utilize the fluid stack's height for various purposes,
     * such as layout calculations, rendering adjustments, or collision detection.</p>
     *
     * @return The height of the rendered fluid stack in pixels.
     */
    @Override
    public int getHeight()
    {
        return height;
    }

    /**
     * Determines if the mouse cursor is positioned above a specified rectangular area.
     * This method checks whether the given mouse coordinates fall within the bounds
     * of a defined area, which is useful for detecting mouse interactions with rendered
     * elements in the user interface.
     *
     * <p>The method takes into account the position and size of the area, as well as
     * any specified offsets, to accurately determine if the mouse is hovering over
     * the target region. This functionality is essential for implementing interactive
     * UI components, such as buttons or tooltips, that respond to mouse movements.</p>
     *
     * <p>The `mouseX` and `mouseY` parameters represent the current coordinates of the
     * mouse cursor. The `x` and `y` parameters define the top-left corner of the area
     * to check, while the `offsetX` and `offsetY` parameters allow for additional
     * adjustments to the area's position, providing flexibility in how the area is
     * defined relative to other UI elements.</p>
     *
     * @param mouseX  The x-coordinate of the mouse cursor.
     * @param mouseY  The y-coordinate of the mouse cursor.
     * @param x       The x-coordinate of the top-left corner of the area to check.
     * @param y       The y-coordinate of the top-left corner of the area to check.
     * @param offsetX The horizontal offset to apply to the area's position.
     * @param offsetY The vertical offset to apply to the area's position.
     * @return True if the mouse cursor is above the specified area, false otherwise.
     */
    public boolean isMouseAboveArea(int mouseX, int mouseY, int x, int y, int offsetX, int offsetY)
    {
        return MouseHelper.isMouseOver(mouseX, mouseY, x, y, getWidth(), getHeight(), offsetX, offsetY);
    }
}
package jiraiyah.jifluid.block;

import com.mojang.serialization.MapCodec;
import jiraiyah.jifluid.FluidHelper;
import jiraiyah.jiralib.interfaces.ITickBE;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Constructs a new instance of `MachineBase` with the specified settings.
 *
 * <p>This constructor initializes the `MachineBase` block using the provided
 * `AbstractBlock.Settings`. The settings parameter allows customization of
 * various block properties such as hardness, resistance, and other block
 * behaviors specific to the Minecraft environment.</p>
 */
@SuppressWarnings("unused")
public abstract class FluidTankBase extends Block implements BlockEntityProvider
{
    /**
     * A codec for serializing and deserializing instances of `FluidTankBase`.
     */
    protected MapCodec<? extends FluidTankBase> CODEC;

    /**
     * Constructs a new `FluidTankBase` block with the specified settings.
     *
     * @param settings The settings for the block, including material, hardness, and other properties.
     */
    public FluidTankBase(Settings settings)
    {
        super(settings);
    }

    /**
     * Returns the codec used for serializing and deserializing this block.
     *
     * @return A `MapCodec` for this block.
     */
    @Override
    protected MapCodec<? extends Block> getCodec()
    {
        return CODEC;
    }

    /**
     * Handles the interaction of a player using an item on this block.
     * This method utilizes the `FluidHelper` to facilitate interaction with the block's fluid storage.
     *
     * @param stack  The item stack being used by the player.
     * @param state  The current block state.
     * @param world  The world in which the block resides.
     * @param pos    The position of the block in the world.
     * @param player The player interacting with the block.
     * @param hand   The hand the player is using to interact.
     * @param hit    The result of the block hit.
     * @return The result of the action, indicating success or failure.
     */
    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit)
    {
        FluidHelper.interactWithBlock(world, pos, player, hand);
        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    /**
     * Handles synchronized block events, allowing the block to respond to specific events.
     *
     * @param state The current block state.
     * @param world The world in which the block resides.
     * @param pos   The position of the block in the world.
     * @param type  The type of event.
     * @param data  Additional data for the event.
     * @return True if the event was handled successfully, false otherwise.
     */
    @Override
    protected boolean onSyncedBlockEvent(BlockState state, World world, BlockPos pos, int type, int data)
    {
        super.onSyncedBlockEvent(state, world, pos, type, data);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity != null && blockEntity.onSyncedBlockEvent(type, data);
    }

    /**
     * Provides a ticker for the block entity, allowing it to perform periodic updates.
     *
     * @param world The world in which the block resides.
     * @param state The current block state.
     * @param type  The type of block entity.
     * @param <T>   The type of the block entity.
     * @return A `BlockEntityTicker` for the block entity, or null if no ticker is required.
     */
    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
    {
        return ITickBE.createTicker(world);
    }

    /**
     * Creates a screen handler factory for the block, allowing it to provide a user interface.
     *
     * @param state The current block state.
     * @param world The world in which the block resides.
     * @param pos   The position of the block in the world.
     * @return A `NamedScreenHandlerFactory` for the block, or null if no factory is available.
     */
    @Nullable
    @Override
    protected NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos)
    {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof NamedScreenHandlerFactory ? (NamedScreenHandlerFactory) blockEntity : null;
    }
}
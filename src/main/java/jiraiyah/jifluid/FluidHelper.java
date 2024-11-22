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

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The `FluidHelper` class provides utility methods for handling fluid transfers and interactions
 * within a Minecraft mod environment. It includes methods for transferring fluids between tanks
 * and inventories, interacting with fluid storage blocks, and converting fluid measurements.
 * This class utilizes the Fabric API for fluid handling and storage.
 */
@SuppressWarnings({"unused", "BooleanMethodIsAlwaysInverted", "UnusedReturnValue"})
public class FluidHelper
{

    // Default fluid capacities in terms of buckets
    public static long DEFAULT_FLUID_CAPACITY = FluidConstants.BUCKET * 10;
    public static long DEFAULT_FLUID_SMALL = FluidConstants.BUCKET * 5;
    public static long DEFAULT_FLUID_MEDIUM = FluidConstants.BUCKET * 25;
    public static long DEFAULT_FLUID_LARGE = FluidConstants.BUCKET * 50;
    public static long DEFAULT_FLUID_EXTRA_LARGE = FluidConstants.BUCKET * 100;

    // Specific material-based fluid capacities
    public static long FLUID_CAPACITY_WOOD = FluidConstants.BUCKET * 4;
    public static long FLUID_CAPACITY_STONE = FluidConstants.BUCKET * 16;
    public static long FLUID_CAPACITY_COPPER = FluidConstants.BUCKET * 36;
    public static long FLUID_CAPACITY_IRON = FluidConstants.BUCKET * 64;
    public static long FLUID_CAPACITY_OBSIDIAN = FluidConstants.BUCKET * 100;
    public static long FLUID_CAPACITY_GOLD = FluidConstants.BUCKET * 144;
    public static long FLUID_CAPACITY_DIAMOND = FluidConstants.BUCKET * 196;
    public static long FLUID_CAPACITY_EMERALD = FluidConstants.BUCKET * 256;
    public static long FLUID_CAPACITY_STAR = FluidConstants.BUCKET * 324;
    public static long FLUID_CAPACITY_NETHERITE = FluidConstants.BUCKET * 432;
    public static long FLUID_CAPACITY_END = FluidConstants.BUCKET * 540;

    // Conversion factor for milli-buckets (mB)
    public static long MILLI_BUCKET = FluidConstants.BUCKET / 1000;

    /**
     * Facilitates the bidirectional transfer of fluids between two inventories and a fluid storage tank.
     * This method first attempts to transfer fluid from the specified input inventory slot to the tank.
     * If this transfer is not possible, it then attempts to transfer fluid from the tank to the specified
     * output inventory slot. The method ensures that the transfer only occurs if there is sufficient fluid
     * in the source and adequate space in the destination.
     *
     * @param world The `World` object representing the game world where the fluid transfer is taking place.
     *              This is used to access the environment and play sound effects during the transfer.
     * @param pos The `BlockPos` indicating the position of the tank within the world. This is used to
     *            determine the location for sound effects and other positional logic.
     * @param tank The `SingleVariantStorage<FluidVariant>` representing the fluid storage tank. This tank
     *             is the destination or source of the fluid transfer, depending on the direction of the transfer.
     * @param inputInventory The `Inventory` object representing the inventory from which fluid is to be transferred
     *                       to the tank. This inventory contains the input slot to be checked for fluid items.
     * @param outputInventory The `Inventory` object representing the inventory to which bucket is to be transferred
     *                        from the input inventory to. This inventory contains the output slot to be checked for receiving bucket items.
     * @param inputSlot The index of the slot in the input inventory from which fluid is to be transferred to the tank.
     *                  This slot is checked for fluid items that can be moved to the tank.
     * @param outputSlot The index of the slot in the output inventory to which bucket is to be transferred from the input.
     *                   This slot is checked to ensure it can receive bucket items from the input inventory.
     * @return True if any fluid transfer (either to or from the tank) was successful, false otherwise.
     *         The method returns false if neither transfer direction is possible due to constraints such as
     *         insufficient fluid or lack of space.
     */
    public static boolean handleTankTransfer(World world, BlockPos pos, SingleVariantStorage<FluidVariant> tank, Inventory inputInventory, Inventory outputInventory, int inputSlot, int outputSlot)
    {
        if (transferToTank(world, pos, tank, inputInventory, outputInventory, inputSlot, outputSlot))
            return true;
        return transferFromTank(world, pos, tank, inputInventory, outputInventory, inputSlot, outputSlot);
    }

    /**
     * Transfers fluid from a fluid storage tank to a specified output slot in an inventory. This method
     * checks if the output slot in the inventory can receive bucket items and if the tank has sufficient
     * fluid to transfer. If both conditions are met, the fluid is transferred from the tank to the inventory.
     * The method also handles playing the appropriate sound effects during the transfer.
     *
     * @param world The `World` object representing the game world where the fluid transfer is taking place.
     *              This is used to access the environment and play sound effects during the transfer.
     * @param pos The `BlockPos` indicating the position of the tank within the world. This is used to
     *            determine the location for sound effects and other positional logic.
     * @param tank The `SingleVariantStorage<FluidVariant>` representing the fluid storage tank. This tank
     *             is the source of the fluid transfer.
     * @param inputInventory The `Inventory` object representing the inventory from which empty bucket is to be transferred
     *                       from.
     * @param outputInventory The `Inventory` object representing the inventory to which fluid is to be transferred
     *                        from the tank into it as a full bucket. This inventory contains the output slot to be
     *                        checked for receiving bucket items.
     * @param inputSlot The index of the slot in the input inventory from which empty bucket is to be transferred from.
     * @param outputSlot The index of the slot in the output inventory to which fluid is to be transferred into it as full bucket.
     *                   This slot is checked to ensure it can receive bucket items from the tank.
     * @return True if the fluid transfer from the tank to the inventory was successful, false otherwise.
     *         The method returns false if the transfer is not possible due to constraints such as insufficient
     *         fluid in the tank or lack of space in the output slot.
     */
    public static boolean transferFromTank(World world, BlockPos pos, SingleVariantStorage<FluidVariant> tank, Inventory inputInventory, Inventory outputInventory, int inputSlot, int outputSlot)
    {
        FluidVariant resource = tank.getResource();

        if (!isOutputReceivable(outputInventory, outputSlot, false, resource))
            return false;

        Storage<FluidVariant> slotStorage = ContainerItemContext.withConstant(inputInventory.getStack(inputSlot)).find(FluidStorage.ITEM);

        if (slotStorage == null || resource.isBlank())
            return false;

        try (Transaction transaction = Transaction.openOuter()) {
            long bucketTransfer = slotStorage.insert(resource, FluidConstants.BUCKET, transaction);
            long tankTransfer = tank.extract(resource, FluidConstants.BUCKET, transaction);
            if (bucketTransfer == tankTransfer) {
                transaction.commit();
                SoundEvent sound = FluidVariantAttributes.getFillSound(resource);
                world.playSound(pos.getX(), pos.getY(), pos.getZ(), sound, SoundCategory.BLOCKS, 1, 1, true);
                inputInventory.removeStack(inputSlot, 1);
                Item item = resource.getFluid().getBucketItem();
                outputInventory.setStack(outputSlot, new ItemStack(item, outputInventory.getStack(outputSlot).getCount() + 1));
                inputInventory.markDirty();
                outputInventory.markDirty();
                return true;
            }
        }
        return false;
    }

    /**
     * Transfers fluid from a specified input slot in an inventory to a fluid storage tank. This method
     * checks if the input slot in the inventory contains full bucket items that can be transferred to the tank
     * and if the tank has enough capacity to receive the fluid. If both conditions are met, the fluid is
     * transferred from the inventory to the tank. The method also handles playing the appropriate sound
     * effects during the transfer.
     *
     * @param world The `World` object representing the game world where the fluid transfer is taking place.
     *              This is used to access the environment and play sound effects during the transfer.
     * @param pos The `BlockPos` indicating the position of the tank within the world. This is used to
     *            determine the location for sound effects and other positional logic.
     * @param tank The `SingleVariantStorage<FluidVariant>` representing the fluid storage tank. This tank
     *             is the destination of the fluid transfer.
     * @param inputInventory The `Inventory` object representing the inventory from which full bucket is to be transferred
     *                       to the tank. This inventory contains the input slot to be checked for bucket items.
     * @param outputInventory The `Inventory` object representing the inventory to which empty bucket is to be transferred
     *                        into.
     * @param inputSlot The index of the slot in the input inventory from which full bucket is to be transferred to the tank.
     *                  This slot is checked for full bucket items that can be moved to the tank.
     * @param outputSlot The index of the slot in the output inventory to which empty is to be transferred into.
     * @return True if the fluid transfer from the inventory to the tank was successful, false otherwise.
     *         The method returns false if the transfer is not possible due to constraints such as insufficient
     *         fluid in the input slot or lack of capacity in the tank.
     */
    public static boolean transferToTank(World world, BlockPos pos, SingleVariantStorage<FluidVariant> tank, Inventory inputInventory, Inventory outputInventory, int inputSlot, int outputSlot)
    {
        if (!isOutputReceivable(outputInventory, outputSlot, true, null))
            return false;

        Storage<FluidVariant> slotStorage = ContainerItemContext.withConstant(inputInventory.getStack(inputSlot)).find(FluidStorage.ITEM);

        if (slotStorage == null)
            return false;

        var iterator = slotStorage.iterator();
        if (!iterator.hasNext())
            return false;

        FluidVariant resource = iterator.next().getResource();

        if (resource.isBlank())
            return false;

        try (Transaction transaction = Transaction.openOuter()) {
            long bucketTransfer = slotStorage.extract(resource, FluidConstants.BUCKET, transaction);
            long tankTransfer = tank.insert(resource, FluidConstants.BUCKET, transaction);
            if (bucketTransfer == tankTransfer) {
                transaction.commit();
                SoundEvent sound = FluidVariantAttributes.getEmptySound(resource);
                world.playSound(pos.getX(), pos.getY(), pos.getZ(), sound, SoundCategory.BLOCKS, 1, 1, true);
                inputInventory.removeStack(inputSlot, 1);
                outputInventory.setStack(outputSlot, new ItemStack(Items.BUCKET, outputInventory.getStack(outputSlot).getCount() + 1));
                inputInventory.markDirty();
                outputInventory.markDirty();
                return true;
            }
        }
        return false;
    }

    /**
     * Allows a player to interact with a fluid storage block in the game world. This method checks
     * each side of the block for a fluid storage capability and attempts to interact with it using
     * the player's current item in hand. If a fluid storage is found, the method facilitates the
     * interaction, which may involve transferring fluid between the player's item and the block's
     * storage. The method also handles any necessary sound effects during the interaction.
     *
     * @param world The `World` object representing the game world where the interaction is taking place.
     *              This is used to access the environment and play sound effects during the interaction.
     * @param pos The `BlockPos` indicating the position of the block within the world. This is used to
     *            determine the location for sound effects and other positional logic.
     * @param player The `PlayerEntity` representing the player interacting with the block. This player
     *               provides the item in hand that may be used for fluid transfer.
     * @param hand The `Hand` indicating which hand the player is using to interact with the block. This
     *             is used to determine the item being used for the interaction.
     * @return True if the interaction with the fluid storage was successful, false otherwise. The method
     *         returns false if no fluid storage is found or if the interaction cannot be completed due
     *         to constraints such as incompatible items or lack of fluid.
     */
    public static boolean interactWithBlock(World world, BlockPos pos, PlayerEntity player, Hand hand)
    {
        Storage<FluidVariant> storage;
        for (Direction direction : Direction.values()) {
            storage = FluidStorage.SIDED.find(world, pos, direction);
            if (storage != null) {
                if (FluidStorageUtil.interactWithFluidStorage(storage, player, hand))
                    return true;
            }
        }
        try {
            storage = FluidStorage.SIDED.find(world, pos, null);
            if (storage != null)
                return FluidStorageUtil.interactWithFluidStorage(storage, player, hand);
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Determines whether a given fluid storage tank is empty. This method checks the amount of fluid
     * currently stored in the tank and returns true if the tank contains no fluid. It is useful for
     * validating whether a tank can receive more fluid or if it needs to be refilled.
     *
     * @param tank The `SingleVariantStorage<FluidVariant>` representing the fluid storage tank to be checked.
     *             This tank is evaluated to determine if it contains any fluid.
     * @return True if the tank is empty, meaning it contains zero fluid, false otherwise. The method
     *         returns false if there is any amount of fluid present in the tank.
     * @see SingleVariantStorage
     */
    public static boolean isTankEmpty(SingleVariantStorage<FluidVariant> tank)
    {
        return tank.amount == 0;
    }

    /**
     * Checks whether a specified fluid storage tank is empty. This method evaluates the amount of fluid
     * currently stored in the tank and returns true if the tank contains no fluid. It is useful for
     * determining if a tank is ready to receive more fluid or if it requires refilling.
     *
     * @param tank The `Storage<FluidVariant>` representing the fluid storage tank to be checked.
     *             This storage is assessed to determine if it holds any fluid.
     * @return True if the tank is empty, meaning it contains zero fluid, false otherwise. The method
     *         returns false if there is any amount of fluid present in the tank.
     */
    public static boolean isTankEmpty(Storage<FluidVariant> tank)
    {
        var iterator = tank.iterator();
        if(!iterator.hasNext())
            return true;
        var view = iterator.next();
        return view.getAmount() == 0;
    }

    /**
     * Determines whether a specified slot in an inventory can receive additional fluid items. This method
     * checks the current contents of the slot and evaluates whether it can accept more items based on the
     * specified fluid variant and conditions. It is useful for validating if a slot is ready to receive
     * fluid items during a transfer operation.
     *
     * @param inventory The `Inventory` object representing the inventory containing the slot to be checked.
     *                  This inventory holds the slot that is evaluated for its capacity to receive items.
     * @param slot The index of the slot within the inventory to be checked. This slot is assessed to determine
     *             if it can accept more fluid items.
     * @param shouldAcceptEmpty A boolean flag indicating whether the slot should accept empty containers.
     *                          If true, the slot can receive empty containers; otherwise, it cannot.
     * @param variant The `FluidVariant` representing the type of fluid to be considered for transfer. This
     *                variant is used to check compatibility with the current contents of the slot.
     * @return True if the slot can receive additional fluid items based on the specified conditions, false otherwise.
     *         The method returns false if the slot is full, incompatible with the fluid variant, or if empty
     *         containers are not accepted when they are present.
     */
    public static boolean isOutputReceivable(Inventory inventory, int slot, boolean shouldAcceptEmpty, FluidVariant variant)
    {
        ItemStack stack = inventory.getStack(slot);
        if(stack.isEmpty())
            return true;

        if(shouldAcceptEmpty && isEmptyBucket(inventory, slot) && stack.getCount() < stack.getMaxCount())
            return true;

        Storage<FluidVariant> slotStorage = ContainerItemContext.withConstant(inventory.getStack(slot)).find(FluidStorage.ITEM);

        if(slotStorage == null)
            return true;

        var iterator = slotStorage.iterator();
        if(!iterator.hasNext())
            return true;

        var view = iterator.next();

        return !shouldAcceptEmpty && view.getResource() == variant && stack.getCount() < stack.getMaxCount();
    }

    /**
     * Checks whether a specified slot in an inventory contains an empty bucket. This method evaluates
     * the item present in the given slot and determines if it is an empty bucket item. It is useful for
     * operations that require identifying empty buckets for fluid transfer or crafting purposes.
     *
     * @param inventory The `Inventory` object representing the inventory containing the slot to be checked.
     *                  This inventory holds the slot that is evaluated for the presence of an empty bucket.
     * @param slot The index of the slot within the inventory to be checked. This slot is assessed to determine
     *             if it contains an empty bucket item.
     * @return True if the slot contains an empty bucket, false otherwise. The method returns false if the slot
     *         is empty or contains an item other than an empty bucket.
     */
    public static boolean isEmptyBucket(Inventory inventory, int slot)
    {
        return inventory.getStack(slot).isOf(Items.BUCKET);
    }

    /**
     * Simulates the insertion of a specified amount of fluid type into a `FluidStorage`.
     * This method opens a nested transaction to determine the maximum amount of fluid
     * that can be inserted without actually committing the transaction.
     *
     * @param storage The `FluidStorage` into which fluid is to be inserted.
     * @param resource The fluid variant to be inserted.
     * @param amount The amount of energy to attempt to insert.
     * @param outer The outer transaction within which this simulation occurs.
     * @return The maximum amount of energy that can be inserted.
     */
    public static long simulateInsertion(Storage<FluidVariant> storage, FluidVariant resource, long amount, Transaction outer)
    {
        try(Transaction inner = outer.openNested())
        {
            long max = storage.insert(resource, amount, inner);
            inner.abort();
            return max;
        }
    }

    /**
     * Simulates the insertion of a specified amount of fluid type into a `FluidStorage`.
     * This method opens a nested transaction to determine the maximum amount of fluid
     * that can be inserted without actually committing the transaction.
     *
     * @param storage The `FluidStorage` into which fluid is to be inserted.
     * @param resource The fluid variant to be inserted.
     * @param amount The amount of energy to attempt to insert.
     * @param outer The outer transaction within which this simulation occurs.
     * @return The maximum amount of energy that can be inserted.
     * @see SingleVariantStorage
     */
    public static long simulateInsertion(SingleVariantStorage<FluidVariant> storage, FluidVariant resource, long amount, Transaction outer)
    {
        try(Transaction inner = outer.openNested())
        {
            long max = storage.insert(resource, amount, inner);
            inner.abort();
            return max;
        }
    }

    /**
     * Checks whether the fluid contained in a specified slot of an inventory is the same as the fluid
     * stored in a given fluid storage tank. This method compares the fluid variant in the inventory slot
     * with the fluid variant in the tank to determine if they match. It is useful for operations that
     * require verifying fluid consistency between an inventory and a tank, such as ensuring compatibility
     * before transferring fluids.
     *
     * @param inventory The `Inventory` object representing the inventory containing the slot to be checked.
     *                  This inventory holds the slot that is evaluated for its fluid content.
     * @param slot The index of the slot within the inventory to be checked. This slot is assessed to determine
     *             the fluid variant it contains.
     * @param tank The `SingleVariantStorage<FluidVariant>` representing the fluid storage tank. This tank
     *             is used as a reference to compare its fluid variant with the fluid variant in the inventory slot.
     * @return True if the fluid variant in the specified inventory slot is the same as the fluid variant in the tank,
     *         false otherwise. The method returns false if the slot is empty or contains a different fluid variant
     *         than the one stored in the tank.
     * @see SingleVariantStorage
     */
    public static boolean sameFluidInTank(Inventory inventory, int slot, SingleVariantStorage<FluidVariant> tank)
    {
        Storage<FluidVariant> slotStorage = ContainerItemContext.withConstant(inventory.getStack(slot)).find(FluidStorage.ITEM);
        if (slotStorage == null)
            return false;
        var iterator = slotStorage.iterator();
        if (!iterator.hasNext())
            return false;
        var resource = iterator.next().getResource();

        return tank.getResource().isOf(resource.getFluid());
    }

    /**
     * Checks whether the fluid contained in a specified slot of an inventory is the same as the fluid
     * stored in a given fluid storage tank. This method compares the fluid variant in the inventory slot
     * with the fluid variant in the tank to determine if they match. It is useful for operations that
     * require verifying fluid consistency between an inventory and a tank, such as ensuring compatibility
     * before transferring fluids.
     *
     * @param inventory The `Inventory` object representing the inventory containing the slot to be checked.
     *                  This inventory holds the slot that is evaluated for its fluid content.
     * @param slot The index of the slot within the inventory to be checked. This slot is assessed to determine
     *             the fluid variant it contains.
     * @param tank The `Storage<FluidVariant>` representing the fluid storage tank. This tank
     *             is used as a reference to compare its fluid variant with the fluid variant in the inventory slot.
     * @return True if the fluid variant in the specified inventory slot is the same as the fluid variant in the tank,
     *         false otherwise. The method returns false if the slot is empty or contains a different fluid variant
     *         than the one stored in the tank.
     */
    public static boolean sameFluidInTank(Inventory inventory, int slot, Storage<FluidVariant> tank)
    {
        Storage<FluidVariant> slotStorage = ContainerItemContext.withConstant(inventory.getStack(slot)).find(FluidStorage.ITEM);
        if (slotStorage == null)
            return false;
        var iterator = slotStorage.iterator();
        if (!iterator.hasNext())
            return false;
        var resource = iterator.next().getResource();

        var tankIterator = tank.iterator();

        boolean result = false;

        while(tankIterator.hasNext() && !result)
        {
            if(tankIterator.next().getResource().isOf(resource.getFluid()))
                result = true;
        }
        return result;
    }

    /**
     * Determines whether a specified fluid storage tank is full based on the fluid contained in a given
     * slot of an inventory. This method checks if the fluid variant in the inventory slot matches the
     * fluid variant in the tank and if the tank has reached its maximum capacity for that fluid. It is
     * useful for operations that require verifying if a tank can no longer accept additional fluid of
     * the same type.
     *
     * @param inventory The `Inventory` object representing the inventory containing the slot to be checked.
     *                  This inventory holds the slot that is evaluated for its fluid content.
     * @param slot The index of the slot within the inventory to be checked. This slot is assessed to determine
     *             the fluid variant it contains and its compatibility with the tank.
     * @param tank The `SingleVariantStorage<FluidVariant>` representing the fluid storage tank. This tank
     *             is used to compare its fluid variant with the fluid variant in the inventory slot and to
     *             check if it has reached its capacity.
     * @return True if the tank is full for the fluid variant present in the specified inventory slot, false otherwise.
     *         The method returns false if the tank can still accept more fluid of the same type or if the fluid
     *         variants do not match.
     * @see SingleVariantStorage
     */
    public static boolean isTankFull(Inventory inventory, int slot, SingleVariantStorage<FluidVariant> tank)
    {
        if(!sameFluidInTank(inventory, slot, tank))
            return true;

        return simulateInsertion(tank, tank.getResource(), Long.MAX_VALUE, Transaction.openOuter()) == 0;
    }

    /**
     * Determines whether a specified fluid storage tank is full based on the fluid contained in a given
     * slot of an inventory. This method checks if the fluid variant in the inventory slot matches the
     * fluid variant in the tank and if the tank has reached its maximum capacity for that fluid. It is
     * useful for operations that require verifying if a tank can no longer accept additional fluid of
     * the same type.
     *
     * @param inventory The `Inventory` object representing the inventory containing the slot to be checked.
     *                  This inventory holds the slot that is evaluated for its fluid content.
     * @param slot The index of the slot within the inventory to be checked. This slot is assessed to determine
     *             the fluid variant it contains and its compatibility with the tank.
     * @param tank The `Storage<FluidVariant>` representing the fluid storage tank. This tank
     *             is used to compare its fluid variant with the fluid variant in the inventory slot and to
     *             check if it has reached its capacity.
     * @return True if the tank is full for the fluid variant present in the specified inventory slot, false otherwise.
     *         The method returns false if the tank can still accept more fluid of the same type or if the fluid
     *         variants do not match.
     */
    public static boolean isTankFull(Inventory inventory, int slot, Storage<FluidVariant> tank)
    {
        if(!sameFluidInTank(inventory, slot, tank))
            return true;

        var iterator = tank.iterator();

        boolean result = true;

        while (iterator.hasNext())
        {
            result &= simulateInsertion(tank, iterator.next().getResource(), Long.MAX_VALUE, Transaction.openOuter()) == 0;
        }

        return result;
    }

    /**
     * Determines whether a specified fluid storage tank is full. This method checks if the tank has
     * reached its maximum capacity for the fluid it currently contains. It is useful for operations
     * that require verifying if a tank can no longer accept additional fluid of the same type.
     *
     * @param tank The `SingleVariantStorage<FluidVariant>` representing the fluid storage tank to be checked.
     *             This tank is evaluated to determine if it has reached its capacity for the stored fluid variant.
     * @return True if the tank is full, meaning it cannot accept any more of the current fluid variant,
     *         false otherwise. The method returns false if there is still capacity available in the tank
     *         for additional fluid.
     * @see SingleVariantStorage
     */
    public static boolean isTankFull(SingleVariantStorage<FluidVariant> tank)
    {
        return tank.getAmount() >= tank.getCapacity();
    }

    /**
     * Determines whether a specified fluid storage tank is full. This method checks if the tank has
     * reached its maximum capacity for the fluid it currently contains. It is useful for operations
     * that require verifying if a tank can no longer accept additional fluid of the same type.
     *
     * @param tank The `Storage<FluidVariant>` representing the fluid storage tank to be checked.
     *             This tank is evaluated to determine if it has reached its capacity for the stored fluid variant.
     * @return True if the tank is full, meaning it cannot accept any more of the current fluid variant,
     *         false otherwise. The method returns false if there is still capacity available in the tank
     *         for additional fluid.
     */
    public static boolean isTankFull(Storage<FluidVariant> tank)
    {
        var iterator = tank.iterator();

        boolean result = true;

        while(iterator.hasNext())
        {
            result &= simulateInsertion(tank, iterator.next().getResource(), Long.MAX_VALUE, Transaction.openOuter()) == 0;
        }

        return result;
    }

    /**
     * Determines whether a specified fluid storage tank has enough capacity to accept a full bucket
     * worth of fluid. This method checks if the tank can accommodate the additional fluid amount
     * equivalent to a bucket, ensuring that the transfer can occur without exceeding the tank's capacity.
     * It is useful for operations that involve transferring a bucket of fluid into the tank and verifying
     * that there is sufficient space available.
     *
     * @param inventory The `Inventory` object representing the inventory containing the slot to be checked.
     *                  This parameter is included for consistency with related methods, although it is not
     *                  directly used in this capacity check.
     * @param slot The index of the slot within the inventory. This parameter is included for consistency
     *             with related methods, although it is not directly used in this capacity check.
     * @param tank The `SingleVariantStorage<FluidVariant>` representing the fluid storage tank. This tank
     *             is evaluated to determine if it has enough remaining capacity to accept a bucket's worth
     *             of fluid.
     * @return True if the tank has sufficient capacity to accept a full bucket of fluid, false otherwise.
     *         The method returns false if adding a bucket's worth of fluid would exceed the tank's capacity.
     * @see SingleVariantStorage
     */
    public static boolean hasCapacityForBucket(Inventory inventory, int slot, SingleVariantStorage<FluidVariant> tank)
    {
        if(!sameFluidInTank(inventory, slot, tank))
            return false;

        return simulateInsertion(tank, tank.getResource(), Long.MAX_VALUE, Transaction.openOuter()) >= FluidConstants.BUCKET;
    }

    /**
     * Checks whether a specified fluid storage tank has enough capacity to accept an additional amount
     * of fluid equivalent to a full bucket. This method ensures that the tank can accommodate the fluid
     * without exceeding its capacity, which is crucial for operations involving fluid transfer from an
     * inventory to the tank.
     *
     * @param inventory The `Inventory` object representing the inventory containing the slot to be checked.
     *                  This parameter is included for consistency with related methods, although it is not
     *                  directly used in this capacity check.
     * @param slot The index of the slot within the inventory. This parameter is included for consistency
     *             with related methods, although it is not directly used in this capacity check.
     * @param tank The `Storage<FluidVariant>` representing the fluid storage tank. This tank is evaluated
     *             to determine if it has sufficient remaining capacity to accept a bucket's worth of fluid.
     * @return True if the tank has enough capacity to accept a full bucket of fluid, false otherwise.
     *         The method returns false if adding a bucket's worth of fluid would exceed the tank's capacity.
     */
    public static boolean hasCapacityForBucket(Inventory inventory, int slot, Storage<FluidVariant> tank)
    {
        if(!sameFluidInTank(inventory, slot, tank))
            return false;

        var tankIterator = tank.iterator();

        boolean result = false;

        while(tankIterator.hasNext() && !result)
        {
            if(simulateInsertion(tank, tankIterator.next().getResource(), Long.MAX_VALUE, Transaction.openOuter()) >= FluidConstants.BUCKET)
                result = true;
        }

        return result;
    }

    //TODO: Generate Java Docs
    public static Storage<FluidVariant> getFluidStorage(World world, BlockPos pos, Set<BlockPos> exceptions, Direction direction)
    {
        BlockPos adjacentPos = pos.offset(direction);
        if(exceptions != null && exceptions.contains(adjacentPos))
            return null;

        return FluidStorage.SIDED.find(world, adjacentPos, direction.getOpposite());
    }

    //TODO: Generate Java Docs
    public static List<Storage<FluidVariant>> getAllFluidStorages(World world, BlockPos pos, Set<BlockPos> exceptions)
    {
        List<Storage<FluidVariant>> storages = new ArrayList<>();
        for (Direction direction : Direction.values())
        {
            BlockPos adjacentPos = pos.offset(direction);
            if(exceptions != null && exceptions.contains(adjacentPos))
                continue;

            Storage<FluidVariant> fluidStorage = FluidStorage.SIDED.find(world, adjacentPos, direction.getOpposite());
            if(fluidStorage == null)
                continue;

            storages.add(fluidStorage);
        }
        return storages;
    }

    /**
     * Converts a specified amount of fluid measured in droplets to milli-buckets. In the context of
     * Fabric MC, droplets are the base unit of fluid measurement, and this method provides a way
     * to translate that measurement into milli-buckets, which are commonly used in fluid GUI
     * information. This conversion is essential for ensuring consistency across mods for showing
     * different fluid amounts in systems and APIs that may use varying units of measurement.
     *
     * @param droplets The amount of fluid in droplets to be converted. This value represents the
     *                 fluid quantity in the Fabric MC unit.
     * @return The equivalent amount of fluid in milli-buckets. The method returns a long value
     *         representing the converted fluid quantity, facilitating operations that require
     *         milli-bucket measurements.
     */
    public static long convertDropletsToMb(long droplets)
    {
        return droplets * 1000 / FluidConstants.BUCKET;
    }

    /**
     * Converts a specified amount of fluid measured in milli-buckets to droplets. In the context of
     * Fabric MC, milli-buckets are not a used unit of fluid measurement, and this method provides a way
     * to translate that measurement into droplets, which are the base unit. This conversion is essential
     * for ensuring compatibility and consistency across different fluid systems and APIs that may use
     * varying units of measurement.
     *
     * @param mb The amount of fluid in milli-buckets to be converted. This value represents the fluid
     *           quantity in the milli-bucket unit.
     * @return The equivalent amount of fluid in droplets. The method returns a long value representing
     *         the converted fluid quantity, facilitating operations that require droplet measurements
     *         specially for Fabric MC.
     */
    public static long convertMbToDroplets(long mb)
    {
        return mb * FluidConstants.BUCKET / 1000;
    }
}
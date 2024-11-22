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

package jiraiyah.jifluid.interfaces;

import jiraiyah.jifluid.FluidHelper;
import jiraiyah.jiralib.blockentity.UpdatableBE;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The `IFluidSpreader` interface provides methods for managing fluid transfer between different fluid storage systems.
 * It includes functionality to simulate fluid insertion and to spread fluid across multiple connected fluid storages.
 *
 * <p>This interface is designed to be implemented by classes that handle fluid distribution in a Minecraft modding context,
 * utilizing the Fabric API.</p>
 *
 * <p>Author: Jiraiyah</p>
 */
@SuppressWarnings("unused")
public interface IFluidSpreader
{
    /**
     * Spreads fluid from a `FluidStorage` to adjacent `FluidStorage` instances in the world,
     * It checks each direction for available fluid storage and attempts to distribute fluid evenly.
     * If the fluid amount changes, it updates the block entity state accordingly.
     *
     * @param world The `World` in which the fluid spreading occurs.
     * @param pos The `BlockPos` of the `FluidStorage`.
     * @param storage The `FluidStorage` from which fluid is spread.
     */
    default void spread(World world, BlockPos pos, Storage<FluidVariant> storage)
    {
        spread(world, pos, null, storage);
    }

    /**
     * Spreads fluid from a `FluidStorage` to adjacent `FluidStorage` instances in the world,
     * It checks each direction for available fluid storage and attempts to distribute fluid.
     * The distribution can be equal or max possible per side, depending on the `equalAmount` flag.
     * If the fluid amount changes, it updates the block entity state accordingly.
     *
     * @param world The `World` in which the fluid spreading occurs.
     * @param pos The `BlockPos` of the `FluidStorage`.
     * @param storage The `FluidStorage` from which fluid is spread.
     */
    default void spread(World world, BlockPos pos, Storage<FluidVariant> storage, boolean equalAmount)
    {
        spread(world, pos, null, storage, equalAmount);
    }

    /**
     * Spreads fluid from a `FluidStorage` to adjacent `FluidStorage` instances in the world,
     * excluding specific positions defined in the exceptions set. It checks each direction for available
     * fluid storage and attempts to distribute fluid evenly. If the fluid amount changes, it updates
     * the block entity state accordingly.
     *
     * @param world The `World` in which the fluid spreading occurs.
     * @param pos The `BlockPos` of the `FluidStorage`.
     * @param exceptions A set of `BlockPos` that should be excluded from fluid spreading.
     * @param storage The `FluidStorage` from which fluid is spread.
     */
    default void spread(World world, BlockPos pos, Set<BlockPos> exceptions, Storage<FluidVariant> storage)
    {
        spread(world, pos, exceptions, storage, true);
    }

    /**
     * Spreads fluid from a `FluidStorage` to adjacent `FluidStorage` instances in the world,
     * excluding specific positions defined in the exceptions set. It checks each direction for available
     * fluid storage and attempts to distribute fluid. The distribution can be equal or max possible per side,
     * depending on the `equalAmount` flag. If the fluid amount changes, it updates
     * the block entity state accordingly.
     *
     * @param world The `World` in which the fluid spreading occurs.
     * @param pos The `BlockPos` of the `FluidStorage`.
     * @param exceptions A set of `BlockPos` that should be excluded from fluid spreading.
     * @param storage The `FluidStorage` from which fluid is spread.
     */
    default void spread(World world, BlockPos pos, Set<BlockPos> exceptions, Storage<FluidVariant> storage, boolean equalAmount)
    {
        Iterator<StorageView<FluidVariant>> tankIterator = storage.iterator();
        if(!tankIterator.hasNext())
            return;

        StorageView<FluidVariant> tankView = tankIterator.next();
        FluidVariant tankResource = tankView.getResource();

        List<Storage<FluidVariant>> storages = new ArrayList<>();
        for (Direction direction : Direction.values())
        {
            BlockPos adjacentPos = pos.offset(direction);
            if(exceptions != null && exceptions.contains(adjacentPos))
                continue;

            Storage<FluidVariant> fluidStorage = FluidStorage.SIDED.find(world, adjacentPos, direction.getOpposite());
            if(fluidStorage == null || !fluidStorage.supportsInsertion())
                continue;

            Iterator<StorageView<FluidVariant>> iterator = fluidStorage.iterator();
            if(!iterator.hasNext())
                continue;

            StorageView<FluidVariant> fluidView = iterator.next();
            if(fluidView.getAmount() >= fluidView.getCapacity())
                continue;

            storages.add(fluidStorage);
        }

        if(storages.isEmpty())
            return;

        try(Transaction transaction = Transaction.openOuter())
        {
            long current = tankView.getAmount();
            long totalExtractable = storage.extract(tankResource, Long.MAX_VALUE, transaction);
            long totalInserted = 0;
            long finalAmount = equalAmount ? totalExtractable / storages.size() : totalExtractable;

            for (Storage<FluidVariant> fluidStorage : storages)
            {
                long insertable = FluidHelper.simulateInsertion(fluidStorage, tankResource, finalAmount, transaction);
                long inserted = fluidStorage.insert(tankResource, insertable, transaction);
                totalInserted += inserted;
            }

            if(totalInserted < totalExtractable)
                storage.insert(tankResource, totalExtractable - totalInserted, transaction);

            transaction.commit();

            if(current != tankView.getAmount())
            {
                if(this instanceof UpdatableBE updatableBE)
                    updatableBE.update();
                else if(this instanceof BlockEntity blockEntity)
                    blockEntity.markDirty();
            }
        }
    }
}
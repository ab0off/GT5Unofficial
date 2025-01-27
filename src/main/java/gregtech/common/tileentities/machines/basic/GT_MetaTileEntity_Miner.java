package gregtech.common.tileentities.machines.basic;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicMachine;
import gregtech.api.objects.GT_RenderedTexture;
import gregtech.api.objects.ItemData;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.api.util.GT_Utility;
import gregtech.common.blocks.GT_Block_Ores_Abstract;
import gregtech.common.blocks.GT_TileEntity_Ores;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.FakePlayer;

import java.util.ArrayList;

import static gregtech.api.enums.GT_Values.V;

public class GT_MetaTileEntity_Miner extends GT_MetaTileEntity_BasicDrillerBase {

       protected static int[] RADIUS = new int[]{8, 8, 16, 24},
       SPEED = new int[]{160, 160, 80, 40},
       ENERGY = new int[]{8, 8, 32, 128};


    public GT_MetaTileEntity_Miner(int aID, String aName, String aNameRegional, int aTier) {
        super(aID, aName, aNameRegional, aTier, new String[]{"Digging ore instead of you", ENERGY[aTier] + " EU/t, " + SPEED[aTier] / 20 + " sec per block",
                "Work area " + (RADIUS[aTier] * 2 + 1) + "x" + (RADIUS[aTier] * 2 + 1)}, 2, 2, "Miner.png", new GT_RenderedTexture(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_SIDE_ACTIVE")), new GT_RenderedTexture(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_SIDE")), new GT_RenderedTexture(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_FRONT_ACTIVE")), new GT_RenderedTexture(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_FRONT")), new GT_RenderedTexture(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_TOP_ACTIVE")), new GT_RenderedTexture(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_TOP")), new GT_RenderedTexture(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_BOTTOM_ACTIVE")), new GT_RenderedTexture(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_BOTTOM")));
    }


    public GT_MetaTileEntity_Miner(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures, String aGUIName) {
        super(aName, aTier, aDescription, aTextures, 2, 2, aGUIName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Miner(mName, mTier, mDescriptionArray, mTextures, mGUIName);
    }

    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return (super.allowPutStack(aBaseMetaTileEntity, aIndex, aSide, aStack)) && (aStack.getItem() == MINING_PIPE.getItem());
    }

    @Override
    public boolean hasFreeSpace() {
        for (int i = getOutputSlot(); i < getOutputSlot() + 2; i++) {
            if (mInventory[i] != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getRadius(int aTier) {
        return RADIUS[aTier];
    }

    @Override
    public int getSpeed(int aTier) {
        return SPEED[aTier];
    }

    @Override
    public int getEnergy(int aTier) {
        return ENERGY[aTier];
    }

    @Override
    public boolean workBlock(IGregTechTileEntity aBaseMetaTileEntity) {
        Block block = aBaseMetaTileEntity.getBlockOffset(drillX, drillY, drillZ);
        int blockMeta = aBaseMetaTileEntity.getMetaIDOffset(drillX, drillY, drillZ);
        if (block instanceof GT_Block_Ores_Abstract) {
            TileEntity tTileEntity = getBaseMetaTileEntity().getTileEntityOffset(drillX, drillY, drillZ);
            if (tTileEntity != null && tTileEntity instanceof GT_TileEntity_Ores && ((GT_TileEntity_Ores) tTileEntity).mNatural) {
                mineBlock(aBaseMetaTileEntity, drillX, drillY, drillZ);
                return true;
            }
        } else {
            ItemData association = GT_OreDictUnificator.getAssociation(new ItemStack(block, 1, blockMeta));
            if (association != null && association.mPrefix.toString().startsWith("ore")) {
                mineBlock(aBaseMetaTileEntity, drillX, drillY, drillZ);
                return true;
            }
        }
        return false;
    }

    @Override
    public long maxEUStore() {
        return mTier == 1 ? 4096 : V[mTier] * 64;
    }

    public boolean moveOneDown(IGregTechTileEntity aBaseMetaTileEntity) {
        if (aBaseMetaTileEntity.getYCoord() + drillY - 1 < 0 
        		|| GT_Utility.getBlockHardnessAt(aBaseMetaTileEntity.getWorld(), aBaseMetaTileEntity.getXCoord(), aBaseMetaTileEntity.getYCoord() + drillY - 1, aBaseMetaTileEntity.getZCoord()) < 0
        		|| !GT_Utility.setBlockByFakePlayer(getFakePlayer(aBaseMetaTileEntity), aBaseMetaTileEntity.getXCoord(), aBaseMetaTileEntity.getYCoord() + drillY - 1, aBaseMetaTileEntity.getZCoord(), MINING_PIPE_TIP_BLOCK, 0, true)) {
            isPickingPipes = true;
            return false;
        }
        if (aBaseMetaTileEntity.getBlockOffset(0, drillY, 0) == MINING_PIPE_TIP_BLOCK) {
            aBaseMetaTileEntity.getWorld().setBlock(aBaseMetaTileEntity.getXCoord(), aBaseMetaTileEntity.getYCoord() + drillY, aBaseMetaTileEntity.getZCoord(), MINING_PIPE_BLOCK);
        }
        miningPipes:
        {
            for (int i = 0; i < mInputSlotCount; i++) {
                ItemStack s = getInputAt(i);
                if (s != null && s.getItem() == MINING_PIPE.getItem() && s.stackSize > 0) {
                    s.stackSize--;
                    if (s.stackSize == 0) {
                        mInventory[getInputSlot() + i] = null;
                    }
                    break miningPipes;
                }
            }
            waitMiningPipe = true;
            return false;
        }
        if (aBaseMetaTileEntity.getBlockOffset(0, drillY - 1, 0) != Blocks.air) {
            mineBlock(aBaseMetaTileEntity, 0, drillY - 1, 0);
        }
        aBaseMetaTileEntity.getWorld().setBlock(aBaseMetaTileEntity.getXCoord(), aBaseMetaTileEntity.getYCoord() + drillY - 1, aBaseMetaTileEntity.getZCoord(), MINING_PIPE_TIP_BLOCK);
        drillY--;
        drillZ = -RADIUS[mTier];
        drillX = -RADIUS[mTier];
        return true;
    }

    public void mineBlock(IGregTechTileEntity aBaseMetaTileEntity, int x, int y, int z) {
    	if (!GT_Utility.eraseBlockByFakePlayer(getFakePlayer(aBaseMetaTileEntity), aBaseMetaTileEntity.getXCoord() + x, aBaseMetaTileEntity.getYCoord() + y, aBaseMetaTileEntity.getZCoord() + z, true));
        ArrayList<ItemStack> drops = getBlockDrops(aBaseMetaTileEntity.getBlockOffset(x, y, z), aBaseMetaTileEntity.getXCoord() + x, aBaseMetaTileEntity.getYCoord() + y, aBaseMetaTileEntity.getZCoord() + z);
        if (drops.size() > 0)
            mOutputItems[0] = drops.get(0);
        if (drops.size() > 1)
            mOutputItems[1] = drops.get(1);
        aBaseMetaTileEntity.getWorld().setBlockToAir(aBaseMetaTileEntity.getXCoord() + x, aBaseMetaTileEntity.getYCoord() + y, aBaseMetaTileEntity.getZCoord() + z);
    }

    private ArrayList<ItemStack> getBlockDrops(final Block oreBlock, int posX, int posY, int posZ) {
        final int blockMeta = getBaseMetaTileEntity().getMetaID(posX, posY, posZ);
        return oreBlock.getDrops(getBaseMetaTileEntity().getWorld(), posX, posY, posZ, blockMeta, 1);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setBoolean("isPickingPipe", isPickingPipes);
        aNBT.setInteger("drillX", drillX);
        aNBT.setInteger("drillY", drillY);
        aNBT.setInteger("drillZ", drillZ);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        isPickingPipes = aNBT.getBoolean("isPickingPipe");
        drillX = aNBT.getInteger("drillX");
        drillY = aNBT.getInteger("drillY");
        drillZ = aNBT.getInteger("drillZ");
    }

    private FakePlayer mFakePlayer = null;

    protected FakePlayer getFakePlayer(IGregTechTileEntity aBaseTile) {
    	if (mFakePlayer == null) mFakePlayer = GT_Utility.getFakePlayer(aBaseTile);
    	mFakePlayer.setWorld(aBaseTile.getWorld());
    	mFakePlayer.setPosition(aBaseTile.getXCoord(), aBaseTile.getYCoord(), aBaseTile.getZCoord());
    	return mFakePlayer;
    }

}

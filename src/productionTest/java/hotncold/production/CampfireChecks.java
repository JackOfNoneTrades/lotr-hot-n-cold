package hotncold.production;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.compat.MineFantasyCampfire;

import com.mojang.authlib.GameProfile;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.recipes.CampfireRecipe;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.util.EnumCampfireType;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.api.crafting.IBasicMetre;
import minefantasy.mf2.block.tileentity.TileEntityFirepit;
import minefantasy.mf2.item.food.FoodListMF;
import minefantasy.mf2.item.list.ComponentListMF;

/** Executes real block interactions and tile ticks in an obfuscated release runtime. */
public final class CampfireChecks {

    private final List<ItemStack> drops = new ArrayList<>();
    private final WorldServer world = DimensionManager.getWorld(0);
    private final EntityPlayerMP player = FakePlayerFactory
        .get(world, new GameProfile(UUID.fromString("825e8d77-4291-4a2b-a192-81e61ebc0c81"), "CampfireTest"));
    private TileEntityCampfire tile;
    private MineFantasyCampfire fire;

    public static void run() {
        boolean enabled = Config.enableMineFantasyCampfires;
        float chance = Config.campfireFoodBurnChance;
        CampfireChecks checks = new CampfireChecks();
        MinecraftForge.EVENT_BUS.register(checks);
        try {
            checks.world.getChunkFromBlockCoords(8, 8);
            checks.world.setBlock(8, 199, 8, Blocks.stone);
            checks.world.setBlock(8, 202, 8, Blocks.stone); // keep rain out of deterministic cooking checks
            playerPosition(checks.player);
            Config.enableMineFantasyCampfires = true;
            checks.fueling();
            checks.cooking();
            checks.persistence();
            checks.disabled();
            ProductionFixture.LOG.info(
                "PRODUCTION_CAMPFIRE_PASSED: actual fuel interactions, MF fuel values, capacity, creative, ignition, depletion, signal/soul fires, food outcomes, multi-input/byproducts, non-food, NBT, disabled setting");
        } finally {
            Config.enableMineFantasyCampfires = enabled;
            Config.campfireFoodBurnChance = chance;
            checks.world.setBlockToAir(8, 200, 8);
            MinecraftForge.EVENT_BUS.unregister(checks);
        }
        if (!net.minecraft.server.MinecraftServer.getServer()
            .isDedicatedServer()) {
            cpw.mods.fml.common.FMLCommonHandler.instance()
                .bus()
                .register(new CampfireNetworkChecks());
        }
    }

    private static void playerPosition(EntityPlayerMP player) {
        player.setPosition(8.5, 200, 8.5);
        player.capabilities.isCreativeMode = false;
    }

    private void place(boolean lit, int type) {
        world.setBlockToAir(8, 200, 8);
        world.setBlock(8, 200, 8, CampfireBackportBlocks.getBlockFromLitAndType(lit, type), 2, 3);
        tile = (TileEntityCampfire) world.getTileEntity(8, 200, 8);
        ProductionFixture.require(tile instanceof MineFantasyCampfire, "Campfire integration was not applied");
        fire = (MineFantasyCampfire) tile;
        drops.clear();
    }

    private boolean click(ItemStack stack) {
        player.setCurrentItemOrArmor(0, stack);
        return world.getBlock(8, 200, 8)
            .onBlockActivated(world, 8, 200, 8, player, 1, 0.5F, 0.5F, 0.5F);
    }

    private void fueling() {
        place(false, 0);
        ProductionFixture.require(fire.hotncold$getFuel() == 0, "New campfire granted free fuel");
        ProductionFixture.require(!((IBasicMetre) tile).shouldShowMetre(), "Empty fire shows fuel HUD");
        ItemStack lighter = new ItemStack(Items.flint_and_steel);
        click(lighter);
        ProductionFixture.require(!tile.isLit() && lighter.getItemDamage() == 0, "Empty fire lit or consumed lighter");
        for (Item fuel : new Item[] { Items.stick, ComponentListMF.plank, ComponentListMF.plank_cut,
            ComponentListMF.plank_pane }) {
            place(false, 0);
            ItemStack stack = new ItemStack(fuel, 2);
            int expected = TileEntityFirepit.getItemBurnTime(stack);
            ProductionFixture.require(expected > 0 && click(stack), "MineFantasy fuel was not accepted");
            ProductionFixture.require(
                fire.hotncold$getFuel() == expected && stack.stackSize == 1 && !tile.isLit(),
                "Refueling did not consume exactly one item / match MF value / keep unlit state");
        }
        place(false, 0);
        click(new ItemStack(Items.coal));
        ProductionFixture.require(fire.hotncold$getFuel() == 0, "Non-firepit fuel was accepted");
        ItemStack sticks = new ItemStack(Items.stick, 64);
        while (fire.hotncold$getFuel() < MineFantasyCampfire.FUEL_CAPACITY) {
            click(sticks);
        }
        int remaining = sticks.stackSize;
        click(sticks);
        ProductionFixture.require(sticks.stackSize == remaining, "Full fire consumed fuel");
        ProductionFixture.require(fire.hotncold$getFuel() == 12000, "Fuel capacity differs from MF firepit");
        tile.resetLife(true);
        ProductionFixture.require(fire.hotncold$getFuel() == 12000, "Timer reset changed stored fuel");
        place(false, 0);
        player.capabilities.isCreativeMode = true;
        sticks = new ItemStack(Items.stick);
        click(sticks);
        ProductionFixture.require(sticks.stackSize == 1 && fire.hotncold$getFuel() > 0, "Creative refuel was wrong");
        player.capabilities.isCreativeMode = false;
        click(lighter);
        ProductionFixture.require(tile.isLit(), "Fueled fire did not light");
        int fuel = fire.hotncold$getFuel();
        int damage = lighter.getItemDamage();
        click(lighter);
        ProductionFixture.require(
            fire.hotncold$getFuel() == fuel && lighter.getItemDamage() == damage,
            "Relighting granted fuel or consumed lighter");
        tile.updateEntity();
        ProductionFixture.require(fire.hotncold$getFuel() == fuel - 1, "Lit fire did not consume one fuel tick");
        BlockCampfire.updateCampfireBlockState(0, null, tile);
        tile.updateEntity();
        ProductionFixture.require(fire.hotncold$getFuel() == fuel - 1, "Unlit fire consumed fuel");
        for (int type : new int[] { 0, 1 }) {
            place(true, type);
            tile.updateEntity();
            ProductionFixture.require(!tile.isLit(), "Pre-lit empty fire stayed lit");
            click(new ItemStack(Items.stick));
            tile.setSignalFire(true);
            BlockCampfire.updateCampfireBlockState(1, null, tile);
            tile.setSignalFire(true);
            int ticks = fire.hotncold$getFuel();
            for (int i = 0; i < ticks; i++) {
                tile.updateEntity();
            }
            ProductionFixture.require(
                !tile.isLit() && fire.hotncold$getFuel() == 0 && world.getTileEntity(8, 200, 8) == tile,
                "Signal/soul fire did not extinguish intact at zero fuel");
            ProductionFixture.require(!((IBasicMetre) tile).shouldShowMetre(), "Depleted fire shows HUD");
        }
    }

    private void prepareCooking() {
        place(false, 0);
        click(new ItemStack(Items.stick));
        click(new ItemStack(Items.stick));
        BlockCampfire.updateCampfireBlockState(1, null, tile);
    }

    private void cooking() {
        CampfireRecipe.addToRecipeLists("minecraft:paper/minecraft:cooked_beef/4", EnumCampfireType.BOTH);
        CampfireRecipe.addToRecipeLists("minecraft:iron_ore/minecraft:iron_ingot/4", EnumCampfireType.BOTH);
        CampfireRecipe.addToRecipeLists(
            "minecraft:red_mushroom&minecraft:brown_mushroom/minecraft:mushroom_stew/4/any/minecraft:bowl/1",
            EnumCampfireType.BOTH);
        for (float chance : new float[] { 0, 1 }) {
            Config.campfireFoodBurnChance = chance;
            prepareCooking();
            for (int slot = 0; slot < 4; slot++) {
                tile.setInventorySlotContents(slot, new ItemStack(Items.paper));
            }
            for (int i = 0; i < 3; i++) {
                tile.updateEntity();
            }
            ProductionFixture.require(drops.isEmpty(), "Recipe finished before its cooking time");
            tile.updateEntity();
            ProductionFixture.require(drops.size() == 4, "Four cooking slots did not finish independently");
            for (ItemStack drop : drops) {
                ProductionFixture.require(
                    drop.getItem() == (chance == 1 ? FoodListMF.burnt_food : Items.cooked_beef) && drop.stackSize == 1,
                    "Wrong cooking outcome or duplicated output");
            }
            for (int slot = 0; slot < 4; slot++) {
                ProductionFixture.require(tile.getStackInSlot(slot) == null, "Recipe did not consume input");
            }
        }
        // Verify the probability threshold in the real output path, in addition to the 0% / 100% cases.
        Config.campfireFoodBurnChance = 0.25F;
        for (long seed : new long[] { 0, 4096 }) {
            prepareCooking();
            tile.setInventorySlotContents(0, new ItemStack(Items.paper));
            tile.setCookingTimeInSlot(0, 3);
            world.rand.setSeed(seed);
            tile.updateEntity();
            boolean expectedBurn = new java.util.Random(seed).nextFloat() < 0.25F;
            ProductionFixture.require(
                drops.size() == 1 && (drops.get(0)
                    .getItem() == FoodListMF.burnt_food) == expectedBurn,
                "Default probability threshold was not applied");
        }
        Config.campfireFoodBurnChance = 1;
        prepareCooking();
        tile.setInventorySlotContents(0, new ItemStack(Blocks.iron_ore));
        for (int i = 0; i < 4; i++) {
            tile.updateEntity();
        }
        ProductionFixture.require(
            drops.size() == 1 && drops.get(0)
                .getItem() == Items.iron_ingot,
            "Non-food burned");
        prepareCooking();
        tile.setInventorySlotContents(0, new ItemStack(Blocks.red_mushroom));
        tile.setInventorySlotContents(1, new ItemStack(Blocks.brown_mushroom));
        for (int i = 0; i < 4; i++) {
            tile.updateEntity();
        }
        ProductionFixture.require(
            drops.size() == 2 && drops.get(0)
                .getItem() == FoodListMF.burnt_food
                && drops.get(1)
                    .getItem() == Items.bowl,
            "Multi-input cooking or byproduct handling changed");
        place(true, 0);
        tile.setInventorySlotContents(0, new ItemStack(Items.paper));
        tile.setCookingTimeInSlot(0, 4);
        tile.updateEntity();
        ProductionFixture.require(drops.isEmpty() && tile.getStackInSlot(0) != null, "Empty fire cooked food");
    }

    private void persistence() {
        prepareCooking();
        tile.updateEntity();
        int before = fire.hotncold$getFuel();
        NBTTagCompound saved = new NBTTagCompound();
        tile.writeToNBT(saved);
        place(false, 0);
        tile.readFromNBT(saved);
        ProductionFixture.require(fire.hotncold$getFuel() == before, "Fuel did not survive tile NBT round-trip");
        ProductionFixture.require(
            ((IBasicMetre) tile).shouldShowMetre() && ((IBasicMetre) tile).getMetreScale(12000) == before,
            "MineFantasy fuel meter does not reflect stored fuel");
        saved.setInteger(MineFantasyCampfire.FUEL_TAG, Integer.MAX_VALUE);
        tile.readFromNBT(saved);
        ProductionFixture.require(fire.hotncold$getFuel() == 12000, "Excess NBT fuel was not clamped");
        saved.setInteger(MineFantasyCampfire.FUEL_TAG, -100);
        tile.readFromNBT(saved);
        ProductionFixture.require(fire.hotncold$getFuel() == 0, "Negative NBT fuel was not clamped");
        place(true, 0);
        tile.readFromNBTIfItExists(new NBTTagCompound());
        tile.updateEntity();
        ProductionFixture.require(!tile.isLit() && fire.hotncold$getFuel() == 0, "Old campfire gained free fuel");
    }

    private void disabled() {
        Config.enableMineFantasyCampfires = false;
        Config.campfireFoodBurnChance = 1;
        place(true, 0);
        ProductionFixture.require(!fire.hotncold$isFuelEnabled(), "Disabled switch was ignored");
        ItemStack sticks = new ItemStack(Items.stick, 2);
        click(sticks);
        ProductionFixture
            .require(sticks.stackSize == 2 && fire.hotncold$getFuel() == 0, "Disabled feature consumed fuel");
        tile.setInventorySlotContents(0, new ItemStack(Items.paper));
        for (int i = 0; i < 4; i++) {
            tile.updateEntity();
        }
        ProductionFixture.require(
            tile.isLit() && drops.size() == 1
                && drops.get(0)
                    .getItem() == Items.cooked_beef,
            "Disabled integration did not restore normal cooking");
    }

    @SubscribeEvent
    public void dropped(EntityJoinWorldEvent event) {
        if (event.world == world && event.entity instanceof EntityItem
            && Math.abs(event.entity.posX - 8.5) < 2
            && Math.abs(event.entity.posY - 200) < 3
            && Math.abs(event.entity.posZ - 8.5) < 2) {
            drops.add(
                ((EntityItem) event.entity).getEntityItem()
                    .copy());
            event.setCanceled(true);
        }
    }
}

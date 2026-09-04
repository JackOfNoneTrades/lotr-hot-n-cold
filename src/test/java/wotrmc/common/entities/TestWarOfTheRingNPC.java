package wotrmc.common.entities;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.world.World;

/** A non-animal in WOTR's package must never match the animal-only switch. */
public final class TestWarOfTheRingNPC extends EntityZombie {

    public TestWarOfTheRingNPC(World world) {
        super(world);
    }
}

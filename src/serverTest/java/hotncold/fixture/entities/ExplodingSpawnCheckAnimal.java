package hotncold.fixture.entities;

import net.minecraft.entity.passive.EntityCow;
import net.minecraft.world.World;

public final class ExplodingSpawnCheckAnimal extends EntityCow {

    private static boolean spawnCheckCalled;

    public ExplodingSpawnCheckAnimal(World world) {
        super(world);
    }

    @Override
    public boolean getCanSpawnHere() {
        spawnCheckCalled = true;
        throw new AssertionError("Blocked natural spawn reached the entity's spawn check");
    }

    public static void resetSpawnCheckCalled() {
        spawnCheckCalled = false;
    }

    public static boolean wasSpawnCheckCalled() {
        return spawnCheckCalled;
    }
}

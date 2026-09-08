package org.fentanylsolutions.hotncold.core;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Tags successful spawns at their LOTR call sites, without intercepting general world joins. */
public final class EquipmentSpawnTransformer implements IClassTransformer {

    private static final String HOOK = "org/fentanylsolutions/hotncold/compat/LOTREquipmentSpawns";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        String hook = hookFor(transformedName);
        if (bytes == null || hook == null) {
            return bytes;
        }
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        boolean changed = false;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) instruction;
                // MCP is used in development, SRG in the release jar. This is not a version fallback.
                if (call.getOpcode() == Opcodes.INVOKEVIRTUAL && call.owner.equals("net/minecraft/world/World")
                    && (call.name.equals("spawnEntityInWorld") || call.name.equals("func_72838_d"))
                    && call.desc.equals("(Lnet/minecraft/entity/Entity;)Z")) {
                    method.instructions.set(
                        call,
                        new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            HOOK,
                            hook,
                            "(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;)Z",
                            false));
                    changed = true;
                }
            }
        }
        if (!changed) {
            return bytes;
        }
        // Receiver + entity become two static arguments: stack shape and frames are unchanged.
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    static String hookFor(String name) {
        if ("lotr.common.item.LOTRItemSpawnEgg".equals(name)) {
            return "spawnFromEgg";
        }
        if ("lotr.common.entity.LOTREntityInvasionSpawner".equals(name)) {
            return "spawnFromInvasion";
        }
        if (name != null
            && (name.startsWith("lotr.common.world.structure.") || name.startsWith("lotr.common.world.structure2.")
                || name.equals("lotr.common.entity.LOTREntityNPCRespawner"))) {
            return "spawnFromStructure";
        }
        return null;
    }
}

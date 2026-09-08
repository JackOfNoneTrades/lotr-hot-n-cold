package org.fentanylsolutions.hotncold.core;

import static org.junit.Assert.*;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class EquipmentSpawnTransformerTest {

    @Test
    public void recognizesOnlyTheRequestedLOTRSources() {
        assertEquals("spawnFromEgg", EquipmentSpawnTransformer.hookFor("lotr.common.item.LOTRItemSpawnEgg"));
        assertEquals(
            "spawnFromInvasion",
            EquipmentSpawnTransformer.hookFor("lotr.common.entity.LOTREntityInvasionSpawner"));
        assertEquals(
            "spawnFromStructure",
            EquipmentSpawnTransformer.hookFor("lotr.common.entity.LOTREntityNPCRespawner"));
        assertEquals(
            "spawnFromStructure",
            EquipmentSpawnTransformer.hookFor("lotr.common.world.structure2.LOTRWorldGenStructureBase2"));
        assertEquals(
            "spawnFromStructure",
            EquipmentSpawnTransformer.hookFor("lotr.common.world.structure.LOTRWorldGenAngmarTower"));
        assertNull(EquipmentSpawnTransformer.hookFor("net.minecraft.world.World"));
        assertNull(EquipmentSpawnTransformer.hookFor("lotr.common.world.spawning.LOTRSpawnerNPCs"));
        assertNull(EquipmentSpawnTransformer.hookFor("lotr.common.command.LOTRCommandSummon"));
        assertNull(EquipmentSpawnTransformer.hookFor("wotrmc.common.WOTRMC"));
        assertNull(EquipmentSpawnTransformer.hookFor(null));
    }

    @Test
    public void replacesOnlySpawnInvocationWithoutChangingStackOrOtherCalls() {
        for (String spawnName : new String[] { "spawnEntityInWorld", "func_72838_d" }) {
            ClassWriter writer = new ClassWriter(0);
            writer.visit(
                Opcodes.V1_7,
                Opcodes.ACC_PUBLIC,
                "lotr/common/item/LOTRItemSpawnEgg",
                null,
                "java/lang/Object",
                null);
            org.objectweb.asm.MethodVisitor method = writer.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "spawn",
                "(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;)Z",
                null,
                null);
            method.visitCode();
            method.visitVarInsn(Opcodes.ALOAD, 0);
            method.visitVarInsn(Opcodes.ALOAD, 1);
            method.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/World",
                spawnName,
                "(Lnet/minecraft/entity/Entity;)Z",
                false);
            method.visitInsn(Opcodes.IRETURN);
            method.visitMaxs(2, 2);
            method.visitEnd();
            writer.visitEnd();
            EquipmentSpawnTransformer transformer = new EquipmentSpawnTransformer();
            byte[] original = writer.toByteArray();
            assertSame(original, transformer.transform("unrelated", "unrelated", original));
            byte[] result = transformer
                .transform("lotr.common.item.LOTRItemSpawnEgg", "lotr.common.item.LOTRItemSpawnEgg", original);
            ClassNode node = new ClassNode();
            new ClassReader(result).accept(node, 0);
            MethodNode patched = node.methods.get(0);
            MethodInsnNode call = (MethodInsnNode) patched.instructions.get(2);
            assertEquals(Opcodes.INVOKESTATIC, call.getOpcode());
            assertEquals("spawnFromEgg", call.name);
            assertEquals("(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;)Z", call.desc);
            assertEquals(4, patched.instructions.size());
            assertEquals(2, patched.maxStack);
            assertEquals(2, patched.maxLocals);
            assertSame(
                result,
                transformer
                    .transform("lotr.common.item.LOTRItemSpawnEgg", "lotr.common.item.LOTRItemSpawnEgg", result));
        }
    }
}

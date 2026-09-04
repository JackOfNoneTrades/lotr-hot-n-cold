package org.fentanylsolutions.hotncold.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class WarOfTheRingRestrictionTransformerTest {

    private static final String TARGET_CLASS = "wotrmc.common.map.WOTRMCBiomes";

    @Test
    public void replacesOnlyTheExactRestrictionMethod() {
        byte[] original = fixtureClass();

        byte[] transformed = new WarOfTheRingRestrictionTransformer().transform(TARGET_CLASS, TARGET_CLASS, original);

        Map<String, MethodNode> methods = methodsBySignature(transformed);
        assertOpcodes(methods.get("get()V"), Opcodes.RETURN);
        assertOpcodes(methods.get("get(I)V"), Opcodes.NOP, Opcodes.RETURN);
        assertOpcodes(methods.get("normal()I"), Opcodes.ICONST_1, Opcodes.IRETURN);
    }

    @Test
    public void leavesEveryOtherClassUntouched() {
        byte[] original = fixtureClass();

        byte[] transformed = new WarOfTheRingRestrictionTransformer()
            .transform("example.Unrelated", "example.Unrelated", original);

        assertSame(original, transformed);
    }

    private static byte[] fixtureClass() {
        ClassWriter writer = new ClassWriter(0);
        writer
            .visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, "wotrmc/common/map/WOTRMCBiomes", null, "java/lang/Object", null);
        addVoidMethod(writer, "get", "()V");
        addVoidMethod(writer, "get", "(I)V");
        org.objectweb.asm.MethodVisitor normal = writer
            .visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "normal", "()I", null, null);
        normal.visitCode();
        normal.visitInsn(Opcodes.ICONST_1);
        normal.visitInsn(Opcodes.IRETURN);
        normal.visitMaxs(1, 0);
        normal.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void addVoidMethod(ClassWriter writer, String name, String descriptor) {
        org.objectweb.asm.MethodVisitor method = writer
            .visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, name, descriptor, null, null);
        method.visitCode();
        method.visitInsn(Opcodes.NOP);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, descriptor.equals("()V") ? 0 : 1);
        method.visitEnd();
    }

    private static Map<String, MethodNode> methodsBySignature(byte[] classBytes) {
        ClassNode classNode = new ClassNode();
        new ClassReader(classBytes).accept(classNode, 0);
        Map<String, MethodNode> methods = new HashMap<>();
        for (MethodNode method : classNode.methods) {
            methods.put(method.name + method.desc, method);
        }
        return methods;
    }

    private static void assertOpcodes(MethodNode method, int... expected) {
        int index = 0;
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction
            != null; instruction = instruction.getNext()) {
            if (instruction.getOpcode() >= 0) {
                assertEquals(expected[index++], instruction.getOpcode());
            }
        }
        assertEquals(expected.length, index);
    }
}

package org.fentanylsolutions.hotncold.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

final class WarOfTheRingRestrictionTransformer implements IClassTransformer {

    private static final Map<String, String> RESTRICTION_METHODS;

    static {
        Map<String, String> methods = new LinkedHashMap<>();
        methods.put("wotrmc.common.map.WOTRMCBiomes", "get");
        methods.put("wotrmc.common.items.WOTRMCNewBlocks", "moreBlocks");
        methods.put("wotrmc.common.items.blocks.WOTRMCNewBrick", "moreBlocks");
        methods.put("wotrmc.common.loading.events.WOTRMCEventLoader", "could");
        methods.put("wotrmc.common.WOTRMCFactions", "fileReader");
        methods.put("wotrmc.common.WOTRMCRecipes", "fileReader");
        methods.put("wotrmc.common.items.WOTRMCItems", "using");
        methods.put("wotrmc.common.achievements.WOTRMCAchievement", "export");
        methods.put("wotrmc.common.achievements.WOTRMCServerAchievement", "export");
        RESTRICTION_METHODS = Collections.unmodifiableMap(methods);
    }

    private final Set<String> patchedClasses = new LinkedHashSet<>();

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        String className = RESTRICTION_METHODS.containsKey(transformedName) ? transformedName : name;
        String methodName = RESTRICTION_METHODS.get(className);
        if (methodName == null || basicClass == null) {
            return basicClass;
        }

        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);
        int patchedMethods = 0;
        for (MethodNode method : classNode.methods) {
            if (methodName.equals(method.name) && "()V".equals(method.desc)) {
                method.instructions = returnImmediately();
                method.tryCatchBlocks.clear();
                if (method.localVariables != null) {
                    method.localVariables.clear();
                }
                method.maxStack = 0;
                method.maxLocals = 0;
                patchedMethods++;
            }
        }
        if (patchedMethods != 1) {
            throw new IllegalStateException(
                "Expected one War of the Ring restriction method " + className
                    + "."
                    + methodName
                    + "()V, found "
                    + patchedMethods);
        }

        patchedClasses.add(className);
        ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static InsnList returnImmediately() {
        InsnList instructions = new InsnList();
        instructions.add(new InsnNode(Opcodes.RETURN));
        return instructions;
    }

    static Set<String> targetClassNames() {
        return RESTRICTION_METHODS.keySet();
    }

    void verifyEveryRestrictionWasRemoved() {
        if (!patchedClasses.equals(RESTRICTION_METHODS.keySet())) {
            Set<String> missingClasses = new LinkedHashSet<>(RESTRICTION_METHODS.keySet());
            missingClasses.removeAll(patchedClasses);
            throw new IllegalStateException("Failed to remove War of the Ring restrictions from " + missingClasses);
        }
    }
}

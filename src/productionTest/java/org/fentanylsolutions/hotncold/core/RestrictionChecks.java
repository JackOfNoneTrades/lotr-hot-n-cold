package org.fentanylsolutions.hotncold.core;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;

import cpw.mods.fml.common.Loader;
import hotncold.production.ProductionFixture;

/** Checks the genuine addon bytecode, not a simplified imitation of its classes. */
public final class RestrictionChecks {

    private RestrictionChecks() {}

    public static void run() throws Exception {
        WarOfTheRingRestrictionTransformer transformer = new WarOfTheRingRestrictionTransformer();
        int untouchedMethods = 0;
        try (ZipFile zip = new ZipFile(
            Loader.instance()
                .getIndexedModList()
                .get("wotrmc")
                .getSource())) {
            for (String name : WarOfTheRingRestrictionTransformer.targetClassNames()) {
                byte[] original;
                try (InputStream stream = zip.getInputStream(zip.getEntry(name.replace('.', '/') + ".class"))) {
                    original = IOUtils.toByteArray(stream);
                }
                ClassNode before = read(original);
                ClassNode after = read(transformer.transform(name, name, original));
                ProductionFixture
                    .require(before.methods.size() == after.methods.size(), "Restriction patch changed method count");
                int changed = 0;
                for (int i = 0; i < before.methods.size(); i++) {
                    MethodNode oldMethod = before.methods.get(i);
                    MethodNode newMethod = after.methods.get(i);
                    if ("()V".equals(newMethod.desc) && newMethod.instructions.size() == 1
                        && newMethod.instructions.getFirst()
                            .getOpcode() == org.objectweb.asm.Opcodes.RETURN
                        && oldMethod.instructions.size() > 1) {
                        // Put back only the removed restriction, then compare the entire remaining class.
                        after.methods.set(i, oldMethod);
                        changed++;
                    } else {
                        untouchedMethods++;
                    }
                }
                ProductionFixture.require(changed == 1, "Expected one restriction in " + name);
                ProductionFixture
                    .require(describe(before).equals(describe(after)), "Non-restriction bytecode changed in " + name);
            }
        }
        transformer.verifyEveryRestrictionWasRemoved();
        ProductionFixture.LOG.info(
            "PRODUCTION_RESTRICTION_PASSED: nine genuine classes; {} other methods and all fields unchanged",
            untouchedMethods);
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static String describe(ClassNode node) {
        StringWriter text = new StringWriter();
        node.accept(new TraceClassVisitor(new PrintWriter(text)));
        return text.toString();
    }
}

package com.xluke44.DeltaSkinLoader.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class DeltaSkinLoaderTransformer implements IClassTransformer {
    private static final String TARGET = "net.minecraft.client.entity.AbstractClientPlayer";
    private static final String RUNTIME = "com/xluke44/DeltaSkinLoader/DeltaSkinLoaderRuntime";

    private static final Set<String> SKIN_METHODS = new HashSet<String>(Arrays.asList(
            "getLocationSkin", "func_110306_p", "m"));
    private static final Set<String> CAPE_METHODS = new HashSet<String>(Arrays.asList(
            "getLocationCape", "func_110303_q", "q"));
    private static final Set<String> ELYTRA_METHODS = new HashSet<String>(Arrays.asList(
            "getLocationElytra", "func_184834_t", "s"));
    private static final Set<String> MODEL_METHODS = new HashSet<String>(Arrays.asList(
            "getSkinType", "func_175154_l", "t"));

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !(TARGET.equals(transformedName) || TARGET.equals(name))) {
            return basicClass;
        }

        try {
            ClassReader reader = new ClassReader(basicClass);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            boolean changed = false;

            for (MethodNode method : classNode.methods) {
                String hook = null;
                if (SKIN_METHODS.contains(method.name)) {
                    hook = "getSkinOverride";
                } else if (CAPE_METHODS.contains(method.name)) {
                    hook = "getCapeOverride";
                } else if (ELYTRA_METHODS.contains(method.name)) {
                    hook = "getElytraOverride";
                } else if (MODEL_METHODS.contains(method.name)) {
                    hook = "getModelOverride";
                }

                if (hook != null && (method.access & Opcodes.ACC_STATIC) == 0
                        && Type.getArgumentTypes(method.desc).length == 0
                        && method.instructions != null && method.instructions.size() > 0) {
                    Type returnType = Type.getReturnType(method.desc);
                    if (returnType.getSort() == Type.OBJECT) {

                        if (SKIN_METHODS.contains(method.name)) {
                            insertSkinReturnHook(classNode.name, method, returnType.getDescriptor());
                        }
                        insertNullableReturnHook(classNode.name, method, hook, returnType.getDescriptor());
                        changed = true;
                    }
                }
            }

            if (!changed) {
                return basicClass;
            }

            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            classNode.accept(writer);
            return writer.toByteArray();
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to install DeltaSkinLoader player lookup hooks in "
                    + transformedName, throwable);
        }
    }

    private static void insertSkinReturnHook(String targetInternalName, MethodNode method,
                                             String returnDescriptor) {
        for (org.objectweb.asm.tree.AbstractInsnNode instruction = method.instructions.getFirst();
             instruction != null; ) {
            org.objectweb.asm.tree.AbstractInsnNode next = instruction.getNext();
            if (instruction.getOpcode() == Opcodes.ARETURN) {
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                hook.add(new InsnNode(Opcodes.SWAP));
                String descriptor = "(L" + targetInternalName + ";" + returnDescriptor + ")" + returnDescriptor;
                hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RUNTIME,
                        "getSkinReturnOverride", descriptor, false));
                method.instructions.insertBefore(instruction, hook);
            }
            instruction = next;
        }
    }

    private static void insertNullableReturnHook(String targetInternalName, MethodNode method,
                                                 String hookName, String returnDescriptor) {
        LabelNode continueOriginal = new LabelNode();
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        String descriptor = "(L" + targetInternalName + ";)" + returnDescriptor;
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RUNTIME, hookName, descriptor, false));
        hook.add(new InsnNode(Opcodes.DUP));
        hook.add(new JumpInsnNode(Opcodes.IFNULL, continueOriginal));
        hook.add(new InsnNode(Opcodes.ARETURN));
        hook.add(continueOriginal);
        hook.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1,
                new Object[] {Type.getType(returnDescriptor).getInternalName()}));
        hook.add(new InsnNode(Opcodes.POP));
        method.instructions.insert(hook);
    }
}

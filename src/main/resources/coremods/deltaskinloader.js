var Opcodes = Java.type("org.objectweb.asm.Opcodes");
var Type = Java.type("org.objectweb.asm.Type");
var InsnList = Java.type("org.objectweb.asm.tree.InsnList");
var InsnNode = Java.type("org.objectweb.asm.tree.InsnNode");
var VarInsnNode = Java.type("org.objectweb.asm.tree.VarInsnNode");
var MethodInsnNode = Java.type("org.objectweb.asm.tree.MethodInsnNode");
var ASMAPI = Java.type("net.minecraftforge.coremod.api.ASMAPI");

var RUNTIME = "com/xluke44/DeltaSkinLoader/DeltaSkinLoaderRuntime";
var RESOURCE_LOCATION_DESC = "Lnet/minecraft/resources/ResourceLocation;";

function mappedMethodName(srgName) {
    try {
        return String(ASMAPI.mapMethod(srgName));
    } catch (ignored) {
        return srgName;
    }
}

function nameMatches(name, candidates) {
    for (var i = 0; i < candidates.length; i++) {
        if (name === candidates[i]) {
            return true;
        }
    }
    return false;
}

function insertReturnHook(targetInternalName, method, hookName, returnDescriptor) {
    // Object returns use ARETURN; boolean returns use IRETURN. Both are
    // category-1 values, so the same ALOAD/SWAP call sequence is valid.
    var returnOpcode = returnDescriptor === "Z" ? Opcodes.IRETURN : Opcodes.ARETURN;
    var instruction = method.instructions.getFirst();
    while (instruction !== null) {
        var next = instruction.getNext();
        if (instruction.getOpcode() === returnOpcode) {
            var hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new InsnNode(Opcodes.SWAP));
            var descriptor = "(L" + targetInternalName + ";" + returnDescriptor + ")" + returnDescriptor;
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RUNTIME, hookName, descriptor, false));
            method.instructions.insertBefore(instruction, hook);
            if (method.maxStack < 2) {
                method.maxStack = 2;
            }
        }
        instruction = next;
    }
}

function initializeCoreMod() {
    var skinNames = ["getSkinTextureLocation", "m_108560_", "d", mappedMethodName("m_108560_")];
    var capeNames = ["getCloakTextureLocation", "m_108561_", "e", mappedMethodName("m_108561_")];
    var elytraNames = ["getElytraTextureLocation", "m_108563_", "l", mappedMethodName("m_108563_")];
    var modelNames = ["getModelName", "m_108564_", "n", mappedMethodName("m_108564_")];
    var capeLoadedNames = ["isCapeLoaded", "m_108555_", "a", mappedMethodName("m_108555_")];
    var elytraLoadedNames = ["isElytraLoaded", "m_108562_", "g", mappedMethodName("m_108562_")];

    return {
        "DeltaSkinLoader player texture hooks": {
            "target": {
                "type": "CLASS",
                "name": "net.minecraft.client.player.AbstractClientPlayer"
            },
            "transformer": function(classNode) {
                var changed = false;
                for (var i = 0; i < classNode.methods.size(); i++) {
                    var method = classNode.methods.get(i);
                    if ((method.access & Opcodes.ACC_STATIC) !== 0
                            || Type.getArgumentTypes(method.desc).length !== 0
                            || method.instructions === null
                            || method.instructions.size() === 0) {
                        continue;
                    }

                    var methodName = String(method.name);
                    var returnDescriptor = Type.getReturnType(method.desc).getDescriptor();
                    var hookName = null;

                    if (returnDescriptor === RESOURCE_LOCATION_DESC && nameMatches(methodName, skinNames)) {
                        hookName = "getSkinReturnOverride";
                    } else if (returnDescriptor === RESOURCE_LOCATION_DESC && nameMatches(methodName, capeNames)) {
                        hookName = "getCapeReturnOverride";
                    } else if (returnDescriptor === RESOURCE_LOCATION_DESC && nameMatches(methodName, elytraNames)) {
                        hookName = "getElytraReturnOverride";
                    } else if (returnDescriptor === "Ljava/lang/String;" && nameMatches(methodName, modelNames)) {
                        hookName = "getModelReturnOverride";
                    } else if (returnDescriptor === "Z" && nameMatches(methodName, capeLoadedNames)) {
                        hookName = "getCapeLoadedReturnOverride";
                    } else if (returnDescriptor === "Z" && nameMatches(methodName, elytraLoadedNames)) {
                        hookName = "getElytraLoadedReturnOverride";
                    }

                    if (hookName !== null) {
                        insertReturnHook(classNode.name, method, hookName, returnDescriptor);
                        changed = true;
                    }
                }
                return classNode;
            }
        }
    };
}

/*
 * The MIT License (MIT)
 *
 * Copyright (c) Despector <https://despector.voxelgenesis.com>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.despector.decompiler.kotlin.step;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.despector.ast.AccessModifier;
import org.spongepowered.despector.ast.Annotation;
import org.spongepowered.despector.ast.Locals;
import org.spongepowered.despector.ast.stmt.StatementBlock;
import org.spongepowered.despector.ast.stmt.StatementBlock.Type;
import org.spongepowered.despector.ast.stmt.misc.Comment;
import org.spongepowered.despector.ast.type.MethodEntry;
import org.spongepowered.despector.ast.type.TypeEntry;
import org.spongepowered.despector.config.ConfigManager;
import org.spongepowered.despector.decompiler.BaseDecompiler;
import org.spongepowered.despector.decompiler.DecompilerStep;
import org.spongepowered.despector.decompiler.method.MethodDecompiler;
import org.spongepowered.despector.util.AstUtil;
import org.spongepowered.despector.util.SignatureParser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A decompilation step for decompiling kotlin methods.
 */
public class KotlinMethodInfoStep implements DecompilerStep {

    private MethodDecompiler method_decomp;

    public KotlinMethodInfoStep(MethodDecompiler decomp) {
        this.method_decomp = decomp;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process(ClassNode cn, TypeEntry entry) {
        // Find all methods
        for (MethodNode mn : (List<MethodNode>) cn.methods) {
            MethodEntry m = new MethodEntry(entry.getSource());
            m.setAbstract((mn.access & ACC_ABSTRACT) != 0);
            m.setAccessModifier(AccessModifier.fromModifiers(mn.access));
            m.setFinal((mn.access & ACC_FINAL) != 0);
            m.setName(mn.name);
            m.setOwner(cn.name);
            m.setDescription(mn.desc);
            m.setStatic((mn.access & ACC_STATIC) != 0);
            m.setSynthetic((mn.access & ACC_SYNTHETIC) != 0);

            if (mn.signature != null) {
                m.setMethodSignature(SignatureParser.parseMethod(mn.signature));
            } else {
                m.setMethodSignature(SignatureParser.parseMethod(mn.desc));
            }

            if (mn.visibleAnnotations != null) {
                for (AnnotationNode an : (List<AnnotationNode>) mn.visibleAnnotations) {
                    Annotation anno = BaseDecompiler.createAnnotation(entry.getSource(), an);
                    anno.getType().setRuntimeVisible(true);
                    m.addAnnotation(anno);
                }
            }
            if (mn.invisibleAnnotations != null) {
                for (AnnotationNode an : (List<AnnotationNode>) mn.invisibleAnnotations) {
                    Annotation anno = BaseDecompiler.createAnnotation(entry.getSource(), an);
                    anno.getType().setRuntimeVisible(false);
                    m.addAnnotation(anno);
                }
            }

            Locals locals = this.method_decomp.createLocals(m, mn);
            try {
                StatementBlock insns = this.method_decomp.decompile(m, mn, locals);
                m.setInstructions(insns);
            } catch (Exception ex) {
                System.err.println("Error decompiling method body for " + cn.name + " " + m.toString());
                ex.printStackTrace();
                StatementBlock insns = new StatementBlock(Type.METHOD, locals);
                if (ConfigManager.getConfig().print_opcodes_on_error) {
                    List<String> text = new ArrayList<>();
                    text.add("Error decompiling block");
                    for (Iterator<AbstractInsnNode> it = mn.instructions.iterator(); it.hasNext();) {
                        AbstractInsnNode next = it.next();
                        text.add(AstUtil.insnToString(next));
                    }
                    insns.append(new Comment(text));
                } else {
                    insns.append(new Comment("Error decompiling block"));
                }
                m.setInstructions(insns);
            }
            entry.addMethod(m);
        }
    }

}

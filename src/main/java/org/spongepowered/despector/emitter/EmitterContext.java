/*
 * The MIT License (MIT)
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
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
package org.spongepowered.despector.emitter;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.spongepowered.despector.ast.AstEntry;
import org.spongepowered.despector.ast.Locals.LocalInstance;
import org.spongepowered.despector.ast.members.MethodEntry;
import org.spongepowered.despector.ast.members.insn.Statement;
import org.spongepowered.despector.ast.members.insn.StatementBlock;
import org.spongepowered.despector.ast.members.insn.arg.Instruction;
import org.spongepowered.despector.ast.members.insn.branch.condition.Condition;
import org.spongepowered.despector.ast.members.insn.misc.Return;
import org.spongepowered.despector.ast.type.TypeEntry;
import org.spongepowered.despector.emitter.format.EmitterFormat;
import org.spongepowered.despector.util.TypeHelper;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class EmitterContext {

    private final EmitterSet set;

    private EmitterFormat format;
    private Writer output;
    private StringBuilder buffer = null;
    private Set<LocalInstance> defined_locals = Sets.newHashSet();
    private Set<String> imports = null;
    private TypeEntry type = null;
    private MethodEntry method;

    private int indentation = 0;

    public EmitterContext(EmitterSet set, Writer output, EmitterFormat format) {
        this.set = set;
        this.output = output;
        this.format = format;

        this.imports = Sets.newHashSet();
    }

    public EmitterSet getEmitterSet() {
        return this.set;
    }

    public TypeEntry getType() {
        return this.type;
    }

    public void setType(TypeEntry type) {
        this.type = type;
    }

    public MethodEntry getMethod() {
        return this.method;
    }

    public void setMethod(MethodEntry mth) {
        this.method = mth;
    }

    public EmitterFormat getFormat() {
        return this.format;
    }

    public boolean isDefined(LocalInstance local) {
        return this.defined_locals.contains(local);
    }

    public void markDefined(LocalInstance local) {
        this.defined_locals.add(local);
    }

    @SuppressWarnings("unchecked")
    public <T extends AstEntry> boolean emit(T obj) {
        AstEmitter<T> emitter = (AstEmitter<T>) this.set.getAstEmitter(obj.getClass());
        if (emitter == null) {
            throw new IllegalArgumentException("No emitter for ast entry " + obj.getClass().getName());
        }
        return emitter.emit(this, obj);
    }

    public void emitBody(StatementBlock instructions) {
        if (instructions == null) {
            printIndentation();
            printString("// Error decompiling block");
            return;
        }
        this.defined_locals.clear();
        boolean last_success = false;
        for (int i = 0; i < instructions.getStatements().size(); i++) {
            Statement insn = instructions.getStatements().get(i);
            if (insn instanceof Return && !((Return) insn).getValue().isPresent()
                    && instructions.getType() == StatementBlock.Type.METHOD && i == instructions.getStatements().size() - 1) {
                break;
            }
            if (last_success) {
                printString("\n");
            }
            printIndentation();
            emit(insn, true);
            last_success = true;
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Statement> void emit(T obj, boolean semicolon) {
        StatementEmitter<T> emitter = (StatementEmitter<T>) this.set.getStatementEmitter(obj.getClass());
        if (emitter == null) {
            throw new IllegalArgumentException("No emitter for statement " + obj.getClass().getName());
        }
        emitter.emit(this, obj, semicolon);
    }

    @SuppressWarnings("unchecked")
    public <T extends Instruction> void emit(T obj, String type) {
        InstructionEmitter<T> emitter = (InstructionEmitter<T>) this.set.getInstructionEmitter(obj.getClass());
        if (emitter == null) {
            throw new IllegalArgumentException("No emitter for instruction " + obj.getClass().getName());
        }
        emitter.emit(this, obj, type);
    }

    @SuppressWarnings("unchecked")
    public <T extends Condition> void emit(T condition) {
        ConditionEmitter<T> emitter = (ConditionEmitter<T>) this.set.getConditionEmitter(condition.getClass());
        if (emitter == null) {
            throw new IllegalArgumentException("No emitter for condition " + condition.getClass().getName());
        }
        emitter.emit(this, condition);
    }

    public void indent() {
        this.indentation++;
    }

    public void dedent() {
        this.indentation--;
    }

    public void printIndentation() {
        if (this.format.indent_with_spaces) {
            for (int i = 0; i < this.indentation * this.format.indentation_size; i++) {
                printString(" ");
            }
        } else {
            for (int i = 0; i < this.indentation; i++) {
                printString("\t");
            }
        }
    }

    public void emitType(String name) {
        emitTypeClassName(TypeHelper.descToType(name).replace('/', '.'));
    }

    public void emitTypeName(String name) {
        emitTypeClassName(name.replace('/', '.'));
    }

    public void emitTypeClassName(String name) {
        if (name.endsWith("[]")) {
            emitTypeClassName(name.substring(0, name.length() - 2));
            printString("[]");
            return;
        }
        if (name.indexOf('.') != -1) {
            if (name.startsWith("java.lang.")) {
                name = name.substring("java.lang.".length());
            } else if (this.type != null) {
                String this_package = "";
                String target_package = name;
                String this$name = this.type.getName().replace('/', '.');
                if (this$name.indexOf('.') != -1) {
                    this_package = this$name.substring(0, this$name.lastIndexOf('.'));
                    target_package = name.substring(0, name.lastIndexOf('.'));
                }
                if (this_package.equals(target_package)) {
                    name = name.substring(name.lastIndexOf('.') + 1);
                } else if (checkImport(name)) {
                    name = name.substring(name.lastIndexOf('.') + 1);
                }
            } else if (checkImport(name)) {
                name = name.substring(name.lastIndexOf('.') + 1);
            }
        }
        printString(name.replace('$', '.'));
    }

    public void printString(String line) {
        if (this.buffer == null) {
            try {
                this.output.write(line);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            this.buffer.append(line);
        }
    }

    public boolean checkImport(String type) {
        if (type.indexOf('$') != -1) {
            type = type.substring(0, type.indexOf('$'));
        }
        if (this.imports == null) {
            return false;
        }
        if (TypeHelper.isPrimative(type)) {
            return true;
        }
        this.imports.add(type);
        return true;
    }

    public void resetImports() {
        this.imports.clear();
    }

    public void emitImports() {
        List<String> imports = Lists.newArrayList(this.imports);
        for (String group : this.format.import_order) {
            if (group.startsWith("/#")) {
                // don't have static imports yet
                continue;
            }
            List<String> group_imports = Lists.newArrayList();
            for (Iterator<String> it = imports.iterator(); it.hasNext();) {
                String import_ = it.next();
                if (import_.startsWith(group)) {
                    group_imports.add(import_);
                    it.remove();
                }
            }
            Collections.sort(group_imports);
            for (String import_ : group_imports) {
                printString("import ");
                printString(import_);
                printString(";\n");
            }
            if (!group_imports.isEmpty()) {
                for (int i = 0; i < this.format.blank_lines_between_import_groups; i++) {
                    printString("\n");
                }
            }
        }
    }

    public void enableBuffer() {
        this.buffer = new StringBuilder();
    }

    public void outputBuffer() {

        // Then once we have finished emitting the class contents we detach the
        // buffer and then sort and emit the imports into the actual output and
        // then finally replay the buffer into the output.

        StringBuilder buf = this.buffer;
        this.buffer = null;

        String pkg = this.type.getName();
        int last = pkg.lastIndexOf('.');
        if (last != -1) {
            for (int i = 0; i < this.format.blank_lines_before_package; i++) {
                printString("\n");
            }
            pkg = pkg.substring(0, last);
            printString("package ");
            printString(pkg);
            printString(";\n");
        }
        int lines = Math.max(this.format.blank_lines_after_package, this.format.blank_lines_before_imports);
        for (int i = 0; i < lines; i++) {
            printString("\n");
        }

        emitImports();

        if (!this.imports.isEmpty()) {
            for (int i = 0; i < this.format.blank_lines_after_imports; i++) {
                printString("\n");
            }
        }

        // Replay the buffer.
        try {
            this.output.write(buf.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.imports = null;
        this.buffer = null;
    }

}
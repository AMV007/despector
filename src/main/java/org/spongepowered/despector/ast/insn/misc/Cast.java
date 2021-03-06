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
package org.spongepowered.despector.ast.insn.misc;

import static com.google.common.base.Preconditions.checkNotNull;

import org.spongepowered.despector.ast.AstVisitor;
import org.spongepowered.despector.ast.generic.TypeSignature;
import org.spongepowered.despector.ast.insn.Instruction;
import org.spongepowered.despector.ast.insn.InstructionVisitor;
import org.spongepowered.despector.util.serialization.AstSerializer;
import org.spongepowered.despector.util.serialization.MessagePacker;

import java.io.IOException;

/**
 * An instruction which casts the inner value to a specific type.
 * 
 * <p>Example: (type) (val)</p>
 */
public class Cast implements Instruction {

    private TypeSignature type;
    private Instruction val;

    public Cast(TypeSignature type, Instruction val) {
        this.type = checkNotNull(type, "type");
        this.val = checkNotNull(val, "val");
    }

    /**
     * Gets the type which the valye is casted to.
     */
    public TypeSignature getType() {
        return this.type;
    }

    /**
     * Sets the type which the valye is casted to.
     */
    public void setType(TypeSignature type) {
        this.type = checkNotNull(type, "type");
    }

    /**
     * Gets the instruction for the type being cast.
     */
    public Instruction getValue() {
        return this.val;
    }

    /**
     * Sets the instruction for the type being cast.
     */
    public void setValue(Instruction val) {
        this.val = checkNotNull(val, "val");
    }

    @Override
    public TypeSignature inferType() {
        return this.type;
    }

    @Override
    public void accept(AstVisitor visitor) {
        if (visitor instanceof InstructionVisitor) {
            ((InstructionVisitor) visitor).visitCast(this);
            this.val.accept(visitor);
        }
    }

    @Override
    public void writeTo(MessagePacker pack) throws IOException {
        pack.startMap(3);
        pack.writeString("id").writeInt(AstSerializer.STATEMENT_ID_CAST);
        pack.writeString("value");
        this.val.writeTo(pack);
        pack.writeString("type");
        this.type.writeTo(pack);
        pack.endMap();
    }

    @Override
    public String toString() {
        return "((" + this.type.getName() + ") " + this.val + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Cast)) {
            return false;
        }
        Cast cast = (Cast) obj;
        return this.type.equals(cast.type) && this.val.equals(cast.val);
    }

    @Override
    public int hashCode() {
        int h = 1;
        h = h * 37 + this.type.hashCode();
        h = h * 37 + this.val.hashCode();
        return h;
    }

}

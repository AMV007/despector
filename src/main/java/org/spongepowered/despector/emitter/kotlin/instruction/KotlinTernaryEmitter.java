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
package org.spongepowered.despector.emitter.kotlin.instruction;

import org.spongepowered.despector.ast.generic.ClassTypeSignature;
import org.spongepowered.despector.ast.generic.TypeSignature;
import org.spongepowered.despector.ast.insn.condition.CompareCondition;
import org.spongepowered.despector.ast.insn.misc.Ternary;
import org.spongepowered.despector.emitter.java.JavaEmitterContext;
import org.spongepowered.despector.emitter.java.instruction.TernaryEmitter;
import org.spongepowered.despector.emitter.kotlin.special.KotlinRangeEmitter;

/**
 * An emitter for kotlin ternaries.
 */
public class KotlinTernaryEmitter extends TernaryEmitter {

    @Override
    public void emit(JavaEmitterContext ctx, Ternary ternary, TypeSignature type) {
        if (type == ClassTypeSignature.BOOLEAN && checkBooleanExpression(ctx, ternary)) {
            return;
        }
        ctx.printString("if (");
        if (ternary.getCondition() instanceof CompareCondition) {
            ctx.emit(ternary.getCondition());
        } else {
            ctx.emit(ternary.getCondition());
        }
        ctx.printString(") ");
        ctx.markWrapPoint();
        ctx.emit(ternary.getTrueValue(), type);
        ctx.markWrapPoint();
        ctx.printString(" else ");
        ctx.emit(ternary.getFalseValue(), type);
    }

    @Override
    public boolean checkBooleanExpression(JavaEmitterContext ctx, Ternary ternary) {
        if (KotlinRangeEmitter.checkRangeTernary(ctx, ternary)) {
            return true;
        }
        return super.checkBooleanExpression(ctx, ternary);
    }

}

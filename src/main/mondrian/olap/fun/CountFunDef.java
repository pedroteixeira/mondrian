/*
// $Id$
// This software is subject to the terms of the Common Public License
// Agreement, available at the following URL:
// http://www.opensource.org/licenses/cpl.html.
// Copyright (C) 2006-2006 Julian Hyde
// All Rights Reserved.
// You must accept the terms of that agreement to use this software.
*/
package mondrian.olap.fun;

import mondrian.olap.FunDef;
import mondrian.olap.Literal;
import mondrian.olap.Evaluator;
import mondrian.olap.Dimension;
import mondrian.calc.Calc;
import mondrian.calc.ExpCompiler;
import mondrian.calc.ListCalc;
import mondrian.calc.impl.AbstractIntegerCalc;
import mondrian.mdx.ResolvedFunCall;

import java.util.List;

/**
 * Definition of the <code>Count</code> MDX function.
 *
 * @author jhyde
 * @version $Id$
 * @since Mar 23, 2006
 */
class CountFunDef extends AbstractAggregateFunDef {
    static final String[] ReservedWords = new String[] {"INCLUDEEMPTY", "EXCLUDEEMPTY"};

    static final ReflectiveMultiResolver Resolver = new ReflectiveMultiResolver(
            "Count",
            "Count(<Set>[, EXCLUDEEMPTY | INCLUDEEMPTY])",
            "Returns the number of tuples in a set, empty cells included unless the optional EXCLUDEEMPTY flag is used.",
            new String[]{"fnx", "fnxy"},
            CountFunDef.class,
            ReservedWords);

    public CountFunDef(FunDef dummyFunDef) {
        super(dummyFunDef);
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final ListCalc memberListCalc =
                compiler.compileList(call.getArg(0));
        final boolean includeEmpty =
                call.getArgCount() < 2 ||
                ((Literal) call.getArg(1)).getValue().equals(
                        "INCLUDEEMPTY");
        return new AbstractIntegerCalc(
                call, new Calc[] {memberListCalc}) {
            public int evaluateInteger(Evaluator evaluator) {
                List memberList =
                        memberListCalc.evaluateList(evaluator);
                return count(evaluator, memberList, includeEmpty);
            }

            public boolean dependsOn(Dimension dimension) {
                // COUNT(<set>, INCLUDEEMPTY) is straightforward -- it
                // depends only on the dimensions that <Set> depends
                // on.
                if (super.dependsOn(dimension)) {
                    return true;
                }
                if (includeEmpty) {
                    return false;
                }
                // COUNT(<set>, EXCLUDEEMPTY) depends only on the
                // dimensions that <Set> depends on, plus all
                // dimensions not masked by the set.
                if (memberListCalc.getType().usesDimension(dimension, true) ) {
                    return false;
                }
                return true;
            }
        };
    }
}

// End CountFunDef.java
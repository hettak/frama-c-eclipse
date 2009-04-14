package net.eclipse.why.editeur.lexer.ast;

import net.eclipse.why.editeur.WhyCode;
import net.eclipse.why.editeur.WhyElement;

public class LExprLess extends LExpr implements Visitable {

	Loc lo; LExpr l; LExpr m;
	
	public LExprLess(Loc loc, LExpr l1, LExpr l2) {
		lo = loc;
		l = l1;
		m = l2;
	}

	public void accept(ReflectiveVisitor visitor) {
		visitor.visit(l);
		WhyElement.add(" - ");
		if(m instanceof LExprPar)
			//WhyElement.par = true;
			((LExprPar)m).p = true;
//		} else {
//			WhyElement.par = false;
//		}
		visitor.visit(m);
	}

	public void saccept(ReflectiveVisitor visitor) {
		visitor.svisit(l);
		WhyCode.add("-");
		visitor.svisit(m);
	}

}

package net.eclipse.why.editeur.lexer.ast;

import java.util.Vector;

import net.eclipse.why.editeur.WhyCode;
import net.eclipse.why.editeur.WhyElement;

public class List1PType implements Visitable {

	Vector<PType> v;
	
	public List1PType() {
		v = new Vector<PType>();
	}

	public void add(PType p) {
		v.add(p);
	}
	
	public void accept(ReflectiveVisitor visitor) {
		visitor.visit(v.get(0));
		for(int e=1; e<v.size(); e++) {
			WhyElement.add(", ");
			visitor.visit(v.get(e));
		}
	}

	public void saccept(ReflectiveVisitor visitor) {
		visitor.svisit(v.get(0));
		for(int e=1; e<v.size(); e++) {
			WhyCode.add(",");
			visitor.svisit(v.get(e));
		}
	}

}

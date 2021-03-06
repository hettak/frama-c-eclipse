package net.eclipse.why.editeur.lexer.ast;

public class LExprInteger extends LExpr implements Visitable {

	String integer;
	
	public LExprInteger(String i) {
		integer = i;
	}

	public void accept(ReflectiveVisitor visitor) {
		visitor.visit(integer);
	}

	public void saccept(ReflectiveVisitor visitor) {
		visitor.svisit(integer);
	}

}

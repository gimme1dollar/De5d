package gr.uom.java.ast.decomposition.cfg;

import java.util.Set;

import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class PDGExitNode extends PDGStatementNode {
	private VariableDeclaration returnedVariable;
	
	public PDGExitNode(CFGNode cfgNode, Set<VariableDeclaration> variableDeclarationsInMethod) {
		super(cfgNode, variableDeclarationsInMethod);
		if(cfgNode instanceof CFGExitNode) {
			CFGExitNode exitNode = (CFGExitNode)cfgNode;
			SimpleName returnedVariableSimpleName = exitNode.getReturnedVariable();
			if(returnedVariableSimpleName != null) {
				for(VariableDeclaration declaration : variableDeclarationsInMethod) {
					if(declaration.resolveBinding().isEqualTo(returnedVariableSimpleName.resolveBinding())) {
						returnedVariable = declaration;
						break;
					}
				}
			}
		}
	}

	public VariableDeclaration getReturnedVariable() {
		return returnedVariable;
	}
}
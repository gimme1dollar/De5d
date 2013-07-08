package gr.uom.java.ast.decomposition;

public enum DifferenceType {
	AST_TYPE_MISMATCH,
	ARGUMENT_NUMBER_MISMATCH,
	ARRAY_DIMENSION_MISMATCH,
	VARIABLE_TYPE_MISMATCH,
	VARIABLE_NAME_MISMATCH,
	METHOD_INVOCATION_NAME_MISMATCH,
	LITERAL_VALUE_MISMATCH,
	SUBCLASS_TYPE_MISMATCH,
	TYPE_COMPATIBLE_REPLACEMENT,
	MISSING_METHOD_INVOCATION_EXPRESSION,
	INFIX_OPERATOR_MISMATCH,
	INFIX_EXTENDED_OPERAND_NUMBER_MISMATCH;

	public String toString(){
		if (name().equals(AST_TYPE_MISMATCH.name())){
			return "The expressions have a different structure and type";
		}
		else if (name().equals(ARGUMENT_NUMBER_MISMATCH.name())){
			return "The number of arguments is different";
		}
		else if (name().equals(ARRAY_DIMENSION_MISMATCH.name())){
			return "The dimensions of the arrays are different";
		}
		else if (name().equals(VARIABLE_TYPE_MISMATCH.name())){
			return "The types of the variables are different";
		}
		else if (name().equals(VARIABLE_NAME_MISMATCH.name())){
			return "The names of the variables are different";
		}
		else if (name().equals(METHOD_INVOCATION_NAME_MISMATCH.name())){
			return "The names of the invoked methods are different";
		}
		else if (name().equals(LITERAL_VALUE_MISMATCH.name())){
			return "The values of the literals are different";
		}
		else if (name().equals(SUBCLASS_TYPE_MISMATCH.name())){
			return "The types are different subclasses of the same superclass"; 	
		}
		else if (name().equals(TYPE_COMPATIBLE_REPLACEMENT.name())){
			return "The expressions have a different structure, but the same type";
		}
		else if (name().equals(MISSING_METHOD_INVOCATION_EXPRESSION.name())){
			return "One of the method invocations is not called through an object reference";
		}
		else if (name().equals(INFIX_OPERATOR_MISMATCH.name())){
			return "The operators of the infix expressions are different";
		}
		else if (name().equals(INFIX_EXTENDED_OPERAND_NUMBER_MISMATCH.name())){
			return "The infix expressions have a different number of operands";
		}
		return "";
	}
}

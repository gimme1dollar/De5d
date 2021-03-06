package gr.uom.java.ast;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public class LPLMethodObject extends MethodObject {

	static int NumParameterLimit = 3;
	
	/**
	 * Fills a IBuffer with a new class object declaration
	 * @param buffer buffer to modify
	 * @param pf IPackageFragment of the buffer
	 * @param className name of new class
	 * @param extractedParameterTypes list of the types of the extracted parameters
	 * @param extractedParameterNames list of the names of the new parameters
	 */
	public static void fillNewParameterClass(IBuffer buffer, IPackageFragment pf, String className, List<String> extractedParameterTypes, List<String> extractedParameterNames) {
		try {
			assert(extractedParameterTypes.size() == extractedParameterNames.size());
			
			List<String> parameterTypeAndNames = new ArrayList<String>();
			for (int i = 0; i < extractedParameterTypes.size(); ++i) {
				parameterTypeAndNames.add(extractedParameterTypes.get(i) + " " + extractedParameterNames.get(i));
			}
			
			String packageName = pf.getElementName();
			String packageDeclaration = "package " + packageName + ";\n\n";
			
			StringBuilder parameterDeclarationBuilder = new StringBuilder();
			for (String parameterTypeAndName: parameterTypeAndNames) {
				parameterDeclarationBuilder.append("\tprivate " + parameterTypeAndName + ";\n" );
			}
			String parameterDeclaration = parameterDeclarationBuilder.toString();
			
			StringBuilder constructorBuilder = new StringBuilder();
			constructorBuilder.append("\tpublic " + className);
			constructorBuilder.append("(");
			for (int i = 0; i < parameterTypeAndNames.size() - 1; ++i) {
				String parameterTypeAndName = parameterTypeAndNames.get(i);
				constructorBuilder.append(parameterTypeAndName);
				constructorBuilder.append(", ");
			}
			constructorBuilder.append(parameterTypeAndNames.get(parameterTypeAndNames.size() - 1));
			constructorBuilder.append(") ");
			constructorBuilder.append("{ \n");
			for (String parameterName: extractedParameterNames) {
				constructorBuilder.append("\t\tthis."+parameterName+" = " +parameterName + ";\n");
			}
			constructorBuilder.append("\n\t}\n");
			String constructor = constructorBuilder.toString();
			
			StringBuilder getterBuilder = new StringBuilder();
			for (int i = 0; i < parameterTypeAndNames.size(); ++i) {
				StringBuilder aGetterBuilder = new StringBuilder();
				aGetterBuilder.append("\t public ");
				aGetterBuilder.append(extractedParameterTypes.get(i) + " ");
				String parameterName = extractedParameterNames.get(i);
				aGetterBuilder.append("get");
				aGetterBuilder.append(parameterName.substring(0, 1).toUpperCase() + parameterName.substring(1));
				aGetterBuilder.append("()");
				aGetterBuilder.append(" {\n");
				aGetterBuilder.append("\t\t return ");
				aGetterBuilder.append(parameterName);
				aGetterBuilder.append(";\n\t}\n");
				String aGetter = aGetterBuilder.toString();
				getterBuilder.append(aGetter);
			}
			String getter = getterBuilder.toString();
			
			StringBuilder setterBuilder = new StringBuilder();
			for (int i = 0; i < parameterTypeAndNames.size(); ++i) {
				StringBuilder aSetterBuilder = new StringBuilder();
				aSetterBuilder.append("\t public void ");
				String parameterName = extractedParameterNames.get(i);
				aSetterBuilder.append("set");
				aSetterBuilder.append(parameterName.substring(0, 1).toUpperCase() + parameterName.substring(1));
				aSetterBuilder.append("(");
				aSetterBuilder.append(parameterTypeAndNames.get(i));
				aSetterBuilder.append(")");
				aSetterBuilder.append(" {\n");
				aSetterBuilder.append("\t\t this.");
				aSetterBuilder.append(parameterName);
				aSetterBuilder.append(" = ");
				aSetterBuilder.append(parameterName);
				aSetterBuilder.append(";\n\t}\n");
				String aSetter = aSetterBuilder.toString();
				setterBuilder.append(aSetter);
			}
			String setter = setterBuilder.toString();
			
			StringBuilder classDeclarationBuilder = new StringBuilder();
			classDeclarationBuilder.append("public class ");
			classDeclarationBuilder.append(className);
			classDeclarationBuilder.append("{ \n");
			classDeclarationBuilder.append(parameterDeclaration);
			classDeclarationBuilder.append(constructor);
			classDeclarationBuilder.append(getter);
			classDeclarationBuilder.append(setter);
			classDeclarationBuilder.append("}");
			
			String classDeclaration = classDeclarationBuilder.toString();
			
			buffer.append(packageDeclaration);
			buffer.append(classDeclaration);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Extracts the parameters of the method, add new class to the parameter, and change method body
	 * so that compilation errors are avoided.
	 * @param buffer IBuffer to modify
	 * @param method IMethod to modify
	 * @param parameterToExtractIndexList index list of the parameters to extract
	 * @param smellContent object containing information for refactoring such as new class name.
	 * @param tempVarInitializeCode string to insert at beginning of method body. Usually some get statements.
	 */
	public static void editParameterFromBuffer(IBuffer buffer, IMethod method, List<Integer> parameterToExtractIndexList, 
			LPLSmellContent smellContent, String tempVarInitializeCode) {
		try {
			IMethod convertedIMethod = method;
			int startPosition = convertedIMethod.getSourceRange().getOffset();
			
			while (true) {
				if (buffer.getChar(startPosition) != '(') {
					startPosition += 1;
					continue;
				}
				break;
			}
			int numOfLeftPar = 0;
			int endPosition = startPosition;
			while (true) {
				if (buffer.getChar(endPosition) == '(') {
					numOfLeftPar += 1;
				} 
				else if (buffer.getChar(endPosition) == ')') {
					if (numOfLeftPar == 1)
						break;
					else
						numOfLeftPar -= 1;
				}
				endPosition += 1;
			}
			String argumentString = buffer.getContents().substring(startPosition + 1, endPosition);
			String argumentParts[] = argumentString.split(",");
			for(int it : parameterToExtractIndexList) {
				
				argumentParts[it] = null;
			}
			String refactoredArgumentString = "";
			for(String s : argumentParts) {
				if(s != null) {
					refactoredArgumentString += s.trim();
					refactoredArgumentString += ", ";
				}
			}
			String replaceSignature = "(";
			replaceSignature += refactoredArgumentString;
			replaceSignature += smellContent.getNewClassName() + " " + smellContent.getNewParameterName();
			replaceSignature += ")";
			
			int defPosition = endPosition;
			while (buffer.getChar(defPosition) != '{') {
				defPosition += 1;
			}
			defPosition += 1;
			
			
			buffer.replace(defPosition, 0, tempVarInitializeCode);
			buffer.replace(startPosition, endPosition - startPosition + 1, replaceSignature);
		} catch (Exception e) {
				e.printStackTrace();
		}
	}
	
	/**
	 * Returns a string to insert in the beginning of method body when refactoring.
	 * @param extractedParameterTypes list of types of extracted parameters
	 * @param extractedParameterNames list of names of extracted parameters
	 * @param parameterObjName name of new parameter objects
	 * @return
	 */
	public static String codeForInitializingTempVars(List<String> extractedParameterTypes, List<String> extractedParameterNames, String parameterObjName) {
		StringBuilder codeBuilder = new  StringBuilder();
		codeBuilder.append("\n");
		int parameterSize = extractedParameterTypes.size();
		for (int i = 0; i < parameterSize; ++i) {
			codeBuilder.append("\t\t");
			codeBuilder.append(extractedParameterTypes.get(i));
			codeBuilder.append(" ");
			codeBuilder.append(extractedParameterNames.get(i));
			codeBuilder.append(" = ");
			codeBuilder.append(parameterObjName);
			codeBuilder.append(".get");
			String name = extractedParameterNames.get(i);
			String nameWithUpperCase = name.substring(0, 1).toUpperCase() + name.substring(1);
			codeBuilder.append(nameWithUpperCase);
			codeBuilder.append("()");
			codeBuilder.append(";\n");
		}
		return codeBuilder.toString();
	}

	private String codeSmellType;

	public LPLMethodObject(ConstructorObject co) {
		super(co);
		codeSmellType = "Long Parameter List";
	}

	/**
	 * Creates a new LPLMethodObject from an MethodObject
	 * @param methodObject MethodObject to create from
	 * @return
	 */
	public static LPLMethodObject createLPLMethodObjectFrom(MethodObject mo) {
		LPLMethodObject returnObject = new LPLMethodObject(mo.constructorObject);
		returnObject.returnType = mo.returnType;
		returnObject._abstract = mo._abstract;
		returnObject._static = mo._static;
		returnObject._synchronized = mo._synchronized;
		returnObject._native = mo._native;
		returnObject.constructorObject = mo.constructorObject;
		returnObject.testAnnotation = mo.testAnnotation;
		returnObject.hashCode = mo.hashCode;
		return returnObject;
	}

	/**
	 * Get text of a specific column of a code smell in UI
	 * @param index index of column
	 * @return text in column
	 */
	public String getColumnText(int index) {
		switch (index) {
		case 0:
			return codeSmellType;
		case 1:
			return getName();
		case 2:
			return getClassName();
		case 3:
			return getParameterTypeAndNameList().toString();
		case 4:
			return Integer.toString(getParameterList().size());
		default:
			return "";
		}
	}

	/**
	 * Check if this object has a long parameter
	 * @return true if there are more parameters than the limit.
	 */
	public boolean isLongParamterListMethod() {
		return getParameterList().size() > NumParameterLimit;
	}

	/**
	 * Returns an IMethod corresponding to this LPLMethodObject
	 * @param javaProject IJavaProject to search in
	 * @return IMethod corresponding to this project, null if there is some error
	 * @throws JavaModelException
	 */
	public IMethod toIMethod(IJavaProject javaProject) throws JavaModelException {
		String classNameToParse = this.getClassName();
		String[] fileWords = classNameToParse.split("\\.");
		String className = fileWords[fileWords.length - 1];
		String packageName = "";
		for (int i = 0; i < fileWords.length - 1; i++) {
			packageName += fileWords[i];
			if (i != fileWords.length - 2) {
				packageName += ".";
			}
		}

		List<ICompilationUnit> parentClassCandidates = new ArrayList<ICompilationUnit>();
		for (IPackageFragment packageFragment : javaProject.getPackageFragments()) {
			if (packageFragment.getElementName().equals(packageName)) {
				for (ICompilationUnit parentClassCandidate : packageFragment.getCompilationUnits()) {
					parentClassCandidates.add(parentClassCandidate);
				}
			}
		}

		ICompilationUnit parentClass = null;
		for (ICompilationUnit classCandidate : parentClassCandidates) {
			if (classCandidate.getElementName().equals(className + ".java")) {
				parentClass = classCandidate;
				break;
			}
		}
		assert (parentClass != null);

		IMethod methodFound = null;
		for (IType iType : parentClass.getTypes()) {
			for (IMethod method : iType.getMethods()) {
				if (method.getElementName().equals(this.getName())) {
					methodFound = method;
					break;
				}
			}
		}
		assert (methodFound != null);
		return methodFound;

	}

}

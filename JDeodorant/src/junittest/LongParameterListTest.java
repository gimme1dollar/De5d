package junittest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IBufferChangedListener;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import gr.uom.java.ast.ConstructorObject;
import gr.uom.java.ast.LPLMethodObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.jdeodorant.refactoring.views.LPLRefactorWizard;
import gr.uom.java.jdeodorant.refactoring.views.LongParameterList;

public class LongParameterListTest {
	private static MethodObject createMockMethodObject1() {
		ConstructorObject co = new ConstructorObject() {
			@Override
			public String getName(){
				return "testMethod1";
			}
			@Override
			public String getClassName() {
				return "testClass";
			}
			@Override
			public List<String> getParameterList() {
				List<String> parameterList = new ArrayList<String>();
				parameterList.add("a");
				parameterList.add("b");
				return parameterList;
			}
			@Override
			public List<String> getParameterTypeAndNameList() {
				List<String> parameterTypeList = new ArrayList<String>();
				parameterTypeList.add("int a");
				parameterTypeList.add("char b");
				return parameterTypeList;
			}
		};
		return new MethodObject(co);
	}
	static LPLMethodObject mockLPLMethodObject1;
	static LPLMethodObject mockLPLMethodObject2;
	
	@BeforeAll
	public static void setUpBeforeClass() {
		mockLPLMethodObject1 = createMockLPLMethodObject1();
		mockLPLMethodObject2 = createMockLPLMethodObject2();
	}
	
	@Test
	public void testGetColumnText() {
		assertEquals(mockLPLMethodObject1.getColumnText(0), "Long Parameter List");
		assertEquals(mockLPLMethodObject1.getColumnText(1), "testMethod1");
		assertEquals(mockLPLMethodObject1.getColumnText(2), "testClass");
		assertEquals(mockLPLMethodObject1.getColumnText(3), "[int a, char b]");
		assertEquals(mockLPLMethodObject1.getColumnText(4), "2");

	}

	@Test
	public void testCompareTo() {
		assertTrue(mockLPLMethodObject1.compareTo(mockLPLMethodObject2) < 0);
	}

	@Test
	public void testIsLongParameterListMethod() {
		assertFalse(mockLPLMethodObject1.isLongParamterListMethod());
		assertTrue(mockLPLMethodObject2.isLongParamterListMethod());
	}
	
	@Test
	public void testLongParameterListEditParameterFromBuffer() 

	{
		IBuffer buffer = createMockIBuffer();
		
		IMethod method = createMockIMethod();
		
		String parameterString = "int x, int y";
		
		LongParameterList lpl = new LongParameterList();
		
		lpl.editParameterFromBuffer(buffer, method, parameterString);
		
		assertEquals(buffer.getContents(), "class HighInterest{\n" + 
				"public int getAccountNumber() {\n" + 
				"return accountNumber;\n" + 
				"}\n" + 
				"}\n" + 
				"");
	}
	
	@Test
	public void testLPLRefactoringWizardEditParameterFromBuffer() {
		IBuffer buffer = createMockIBuffer();
		IMethod method = createMockIMethod();
		String parameterString = "int x, int y";
		//LPLRefactorWizard lrw = new LPLRefactorWizard(null, null);
		ArrayList<Integer> parameterIndexList = new ArrayList<Integer>();
		parameterIndexList.add(1);
		parameterIndexList.add(2);
		LPLRefactorWizard.editParameterFromBuffer(buffer, method, parameterString, parameterIndexList);
		assertEquals(buffer.getContents(), "class HighInterest{\n" + 
				"public int getAccountNumber(int a, int d, int e) {\n" + 
				"return accountNumber;\n" + 
				"}\n" + 
				"}\n" + 
				"");
	}

	private static LPLMethodObject createMockLPLMethodObject1() {
		MethodObject mockMethodObject = createMockMethodObject1();
		return LPLMethodObject.createLPLMethodObjectFrom(mockMethodObject);
	}

	private static MethodObject createMockMethodObject2() {
			ConstructorObject co = new ConstructorObject() {
				@Override
				public String getName(){
					return "testMethod2";
				}
				@Override
				public String getClassName() {
					return "testClass";
				}
				@Override
				public List<String> getParameterList() {
					List<String> parameterList = new ArrayList<String>();
					parameterList.add("a");
					parameterList.add("b");
					parameterList.add("c");
					parameterList.add("d");
					return parameterList;
				}
				@Override
				public List<String> getParameterTypeAndNameList() {
					List<String> parameterTypeList = new ArrayList<String>();
					parameterTypeList.add("int a");
					parameterTypeList.add("char b");
					parameterTypeList.add("String c");
					parameterTypeList.add("boolean d");
					return parameterTypeList;
				}
			};
			return new MethodObject(co);
	}

	private static LPLMethodObject createMockLPLMethodObject2() {
		MethodObject mockMethodObject = createMockMethodObject2();
		return LPLMethodObject.createLPLMethodObjectFrom(mockMethodObject);
	}

	private static IBuffer createMockIBuffer() {
		return new IBuffer() {
			private String content = "class HighInterest{\n" + 
					"public int getAccountNumber(int a, int b, int c, int d, int e) {\n" + 
					"return accountNumber;\n" + 
					"}\n" + 
					"}\n" + 
					"";

			public void addBufferChangedListener(IBufferChangedListener arg0) {
			}

			public void append(char[] arg0) {	
			}

			public void append(String arg0) {		
			}

			public void close() {		
			}

			public char getChar(int i) {

				return content.charAt(i);
			}

			public char[] getCharacters() {
				return null;
			}

			public String getContents() {
				return content;
			}

			public int getLength() {
				return 0;
			}

			public IOpenable getOwner() {
				return null;
			}

			public String getText(int arg0, int arg1) throws IndexOutOfBoundsException {
				return null;
			}

			public IResource getUnderlyingResource() {
				return null;
			}

			public boolean hasUnsavedChanges() {
				return false;
			}

			public boolean isClosed() {
				return false;
			}

			public boolean isReadOnly() {
				// TODO Auto-generated method stub
				return false;
			}

			public void removeBufferChangedListener(IBufferChangedListener arg0) {
				// TODO Auto-generated method stub
				
			}

			public void replace(int arg0, int arg1, char[] arg2) {
				// TODO Auto-generated method stub
			}

			public void replace(int startIdx, int length, String replacement) {
				content = content.substring(0, startIdx) + replacement + content.substring(startIdx + length);
			}

			public void save(IProgressMonitor arg0, boolean arg1) throws JavaModelException {
				// TODO Auto-generated method stub
				
			}

			public void setContents(char[] arg0) {
				// TODO Auto-generated method stub
				
			}

			public void setContents(String arg0) {
				// TODO Auto-generated method stub				
			}
		};
	}
	
	private static IMethod createMockIMethod() {
		return new IMethod() {

			public String[] getCategories() throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public IClassFile getClassFile() {
				// TODO Auto-generated method stub
				return null;
			}

			public ICompilationUnit getCompilationUnit() {
				// TODO Auto-generated method stub
				return null;
			}

			public IType getDeclaringType() {
				// TODO Auto-generated method stub
				return null;
			}

			public int getFlags() throws JavaModelException {
				// TODO Auto-generated method stub
				return 0;
			}

			public ISourceRange getJavadocRange() throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public int getOccurrenceCount() {
				// TODO Auto-generated method stub
				return 0;
			}

			public IType getType(String arg0, int arg1) {
				// TODO Auto-generated method stub
				return null;
			}

			public ITypeRoot getTypeRoot() {
				// TODO Auto-generated method stub
				return null;
			}

			public boolean isBinary() {
				// TODO Auto-generated method stub
				return false;
			}

			public boolean exists() {
				// TODO Auto-generated method stub
				return false;
			}

			public IJavaElement getAncestor(int arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			public String getAttachedJavadoc(IProgressMonitor arg0) throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public IResource getCorrespondingResource() throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public int getElementType() {
				// TODO Auto-generated method stub
				return 0;
			}

			public String getHandleIdentifier() {
				// TODO Auto-generated method stub
				return null;
			}

			public IJavaModel getJavaModel() {
				// TODO Auto-generated method stub
				return null;
			}

			public IJavaProject getJavaProject() {
				// TODO Auto-generated method stub
				return null;
			}

			public IOpenable getOpenable() {
				// TODO Auto-generated method stub
				return null;
			}

			public IJavaElement getParent() {
				// TODO Auto-generated method stub
				return null;
			}

			public IPath getPath() {
				// TODO Auto-generated method stub
				return null;
			}

			public IJavaElement getPrimaryElement() {
				// TODO Auto-generated method stub
				return null;
			}

			public IResource getResource() {
				// TODO Auto-generated method stub
				return null;
			}

			public ISchedulingRule getSchedulingRule() {
				// TODO Auto-generated method stub
				return null;
			}

			public IResource getUnderlyingResource() throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public boolean isReadOnly() {
				// TODO Auto-generated method stub
				return false;
			}

			public boolean isStructureKnown() throws JavaModelException {
				// TODO Auto-generated method stub
				return false;
			}

			public <T> T getAdapter(Class<T> adapter) {
				// TODO Auto-generated method stub
				return null;
			}

			public ISourceRange getNameRange() throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public String getSource() throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public ISourceRange getSourceRange() throws JavaModelException {
				ISourceRange sourceRange = new ISourceRange() {

					public int getLength() {
						// TODO Auto-generated method stub
						return 0;
					}

					public int getOffset() {
						// TODO Auto-generated method stub
						return 21;
					}
					
				};
				return sourceRange;
			}

			public void copy(IJavaElement arg0, IJavaElement arg1, String arg2, boolean arg3, IProgressMonitor arg4)
					throws JavaModelException {
				// TODO Auto-generated method stub
				
			}

			public void delete(boolean arg0, IProgressMonitor arg1) throws JavaModelException {
				// TODO Auto-generated method stub
				
			}

			public void move(IJavaElement arg0, IJavaElement arg1, String arg2, boolean arg3, IProgressMonitor arg4)
					throws JavaModelException {
				// TODO Auto-generated method stub
				
			}

			public void rename(String arg0, boolean arg1, IProgressMonitor arg2) throws JavaModelException {
				// TODO Auto-generated method stub
				
			}

			public IJavaElement[] getChildren() throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public boolean hasChildren() throws JavaModelException {
				// TODO Auto-generated method stub
				return false;
			}

			public IAnnotation getAnnotation(String arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			public IAnnotation[] getAnnotations() throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public IMemberValuePair getDefaultValue() throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public String getElementName() {
				// TODO Auto-generated method stub
				return null;
			}

			public String[] getExceptionTypes() throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public String getKey() {
				// TODO Auto-generated method stub
				return null;
			}

			public int getNumberOfParameters() {
				// TODO Auto-generated method stub
				return 0;
			}

			public String[] getParameterNames() throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public String[] getParameterTypes() {
				// TODO Auto-generated method stub
				return null;
			}

			public ILocalVariable[] getParameters() throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public String[] getRawParameterNames() throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public String getReturnType() throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public String getSignature() throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public ITypeParameter getTypeParameter(String arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			public String[] getTypeParameterSignatures() throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public ITypeParameter[] getTypeParameters() throws JavaModelException {
				// TODO Auto-generated method stub
				return null;
			}

			public boolean isConstructor() throws JavaModelException {
				// TODO Auto-generated method stub
				return false;
			}

			public boolean isLambdaMethod() {
				// TODO Auto-generated method stub
				return false;
			}

			public boolean isMainMethod() throws JavaModelException {
				// TODO Auto-generated method stub
				return false;
			}

			public boolean isResolved() {
				// TODO Auto-generated method stub
				return false;
			}

			public boolean isSimilar(IMethod arg0) {
				// TODO Auto-generated method stub
				return false;
			}
			
		};
	}
}

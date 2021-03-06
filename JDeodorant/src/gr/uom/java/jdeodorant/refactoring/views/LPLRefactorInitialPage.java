package gr.uom.java.jdeodorant.refactoring.views;

import java.util.ArrayList;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import gr.uom.java.ast.LPLMethodObject;

public class LPLRefactorInitialPage extends WizardPage {
	private ArrayList<Integer> parameterIndexList;
    private Composite container;
    private LPLMethodObject methodToRefactor;
    private TableViewer tableViewer;
    private ArrayList<String> extractParameterNames;
    private ArrayList<String> extractParameterTypes;
    private IJavaProject javaProject;

    /**
     * Constructor for the parameter selection page
     * @param methodToRefactor LPLMethodObject
     * @param javaProject target project
     */
    public LPLRefactorInitialPage(LPLMethodObject methodToRefactor, IJavaProject javaProject) {
        super("First Page");
        this.methodToRefactor = methodToRefactor;
        this.javaProject = javaProject;
        setTitle(methodToRefactor.getName());
        setDescription("Select parameters to extract");
        extractParameterNames = new ArrayList<String>();
        extractParameterTypes = new ArrayList<String>();
        parameterIndexList = new ArrayList<Integer>();
    }

    /**
     * Creates the interface for the table
     */
    public void createControl(Composite parent) {
    	container = new Composite(parent, SWT.NONE);
    	container.setLayout(new FillLayout());
    	
    	Table table = new Table(container, SWT.BORDER | SWT.CHECK | SWT.H_SCROLL);
    	table.setHeaderVisible(true);
    	TableColumn checkColumn = new TableColumn(table, SWT.CENTER);
    	checkColumn.setText("Select");
    	checkColumn.setWidth(80);
    	checkColumn.setAlignment(SWT.CENTER);
    	TableColumn tableTypeColumn = new TableColumn(table, SWT.LEFT);
		tableTypeColumn.setText("Type");
		tableTypeColumn.setWidth(200);
		TableColumn tableNameColumn = new TableColumn(table, SWT.LEFT);
		tableNameColumn.setText("Name");
		tableNameColumn.setWidth(200);
		ArrayList<String> parameterTypeList = new ArrayList<String>();
		
		try {
			parameterTypeList = getSourceCodeTypeList(methodToRefactor.toIMethod(javaProject));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
		for(int i = 0; i < methodToRefactor.getParameterTypeList().size(); i++) {
			TableItem tableItem = new TableItem(table, SWT.NONE);
			tableItem.setText(1, parameterTypeList.get(i));
			tableItem.setText(2, methodToRefactor.getParameterNameList().get(i));
		}
		
		table.addListener(SWT.Selection,  new Listener() {
			public void handleEvent(Event event) {
				boolean isChecked = false;
				if(event.detail == SWT.CHECK) {
					parameterIndexList.clear();
					extractParameterTypes.clear();
					extractParameterNames.clear();
					for(int i = 0; i < ((Table)event.widget).getItems().length; i++) {
						TableItem item = ((Table)event.widget).getItem(i);
						if(item.getChecked()) {
							parameterIndexList.add(i);
							extractParameterTypes.add(((Table)event.widget).getItem(i).getText(1));
							extractParameterNames.add(((Table)event.widget).getItem(i).getText(2));
							isChecked = true;
						}
					}
					if(isChecked) {
						setPageComplete(true);
					}
					else {
					setPageComplete(false);
					}
				}
			}
		});
		setControl(container);
        setPageComplete(false);
    }
    
    /**
     * Get the indexes of the parameters the user checed 
     * @return ArrayList of indexes
     */
     public ArrayList getParameterIndexList() {
        return parameterIndexList;
    }
    
     /**
      * Get the names of the parameters the user checked
      * @return ArrayList of indexes
      */
    public ArrayList getExtractParameterNames() {
    	return extractParameterNames;
    }
    
    /**
     * Get the types of the parameters the user checked
     * @return ArrayList of types
     */
    public ArrayList getExtractParameterTypes() {
    	return extractParameterTypes;
    }
    
    /**
     * Get the strings of the source code of the parameters the user checked
     * For example ["int", "int", "String"]
     * @return ArrayList of parameters in source code form
     */
    public static ArrayList<String> getSourceCodeTypeList(IMethod method) {
    	try {
			IMethod convertedIMethod = method;
			int startPosition = convertedIMethod.getSourceRange().getOffset();
			IBuffer buffer = convertedIMethod.getCompilationUnit().getBuffer();
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
			for(int i = 0; i < argumentParts.length; i++) {
				argumentParts[i] = argumentParts[i].trim();
				argumentParts[i] = argumentParts[i].split(" ")[0];
			}
			ArrayList<String> ret = new ArrayList<String>();
			for(String s : argumentParts) {
				ret.add(s);
			}
			return ret;
		} catch (Exception e) {
				e.printStackTrace();
		}
    	return null;
    }

}

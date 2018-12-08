package gr.uom.java.jdeodorant.refactoring.views;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.AbstractMethodDeclaration;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.ClassObjectCandidate;
import gr.uom.java.ast.CompilationErrorDetectedException;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CFG;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.PDGObjectSliceUnion;
import gr.uom.java.ast.decomposition.cfg.PDGObjectSliceUnionCollection;
import gr.uom.java.ast.decomposition.cfg.PDGSliceUnion;
import gr.uom.java.ast.decomposition.cfg.PDGSliceUnionCollection;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.util.StatementExtractor;
import gr.uom.java.distance.CandidateRefactoring;
import gr.uom.java.distance.MoveMethodCandidateRefactoring;
import gr.uom.java.jdeodorant.preferences.PreferenceConstants;
import gr.uom.java.jdeodorant.refactoring.Activator;
import gr.uom.java.jdeodorant.refactoring.manipulators.ASTSlice;
import gr.uom.java.jdeodorant.refactoring.manipulators.ASTSliceGroup;
import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractMethodRefactoring;
import gr.uom.java.jdeodorant.refactoring.manipulators.MoveMethodRefactoring;

import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.texteditor.ITextEditor;

public class MessageChain extends ViewPart {
	private static final String MESSAGE_DIALOG_TITLE = "Message Chain";
	private TreeViewer treeViewer;
	private Action identifyBadSmellsAction;
	private Action applyRefactoringAction;
	private Action doubleClickAction;
	private Action saveResultsAction;
	// private Action evolutionAnalysisAction;
	private IJavaProject selectedProject;
	private IJavaProject activeProject;
	private IPackageFragmentRoot selectedPackageFragmentRoot;
	private IPackageFragment selectedPackageFragment;
	private ICompilationUnit selectedCompilationUnit;
	private IType selectedType;
	private IMethod selectedMethod;
	private ASTSliceGroup[] sliceGroupTable;
	// private MethodEvolution methodEvolution;
	/* Mine */
	
	private String PLUGIN_ID = "gr.uom.java.jdeodorant";
	
	public MessageChainStructure[] targets;
	private Map<String, Map<Integer, List<MethodInvocationObject>>> originCodeSmells; // for storing origin map
	public List<String> newRefactoringMethod;//store method's name and class name of refactoring code. We will add new method name whenever we do refactoring
	
	// Do it in Iteration 4
	public String newMethodName;
	   
	//private IJavaProject 
	private class MessageChainRefactoringButtonUI extends RefactoringButtonUI{
	   public void pressRefactoringButton(int index) {	         
	   }
	   // Edit it in Iteration 4
	   public void pressChildRefactorButton(int parentIndex, int childIndex) {
	      //System.out.println("Success");
	      MCRefactorWizard wizard = new MCRefactorWizard(selectedProject);
	         
	      WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard); dialog.open();
	         
	      newMethodName = wizard.getNewMethodName();
	      //System.out.println("New Method Name is: " + newMethodName);
	         
	      if(newMethodName != null)   
	    	  messageChainRefactoring(parentIndex, childIndex);
	  }
	}

	private MessageChainRefactoringButtonUI refactorButtonMaker;
	
	public class ViewContentProvider implements ITreeContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if(targets!=null) {
				return targets;
			}
			else {
				return new MessageChainStructure[] {};
			}
	      }

	      public Object[] getChildren(Object arg) {
	    	  if(arg != null && arg instanceof MessageChainStructure) {
	    		  List<MessageChainStructure> temp = ((MessageChainStructure)arg).getChildren();
	    		  int len = temp.size();
	    		  MessageChainStructure[] childArray = new MessageChainStructure[len];
	    		  for(int i = 0; i<len; i++) {
	    			  childArray[i] = temp.get(i);
	    		  }
		            return childArray;
		         }
		         else {
		            return (MessageChainStructure[])new MessageChainStructure[] {};
		         }
	      }

	      public Object getParent(Object arg0) {
	         return ((MessageChainStructure)arg0).getParent();
	      }

	      public boolean hasChildren(Object arg0) {
	         return getChildren(arg0).length > 0;
	      }

		
	}

	public class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {

			if(obj instanceof MessageChainStructure) {
				MessageChainStructure entry = (MessageChainStructure)obj;
				switch(index){
				case 0:
					if(entry.getStart() == -1) {
						return "";
					}
					return entry.getStart().toString();
				case 1:
					return entry.getName();
				case 2:
					if(entry.getLength() == -1) {
						return "";
					}
					return String.valueOf(entry.getLength());
				default:
					return "";
				}
			}

			return "";
		}

		public Image getColumnImage(Object obj, int index) {
			return null;
		}
		public Image getImage(Object obj) {
			return null;
		}

	}


	private ISelectionListener selectionListener = new ISelectionListener() {
		public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection) selection;
				Object element = structuredSelection.getFirstElement();
				IJavaProject javaProject = null;
				if (element instanceof IJavaProject) {
					javaProject = (IJavaProject) element;
					selectedPackageFragmentRoot = null;
					selectedPackageFragment = null;
					selectedCompilationUnit = null;
					selectedType = null;
					selectedMethod = null;
				} else if (element instanceof IPackageFragmentRoot) {
					IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot) element;
					javaProject = packageFragmentRoot.getJavaProject();
					selectedPackageFragmentRoot = packageFragmentRoot;
					selectedPackageFragment = null;
					selectedCompilationUnit = null;
					selectedType = null;
					selectedMethod = null;
				} else if (element instanceof IPackageFragment) {
					IPackageFragment packageFragment = (IPackageFragment) element;
					javaProject = packageFragment.getJavaProject();
					selectedPackageFragment = packageFragment;
					selectedPackageFragmentRoot = null;
					selectedCompilationUnit = null;
					selectedType = null;
					selectedMethod = null;
				} else if (element instanceof ICompilationUnit) {
					ICompilationUnit compilationUnit = (ICompilationUnit) element;
					javaProject = compilationUnit.getJavaProject();
					selectedCompilationUnit = compilationUnit;
					selectedPackageFragmentRoot = null;
					selectedPackageFragment = null;
					selectedType = null;
					selectedMethod = null;
				} else if (element instanceof IType) {
					IType type = (IType) element;
					javaProject = type.getJavaProject();
					selectedType = type;
					selectedPackageFragmentRoot = null;
					selectedPackageFragment = null;
					selectedCompilationUnit = null;
					selectedMethod = null;
				} else if (element instanceof IMethod) {
					IMethod method = (IMethod) element;
					javaProject = method.getJavaProject();
					selectedMethod = method;
					selectedPackageFragmentRoot = null;
					selectedPackageFragment = null;
					selectedCompilationUnit = null;
					selectedType = null;
				}
				if (javaProject != null && !javaProject.equals(selectedProject)) {
					selectedProject = javaProject;
					/*
					 * if(sliceTable != null) tableViewer.remove(sliceTable);
					 */
					identifyBadSmellsAction.setEnabled(true);
				}
			}
		}
	};

	@Override
	public void createPartControl(Composite parent) {
		newRefactoringMethod = new ArrayList<String>();
		refactorButtonMaker = new MessageChainRefactoringButtonUI();
		originCodeSmells = null;
		treeViewer = new TreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		treeViewer.setContentProvider(new ViewContentProvider());
		treeViewer.setLabelProvider(new ViewLabelProvider());
		//treeViewer.setSorter(new NameSorter());
		treeViewer.setInput(getViewSite());
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(30, true));
		layout.addColumnData(new ColumnWeightData(60, true));
		layout.addColumnData(new ColumnWeightData(40, true));
		layout.addColumnData(new ColumnWeightData(20, true));
		/*layout.addColumnData(new ColumnWeightData(20, true));
		layout.addColumnData(new ColumnWeightData(20, true));*/
		treeViewer.getTree().setLayout(layout);
		treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		treeViewer.getTree().setLinesVisible(true);
		treeViewer.getTree().setHeaderVisible(true);
		TreeColumn column0 = new TreeColumn(treeViewer.getTree(), SWT.LEFT);
		column0.setText("Start Position");
		column0.setResizable(true);
		column0.pack();
		TreeColumn column1 = new TreeColumn(treeViewer.getTree(), SWT.LEFT);
		column1.setText("Name");
		column1.setResizable(true);
		column1.pack();
		TreeColumn column2 = new TreeColumn(treeViewer.getTree(), SWT.LEFT);
		column2.setText("Length");
		column2.setResizable(true);
		column2.pack();
		TreeColumn column3 = new TreeColumn(treeViewer.getTree(), SWT.LEFT);
		column3.setText("Refactoring");
		column3.setResizable(true);
		column3.pack();

		treeViewer.expandAll();

		treeViewer.setColumnProperties(
				new String[] { "Start Position", "Name", "Length", "Refactoring" });
		treeViewer.setCellEditors(new CellEditor[] { new TextCellEditor(), new TextCellEditor(), new TextCellEditor(),
				new TextCellEditor(), new TextCellEditor(), new MyComboBoxCellEditor(treeViewer.getTree(),
						new String[] { "0", "1", "2", "3" }, SWT.READ_ONLY) });


		makeActions();
		hookDoubleClickAction();
		contributeToActionBars();
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);
		JavaCore.addElementChangedListener(ElementChangedListener.getInstance());
		/*getSite().getWorkbenchWindow().getWorkbench().getOperationSupport().getOperationHistory()
				.addOperationHistoryListener(new IOperationHistoryListener() {
					public void historyNotification(OperationHistoryEvent event) {
						int eventType = event.getEventType();
						if (eventType == OperationHistoryEvent.UNDONE || eventType == OperationHistoryEvent.REDONE
								|| eventType == OperationHistoryEvent.OPERATION_ADDED
								|| eventType == OperationHistoryEvent.OPERATION_REMOVED) {
							if (activeProject != null && CompilationUnitCache.getInstance().getAffectedProjects()
									.contains(activeProject)) {
							}
						}
					}
				});*/
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(identifyBadSmellsAction);
	}
 	
	/**
	 * New function for making codes about new method after user decides to Refactoring
	 * 
	 * <Arguments>
	 * String newMethodName: Name of new Method
	 * String returnType: Return type of new Method
	 * List<String> stringOfArgumentType: String List that contains whole of argument types in original methods
	 * List<Integer> numOfArgumentOfEachMethod: Integer List that contains number of arguments for each methods
	 * List<String> stringOfMethodInvocation: String List that contain whole of method invocations
	 * **/
	public String makeNewMethodCode (String newMethodName, String returnType, List<String> stringOfArgumentType, List<Integer> numOfArgumentOfEachMethod, List<String> stringOfMethodInvocation) {
		String strOfMethod = "";
		strOfMethod += "public ";
		strOfMethod += returnType;
		strOfMethod += " ";
		strOfMethod += newMethodName;
		strOfMethod += "(";
		for(int i = 0; i<stringOfArgumentType.size(); i++) {
			strOfMethod += stringOfArgumentType.get(i);
			strOfMethod += " ";
			strOfMethod += "x";
			strOfMethod += Integer.toString(i);
			strOfMethod += ", ";
		}
		if(stringOfArgumentType.size() > 0) {
			strOfMethod = strOfMethod.substring(0,strOfMethod.length()-2);
		}				
		strOfMethod += ")";
		strOfMethod += " {\r\n";
		strOfMethod += "\treturn ";
		int numOfArg = 0;
		int numOfMethod = 0;
		for(String method : stringOfMethodInvocation) {
			strOfMethod += method;
			strOfMethod += "(";
			for(int i = 0; i< numOfArgumentOfEachMethod.get(numOfMethod); i++, numOfArg++) {
				strOfMethod += "x";
				strOfMethod += Integer.toString(numOfArg);
				strOfMethod += ", ";
			}
			if(numOfArgumentOfEachMethod.get(numOfMethod)>0) {
				strOfMethod = strOfMethod.substring(0,strOfMethod.length()-2);
			}					
			strOfMethod += ")";
			strOfMethod += ".";
			numOfMethod++;
		}
		strOfMethod = strOfMethod.substring(0,strOfMethod.length()-1);
		strOfMethod += ";\r\n";
		strOfMethod += "}\r\n";
		return strOfMethod;
	}
	
	/**
	 * New function for modify code that contains new method after user decides to Refactoring
	 * 
	 * <Arguments>
	 * String newMethodName: Name of new method
	 * List<String> stringOfArgument: String List that contains whole of arguments
	 * **/
	public String makeNewRefactorCode(String newMethodName, List<String> stringOfArgument) {
		String strOfRefact = "";
		strOfRefact += newMethodName;
		strOfRefact += "(";
		for (String arg : stringOfArgument) {
			strOfRefact += arg;
			strOfRefact += ", ";
		}
		if(stringOfArgument.size() > 0) {
			strOfRefact = strOfRefact.substring(0, strOfRefact.length() - 2);
		}				
		strOfRefact += ")";
		return strOfRefact;
	}
	 
	//Impossible to make junit test because we don't have permission of making MethodInvocationObject mock object
	/**
	 * New function for Message Chain Refactoring.
	 * 
	 * <Arguments>
	 * int parentIndex: Index of information about parent of selected file
	 * int childIndex: Index of information about children of selected file
	 * **/
	public void messageChainRefactoring(int parentIndex, int childIndex) {
		if(childIndex == -1 && parentIndex == -1) {
			treeViewer.getTree().setSelection(treeViewer.getTree().getItem(parentIndex));
		}
		else {
			treeViewer.getTree().setSelection(treeViewer.getTree().getItem(parentIndex).getItem(childIndex));
		}
		IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
		MessageChainStructure targetSmell = ((MessageChainStructure)selection.getFirstElement());
		if(targetSmell != null && targetSmell.getStart() != -1 && targetSmell.getLength() != -1) {
			SystemObject systemObject = ASTReader.getSystemObject();
			if(systemObject != null) {
				String targetSmellName = targetSmell.getName();
				String classNameOfMethodInvocation = originCodeSmells.get(targetSmell.getParent().getName()).get(targetSmell.getStart()).get(0).getOriginClassName();

				ICompilationUnit compUnitWithCodeSmell = getCompUnit (systemObject, targetSmell.getParent().getName());
				ICompilationUnit compUnitOfMethodInvocation = getCompUnit (systemObject, classNameOfMethodInvocation);
				
//				String newMethodName = "myMethod";//TODO : we have to get methodName from user
				
				int sizeOfMethodInvocation = originCodeSmells.get(targetSmell.getParent().getName()).get(targetSmell.getStart()).size();
				String stringOfMethod = originCodeSmells.get(targetSmell.getParent().getName()).get(targetSmell.getStart()).get(sizeOfMethodInvocation-1).getReturnType().toString();
	            String returnType = getClassName(sizeOfMethodInvocation, stringOfMethod);
	            
				List<String> stringOfMethodInvocation = new ArrayList<String> ();
				List<String> stringOfArgumentType = new ArrayList<String> ();
				List<Integer> numOfArgumentOfEachMethod = new ArrayList<Integer>();
				List<String> stringOfArgument = new ArrayList<String> ();
				for(int i = 0; i<sizeOfMethodInvocation;i++) {
					stringOfMethodInvocation.add(originCodeSmells.get(targetSmell.getParent().getName()).get(targetSmell.getStart()).get(i).getMethodName());
					for(Object arg : originCodeSmells.get(targetSmell.getParent().getName()).get(targetSmell.getStart()).get(i).getMethodInvocation().arguments() ) {
						stringOfArgument.add(arg.toString());
					}

					stringOfArgumentType.addAll(originCodeSmells.get(targetSmell.getParent().getName()).get(targetSmell.getStart()).get(i).getParameterList());
					numOfArgumentOfEachMethod.add(originCodeSmells.get(targetSmell.getParent().getName()).get(targetSmell.getStart()).get(i).getParameterList().size());
				}			
				
				String strOfRefact = makeNewRefactorCode(newMethodName, stringOfArgument);
				String strOfMethod = makeNewMethodCode(newMethodName, returnType, stringOfArgumentType, numOfArgumentOfEachMethod, stringOfMethodInvocation);

				modifyMethodInvocationFile (compUnitOfMethodInvocation, strOfMethod);
				modifyCodeSmellFile (compUnitWithCodeSmell, strOfRefact, targetSmell);
				
				newRefactoringMethod.add(classNameOfMethodInvocation+"/"+newMethodName);//add new method name to notify that this code should not be detected.
				findCodeSmell();
				
				MessageChainStructure nextTargetSmell = findMCSwithGivenName(targetSmellName);
				while(nextTargetSmell != null) {
					ICompilationUnit compUnitWithNextCodeSmell = getCompUnit (systemObject, nextTargetSmell.getParent().getName());
					
					List<String> stringOfNextArgument = new ArrayList<String> ();
					for(int i = 0; i<sizeOfMethodInvocation;i++) {
						for(Object arg : originCodeSmells.get(nextTargetSmell.getParent().getName()).get(nextTargetSmell.getStart()).get(i).getMethodInvocation().arguments() ) {
							stringOfNextArgument.add(arg.toString());
						}
					}
					
					String strOfNextRefact = makeNewRefactorCode(newMethodName, stringOfNextArgument);
					
					modifyCodeSmellFile (compUnitWithNextCodeSmell, strOfNextRefact, nextTargetSmell);
					
					findCodeSmell();
					nextTargetSmell = findMCSwithGivenName(targetSmellName);
				}
			}
		}
	}
	
	public ICompilationUnit getCompUnit (SystemObject systemObject, String className) {
		ClassObject classWithCodeSmell = systemObject.getClassObject(className);
		IFile fileWithCodeSmell = classWithCodeSmell.getIFile();	
		ICompilationUnit compUnitWithCodeSmell = (ICompilationUnit) JavaCore.create(fileWithCodeSmell);
		return compUnitWithCodeSmell;
	}
	
	public void modifyCodeSmellFile (ICompilationUnit compUnitWithCodeSmell, String stringForChange, MessageChainStructure targetSmell) {
		try {
			ICompilationUnit workingCopyWithCodeSmell = compUnitWithCodeSmell.getWorkingCopy(new WorkingCopyOwner() {}, null);
			IBuffer bufferWithCodeSmell = ((IOpenable)workingCopyWithCodeSmell).getBuffer();
		    
		    String temp = bufferWithCodeSmell.getText(targetSmell.getStart(), targetSmell.getLength());
		    int startPos = temp.indexOf(originCodeSmells.get(targetSmell.getParent().getName()).get(targetSmell.getStart()).get(0).getMethodName());
			
		    modifyCompUnit(workingCopyWithCodeSmell, bufferWithCodeSmell, targetSmell.getStart() + startPos, targetSmell.getLength()-startPos, stringForChange);
		} catch (JavaModelException e) {
		}
	}
	
	public void modifyMethodInvocationFile (ICompilationUnit compUnitOfMethodInvocation, String strOfMethod) {
		try {
			ICompilationUnit workingCopyOfMethodInvocation = compUnitOfMethodInvocation.getWorkingCopy(new WorkingCopyOwner() {}, null);
			IBuffer bufferOfMethodInvocation = ((IOpenable)workingCopyOfMethodInvocation).getBuffer();
		    
			   
		    int modifyPosition = getModifyPosition(bufferOfMethodInvocation);
		    
		    modifyCompUnit(workingCopyOfMethodInvocation, bufferOfMethodInvocation, modifyPosition,0,strOfMethod);
		} catch (JavaModelException e) {
		}
	    
	}
	
	public void modifyCompUnit (ICompilationUnit workingCopy, IBuffer buffer, int startPos, int len, String stringForChange) {
		try {					
			buffer.replace(startPos, len, stringForChange);
			workingCopy.reconcile(ICompilationUnit.NO_AST,false,null,null);
			workingCopy.commitWorkingCopy(false,null);
			workingCopy.discardWorkingCopy();
		} catch (JavaModelException e) {
		}
		
	}
	
	public MessageChainStructure findMCSwithGivenName(String name) {
		for(MessageChainStructure mcs : targets) {
			for(MessageChainStructure child :mcs.getChildren()) {
				if(child.getName().equals(name)) {
					return child;
				}
			}
		}
		return null;
	}
	
	/**
	 * New function for getting only class name if the string contains path of class
	 * 
	 * <Arguments>
	 * int sizeOfMethodInvocation: Length of string that is path of class
	 * String stringOfMethodInvocation: String that is path of class
	 * **/
	public String getClassName(int sizeOfMethodInvocation, String stringOfMethodInvocation)
	   {
	      int count = 0;
	      for(int i = stringOfMethodInvocation.length()-1; i >= 0; i--)
	      {
	         if(stringOfMethodInvocation.charAt(i) == '.')
	         {
	            count = i + 1;
	            break;
	         }
	      }
	      return stringOfMethodInvocation.substring(count, stringOfMethodInvocation.length());
	   }
	   
	/**
	 * New function for modifying position to modify original code properly
	 * 
	 * <Arguments>
	 * IBuffer bufferOfMethodInvocation: Buffer that contains whole codes about selected Java File
	 * **/
	 public int getModifyPosition(IBuffer bufferOfMethodInvocation)
	   {   
	       int length = bufferOfMethodInvocation.getLength();
	        int count = 0;
	        for(int i=length - 1;i>=0;i--, count++)
	        {
	           if(bufferOfMethodInvocation.getChar(i) == '}')
	              break;
	        }
	        return length - (count + 1);
	   }

	 private void findCodeSmell() {
			CompilationUnitCache.getInstance().clearCache();
			/* Mine */
			targets = getTable();
			treeViewer.setContentProvider(new ViewContentProvider());
			
			refactorButtonMaker.disposeButtons();
			
			Tree tree = treeViewer.getTree();
			refactorButtonMaker.setTree(tree);
			refactorButtonMaker.makeRefactoringButtons(3);

			tree.addListener(SWT.Expand, new Listener() {
				public void handleEvent(Event e) {
					refactorButtonMaker.makeChildrenRefactoringButtons(3);
				}
			});
	 }

	 
	private void makeActions() {
		identifyBadSmellsAction = new Action() {
			public void run() {
				activeProject = selectedProject;
				findCodeSmell();
			}
		};
		ImageDescriptor refactoringButtonImage = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, "/icons/search_button.png");
        identifyBadSmellsAction.setToolTipText("Identify Bad Smells");
        //identifyBadSmellsAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
        identifyBadSmellsAction.setImageDescriptor(refactoringButtonImage);
        identifyBadSmellsAction.setEnabled(false);


		doubleClickAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
				MessageChainStructure targetSmell = ((MessageChainStructure)selection.getFirstElement());
				
				if(targetSmell != null && targetSmell.getStart() != -1 && targetSmell.getLength() != -1) {
					SystemObject systemObject = ASTReader.getSystemObject();
					if(systemObject != null) {
						ClassObject classwithCodeSmell = systemObject.getClassObject(targetSmell.getParent().getName());
						IFile fileWithCodeSmell = classwithCodeSmell.getIFile();
						IJavaElement sourceJavaElement = JavaCore.create(fileWithCodeSmell);
						try {
							ITextEditor sourceEditor = (ITextEditor) JavaUI.openInEditor(sourceJavaElement);
							AnnotationModel annotationModel = (AnnotationModel) sourceEditor.getDocumentProvider()
									.getAnnotationModel(sourceEditor.getEditorInput());
							Iterator<Annotation> annotationIterator = annotationModel.getAnnotationIterator();
							while (annotationIterator.hasNext()) {
								Annotation currentAnnotation = annotationIterator.next();

								annotationModel.removeAnnotation(currentAnnotation);
							}
							Position position = new Position(targetSmell.getStart(), (targetSmell.getLength() + 1));
							SliceAnnotation annotation = null;
							annotation = new SliceAnnotation(SliceAnnotation.EXTRACTION, "Message Chain : We detect Method Invocation Chain consisting of "+targetSmell.getName()+".\r\n "
									+ "We strongly recommend refactoring this message chain."
									+ " If you want to refactor this message chain, press the table's refactoring button."
									+ " We will then remove the long message chain where the message chain originated and add a new wrapper method for that message chain."
									+ " The name of the new wrapper method can be specified by the user. ");

							annotationModel.addAnnotation(annotation, position);
					
							sourceEditor.setHighlightRange(position.getOffset(), position.getLength(), true);
							System.out.println("Highlighted!!!");
							
						} catch (PartInitException e) {
							e.printStackTrace();
						} catch (JavaModelException e) {
							e.printStackTrace();
						}
					}
				}		
			}
		};
	}

	private void hookDoubleClickAction() {
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	public void dispose() {
		super.dispose();
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(selectionListener);
	}

	//Impossible to make junit test because we don't have permission of making MethodInvocationObject mock object
	private MessageChainStructure[] getTable() {
		
		Map<String, Map<Integer, List<MethodInvocationObject>>> table = null;
		List<MessageChainStructure> listTargets = new ArrayList<MessageChainStructure> ();
		try {
			IWorkbench wb = PlatformUI.getWorkbench();
			IProgressService ps = wb.getProgressService();
			if (ASTReader.getSystemObject() != null && activeProject.equals(ASTReader.getExaminedProject())) {
				new ASTReader(activeProject, ASTReader.getSystemObject(), null);
			} else {
				ps.busyCursorWhile(new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							new ASTReader(activeProject, monitor);
						} catch (CompilationErrorDetectedException e) {
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									MessageDialog.openInformation(
											PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
											MESSAGE_DIALOG_TITLE,
											"Compilation errors were detected in the project. Fix the errors before using JDeodorant.");
								}
							});
						}
					}
				});
			}
			final SystemObject systemObject = ASTReader.getSystemObject();
			if (systemObject != null) {
				final Set<ClassObject> classObjectsToBeExamined = new LinkedHashSet<ClassObject>();
				final Set<AbstractMethodDeclaration> methodObjectsToBeExamined = new LinkedHashSet<AbstractMethodDeclaration>();
				if (selectedPackageFragmentRoot != null) {
					classObjectsToBeExamined.addAll(systemObject.getClassObjects(selectedPackageFragmentRoot));
				} else if (selectedPackageFragment != null) {
					classObjectsToBeExamined.addAll(systemObject.getClassObjects(selectedPackageFragment));
				} else if (selectedCompilationUnit != null) {
//					selectedCompilationUnit.getTypes()[0].createMethod(arg0, arg1, arg2, arg3)
					classObjectsToBeExamined.addAll(systemObject.getClassObjects(selectedCompilationUnit));
				} else if (selectedType != null) {
					classObjectsToBeExamined.addAll(systemObject.getClassObjects(selectedType));
				} else if (selectedMethod != null) {
					AbstractMethodDeclaration methodObject = systemObject.getMethodObject(selectedMethod);
					if (methodObject != null) {
						ClassObject declaringClass = systemObject.getClassObject(methodObject.getClassName());
						if (declaringClass != null && !declaringClass.isEnum() && !declaringClass.isInterface()
								&& methodObject.getMethodBody() != null)
							methodObjectsToBeExamined.add(methodObject);
					}
				} else {
					classObjectsToBeExamined.addAll(systemObject.getClassObjects());
				}
				for (ClassObject classObj : classObjectsToBeExamined) {
					for (AbstractMethodDeclaration methodObj : classObj.getMethodList()) {
						if (!methodObjectsToBeExamined.contains(methodObj)) {
							methodObjectsToBeExamined.add(methodObj);
						}
					}
				}
				
				table = processMethod(methodObjectsToBeExamined);
				originCodeSmells = table;
				listTargets = convertMap2MCS(table);
				}
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (CompilationErrorDetectedException e) {
			MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					MESSAGE_DIALOG_TITLE,
					"Compilation errors were detected in the project. Fix the errors before using JDeodorant.");
		}
		int leng = listTargets.size();
		MessageChainStructure[] arrayTargets = new MessageChainStructure[leng];
		
		for(int i= 0; i <leng ;i++) {
			arrayTargets[i] = listTargets.get(i);
		}
		
		return arrayTargets;
	}
	
	/**
	 * New function for checking whether this method is made by refactoring or not.
	 * 
	 * <Arguments>
	 * String className: Name of class
	 * String methodName: Name of method
	 * **/
	public boolean checkMethodIfFromRefactoring(String className, String methodName) {
		String target = "";
		target += className;
		target += "/";
		target += methodName;
		
		return newRefactoringMethod.contains(target);//true : refactoring's result, false : code smell
	}
	
	/**
	 * New function for checking possibility to modify codes in that class.
	 * 
	 * <Arguments>
	 * String className: Name of class
	 * **/
	public boolean isPossibleChange (String className)
	{
		return !(className.contains("java") || className.contains("org"));
	}
	
	//Impossible to make junit test because we don't have permission of making MethodInvocationObject mock object
	private Map<String, Map<Integer, List<MethodInvocationObject>>> processMethod(Set<AbstractMethodDeclaration> methodObjects) {
		Map<String, Map<Integer, List<MethodInvocationObject>>> store = new HashMap<String, Map<Integer, List<MethodInvocationObject>>>();
		MethodObject[] dummy = new MethodObject[3];
		
		for (AbstractMethodDeclaration methodObject : methodObjects) {
			if (methodObject.getMethodBody() != null && checkMethodIfFromRefactoring(methodObject.getClassName(),methodObject.getName()) == false) {//check method is from refactoring
				List<MethodInvocationObject> test = methodObject.getMethodInvocations();
				
				for(MethodInvocationObject methodInvo : test) {
					MethodInvocation methodInvocation = methodInvo.getMethodInvocation();
					int startPos = methodInvocation.getStartPosition();
					String cls = methodObject.getClassName();
					
					if(store.containsKey(cls)) {
						Map<Integer, List<MethodInvocationObject>> inner = store.get(cls);
						if(inner.containsKey(startPos)) {
							inner.get(startPos).add(methodInvo);
						}
						else {
							List<MethodInvocationObject> temp = new ArrayList<MethodInvocationObject>();
							temp.add(methodInvo);
							inner.put(startPos, temp);
						}
					}
					else {
						List<MethodInvocationObject> temp = new ArrayList<MethodInvocationObject>();
						temp.add(methodInvo);
						Map<Integer, List<MethodInvocationObject>> tmp = new HashMap<Integer, List<MethodInvocationObject>>();
						tmp.put(startPos, temp);
						store.put(cls, tmp);
					}
				}
				
				List<Integer> deleteList = new ArrayList<Integer>();
				for(String type : store.keySet()) {
					Map<Integer, List<MethodInvocationObject>> innerMap = store.get(type);
					for(Integer i : innerMap.keySet()) {
						if(innerMap.get(i).size() <= 2 || !isPossibleChange(innerMap.get(i).get(0).getOriginClassName())) {
							deleteList.add(i);
						}
					}
				}
				
				for(String type : store.keySet()) {
					Map<Integer, List<MethodInvocationObject>> innerMap = store.get(type);
					for(Integer i: deleteList) {
						innerMap.remove(i);
					}
				}


			}
		}
		List<String> tempTrashCan = new ArrayList<String> ();
		if(store.keySet().size()>0) {
			for(String cls : store.keySet()) {
				if(store.get(cls).keySet().size() == 0) {
					tempTrashCan.add(cls);
				}
			}
		}
		if(tempTrashCan.size()>0) {
			for(String cls : tempTrashCan) {
				store.remove(cls);
			}
		}
		
		
		return store;
	}

	//Impossible to make junit test because we don't have permission of making MethodInvocationObject mock object
	private List<MessageChainStructure> convertMap2MCS(Map<String, Map<Integer, List<MethodInvocationObject>>> arg){
		List<MessageChainStructure> ret = new ArrayList<MessageChainStructure>();
		if(arg == null) return ret;
		for(String className : arg.keySet()) {
			Map<Integer, List<MethodInvocationObject>> innerMap = arg.get(className);
			MessageChainStructure cls = new MessageChainStructure(className);
			ret.add(cls);
			for(Integer i : innerMap.keySet()) {
				String methodName = "";
				int codeLength = -1;
				for(MethodInvocationObject methodinvocation : innerMap.get(i)) {
					MethodInvocation methodInvocation = methodinvocation.getMethodInvocation();
					methodName += methodInvocation.getName().toString() + "->";
					codeLength = codeLength > methodInvocation.getLength() ? codeLength : methodInvocation.getLength();
				}
				methodName = methodName.substring(0, methodName.length()-2);
				MessageChainStructure method = new MessageChainStructure(i, cls, methodName,codeLength);
				cls.addChild(method);
				
			}
		}
		return ret;
	}
}
package gr.uom.java.jdeodorant.refactoring.views;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
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

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.ClassObjectCandidate;
import gr.uom.java.ast.CompilationErrorDetectedException;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.SystemObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.decomposition.CompositeStatementObject;
import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.jdeodorant.preferences.PreferenceConstants;
import gr.uom.java.jdeodorant.refactoring.Activator;
import gr.uom.java.jdeodorant.refactoring.manipulators.ASTSlice;
import gr.uom.java.jdeodorant.refactoring.manipulators.ASTSliceGroup;

/**
 * Detect Code Smell of Speculative Generality
 * @author 이재엽, 이주용
 *
 * Refactor the Smelling Codes
 * @author 이주용, 손태영
 */
public class SpeculativeGenerality extends ViewPart {
	private static final String MESSAGE_DIALOG_TITLE = "Speculative Generality";
	private TreeViewer treeViewer;
	private Action identifyBadSmellsAction;
	private Action applyRefactoringAction;
	private Action doubleClickAction;
	private IJavaProject selectedProject;
	private IJavaProject activeProject;
	private IPackageFragmentRoot selectedPackageFragmentRoot;
	private IPackageFragment selectedPackageFragment;
	private ICompilationUnit selectedCompilationUnit;
	private IType selectedType;
	private IMethod selectedMethod;

	private String PLUGIN_ID = "gr.uom.java.jdeodorant";

	// Exp : would-be extended to contain information of line, source-path, and so forth... (for UI)
	private ClassObjectCandidate[] classObjectTable;


	private ClassObjectCandidate[] _smellingClassEntries;
	private Set<ClassObject> _classObjectToBeExamined = new HashSet<ClassObject>();

	private SpeculativeGeneralityRefactoringButtonUI refactorButtonMaker;

	public class SpeculativeGeneralityRefactoringButtonUI extends RefactoringButtonUI {
		@Override
		public void makeChildrenRefactoringButtons(int columnIndex) {
			TreeItem[] items = tree.getItems();
			for(int i = 0; i < items.length; i++) {
				TreeEditor editor = new TreeEditor(tree);

				TreeItem item1 = items[i];

				this.getChildrenButtonList().add(new ArrayList<Button>());

				for(int j = 0; j < item1.getItems().length; j++) {
					TreeEditor editor2 = new TreeEditor(item1.getItem(j).getParent());
					Button button = new Button(item1.getItem(j).getParent(), SWT.PUSH);
					button.setData("parentIndex", i);
					button.setData("childIndex", j);

					button.addPaintListener( new PaintListener() {
						  //@Override
						  public void paintControl( PaintEvent event ) {
							  event.gc.setBackground( event.display.getSystemColor( SWT.COLOR_WHITE ) );
							  event.gc.fillRectangle( event.x, event.y, event.width, event.height );						  }
					});
					button.setText("Child");

					button.setSize(3, 3);
					button.pack();

					button.addSelectionListener(new SelectionListener() {
						public void widgetSelected(SelectionEvent event) {
							Integer parentIndex = (Integer) event.widget.getData("parentIndex");
							Integer childIndex = (Integer) event.widget.getData("childIndex");
							pressChildRefactorButton(parentIndex, childIndex);
						}

						public void widgetDefaultSelected(SelectionEvent event) {
						}
					});

					editor2.horizontalAlignment = SWT.RIGHT;
					editor2.grabHorizontal = true;
					editor2.minimumWidth = 50;
					editor2.setEditor(button, item1.getItem(j), columnIndex);
					this.getChildrenButtonList().get(i).add(button);
				}
			}
		}

		@Override
		public void makeRefactoringButtons(int columnIndex) {
			TreeItem[] items = tree.getItems();
			for(int i = 0; i < items.length; i++) {
				TreeItem item1 = items[i];
				TreeEditor editor = new TreeEditor(item1.getParent());
				Button button = new Button(item1.getParent(), SWT.PUSH);

				button.setText("TEST");
				button.setSize(16, 16);
				button.pack();
				button.setData("index", i);

				editor.horizontalAlignment = SWT.RIGHT;
				editor.grabHorizontal = true;
				editor.minimumWidth = 50;
				editor.setEditor(button, item1, columnIndex);
				buttonList.add(button);
				button.addSelectionListener(new SelectionListener() {
					public void widgetSelected(SelectionEvent event) {
						pressRefactorButton((Integer) event.widget.getData("index"));
					}

					public void widgetDefaultSelected(SelectionEvent event) {
					}
				});

				button.addPaintListener(new PaintListener() {
					public void paintControl( PaintEvent event ) {
						  event.gc.setBackground( event.display.getSystemColor( SWT.COLOR_WHITE ) );
						  event.gc.fillRectangle( event.x, event.y, event.width, event.height );
						  Image image = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, "/icons/refactoring_button.png").createImage();
						  event.gc.drawImage( image, event.width/2-8, event.height/2-8 );
					}
				});

			}
		}

		/**
		 * Action on Refactoring Button
		 *
		 * @author 손태영, 이주용
		 */
		public void pressRefactorButton(int index) {
			ClassObjectCandidate targetClass = _smellingClassEntries[index];

			SGRefactorWizard wizard = new SGRefactorWizard(targetClass, _classObjectToBeExamined, identifyBadSmellsAction, activeProject);
			WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
			dialog.open();
		}
	}

	public class ViewContentProvider implements ITreeContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object arg) {
			if(_smellingClassEntries!=null) {
				return _smellingClassEntries;
			}
			else {
				return new ClassObjectCandidate[] {};
			}
		}

		public Object getParent(Object arg0) {
			if(arg0 instanceof ClassObjectCandidate) {
				ClassObjectCandidate target = (ClassObjectCandidate)arg0;

				for(int i=0; i<_smellingClassEntries.length; i++) {
					if(_smellingClassEntries[i].getName().equals(target.getName())) {
						return _smellingClassEntries[i];
					}
				}
			}

			return null;
		}

		public Object[] getChildren(Object arg) {
			if(arg instanceof ClassObjectCandidate){
				return ((ClassObjectCandidate) arg).getSmellingMethods().toArray();
			} else {
				ClassObjectCandidate[] res = { };
				return res;
			}
		}

		public boolean hasChildren(Object arg0) {
			return getChildren(arg0).length > 0;
		}
	}

	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if(obj instanceof ClassObjectCandidate) {
				ClassObjectCandidate entry = (ClassObjectCandidate)obj;
				switch(index){
				case 0:
					return entry.getName();
				case 1:
					IFile sourceFile = entry.getIFile();
					return sourceFile.getFullPath().toString();
				case 2:
					return entry.getCodeSmellType();
				case 3:
					return entry.getRefactorType();
				default:
					return "";
				}
			} else if(obj instanceof MethodObject) {
				MethodObject entry = (MethodObject)obj;
				switch(index){
				case 0:
					return entry.getName();
				case 1:
					return "";
				case 2:
					return "";
				case 3:
					return "";
				default:
					return "";
				}
			}
			return "";
		}

		public Image getColumnImage(Object obj, int index) {
			Image image = null;
			return image;
		}
		public Image getImage(Object obj) {
			return null;
		}
	}


	private ISelectionListener selectionListener = new ISelectionListener() {
		public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection)selection;
				Object element = structuredSelection.getFirstElement();
				IJavaProject javaProject = null;
				if(element instanceof IJavaProject) {
					javaProject = (IJavaProject)element;
					selectedPackageFragmentRoot = null;
					selectedPackageFragment = null;
					selectedCompilationUnit = null;
					selectedType = null;
					selectedMethod = null;
				}
				else if(element instanceof IPackageFragmentRoot) {
					IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot)element;
					javaProject = packageFragmentRoot.getJavaProject();
					selectedPackageFragmentRoot = packageFragmentRoot;
					selectedPackageFragment = null;
					selectedCompilationUnit = null;
					selectedType = null;
					selectedMethod = null;
				}
				else if(element instanceof IPackageFragment) {
					IPackageFragment packageFragment = (IPackageFragment)element;
					javaProject = packageFragment.getJavaProject();
					selectedPackageFragment = packageFragment;
					selectedPackageFragmentRoot = null;
					selectedCompilationUnit = null;
					selectedType = null;
					selectedMethod = null;
				}
				else if(element instanceof ICompilationUnit) {
					ICompilationUnit compilationUnit = (ICompilationUnit)element;
					javaProject = compilationUnit.getJavaProject();
					selectedCompilationUnit = compilationUnit;
					selectedPackageFragmentRoot = null;
					selectedPackageFragment = null;
					selectedType = null;
					selectedMethod = null;
				}
				else if(element instanceof IType) {
					IType type = (IType)element;
					javaProject = type.getJavaProject();
					selectedType = type;
					selectedPackageFragmentRoot = null;
					selectedPackageFragment = null;
					selectedCompilationUnit = null;
					selectedMethod = null;
				}
				else if(element instanceof IMethod) {
					IMethod method = (IMethod)element;
					javaProject = method.getJavaProject();
					selectedMethod = method;
					selectedPackageFragmentRoot = null;
					selectedPackageFragment = null;
					selectedCompilationUnit = null;
					selectedType = null;
				}
				if(javaProject != null && !javaProject.equals(selectedProject)) {
					selectedProject = javaProject;
					/*if(sliceTable != null)
						tableViewer.remove(sliceTable);*/
					identifyBadSmellsAction.setEnabled(true);
				}
			}
		}
	};

	@Override
	public void createPartControl(Composite parent) {
		refactorButtonMaker = new SpeculativeGeneralityRefactoringButtonUI();
		treeViewer = new TreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		treeViewer.setContentProvider(new ViewContentProvider());
		treeViewer.setLabelProvider(new ViewLabelProvider());

		treeViewer.setInput(getViewSite());
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(60, true));
		layout.addColumnData(new ColumnWeightData(20, true));
		layout.addColumnData(new ColumnWeightData(40, true));
		layout.addColumnData(new ColumnWeightData(20, true));
		layout.addColumnData(new ColumnWeightData(20, true));
		layout.addColumnData(new ColumnWeightData(20, true));
		treeViewer.getTree().setLayout(layout);
		treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		treeViewer.getTree().setLinesVisible(true);
		treeViewer.getTree().setHeaderVisible(true);
		TreeColumn column0 = new TreeColumn(treeViewer.getTree(),SWT.LEFT);
		column0.setText("Target");
		column0.setResizable(true);
		column0.pack();
		TreeColumn column1 = new TreeColumn(treeViewer.getTree(),SWT.LEFT);
		column1.setText("Source Path");
		column1.setResizable(true);
		column1.pack();
		TreeColumn column2 = new TreeColumn(treeViewer.getTree(),SWT.LEFT);
		column2.setText("Code Smell Type");
		column2.setResizable(true);
		column2.pack();
		TreeColumn column3 = new TreeColumn(treeViewer.getTree(),SWT.LEFT);
		column3.setText("Refactoring Type");
		column3.setResizable(true);
		column3.pack();
		TreeColumn column4 = new TreeColumn(treeViewer.getTree(),SWT.LEFT);
		column4.setText("Apply Refactoring");
		column4.setResizable(true);
		column4.pack();
		treeViewer.expandAll();

		treeViewer.setColumnProperties(new String[] {"type", "source", "variable", "block", "duplicationRatio", "rate"});
		treeViewer.setCellEditors(new CellEditor[] {
				new TextCellEditor(), new TextCellEditor(), new TextCellEditor(), new TextCellEditor(), new TextCellEditor(),
				new MyComboBoxCellEditor(treeViewer.getTree(), new String[] {"0", "1", "2", "3", "4", "5"}, SWT.READ_ONLY)
		});

		treeViewer.setCellModifier(new ICellModifier() {
			public boolean canModify(Object element, String property) {
				return property.equals("rate");
			}

			public Object getValue(Object element, String property) {
				if(element instanceof ASTSlice) {
					ASTSlice slice = (ASTSlice)element;
					if(slice.getUserRate() != null)
						return slice.getUserRate();
					else
						return 0;
				}

				if(element instanceof ClassObject) {
				}

				return 0;
			}

			public void modify(Object element, String property, Object value) {
				TreeItem item = (TreeItem)element;
				Object data = item.getData();
				if(data instanceof ASTSlice) {
					ASTSlice slice = (ASTSlice)data;
					slice.setUserRate((Integer)value);
					IPreferenceStore store = Activator.getDefault().getPreferenceStore();
					boolean allowUsageReporting = store.getBoolean(PreferenceConstants.P_ENABLE_USAGE_REPORTING);
					if(allowUsageReporting) {
						Tree tree = treeViewer.getTree();
						int groupPosition = -1;
						int totalGroups = tree.getItemCount();
						for(int i=0; i<tree.getItemCount(); i++) {
							TreeItem treeItem = tree.getItem(i);
							ASTSliceGroup group = (ASTSliceGroup)treeItem.getData();
							if(group.getCandidates().contains(slice)) {
								groupPosition = i;
								break;
							}
						}
						try {
							boolean allowSourceCodeReporting = store.getBoolean(PreferenceConstants.P_ENABLE_SOURCE_CODE_REPORTING);
							String declaringClass = slice.getSourceTypeDeclaration().resolveBinding().getQualifiedName();
							String methodName = slice.getSourceMethodDeclaration().resolveBinding().toString();
							String sourceMethodName = declaringClass + "::" + methodName;
							String content = URLEncoder.encode("project_name", "UTF-8") + "=" + URLEncoder.encode(activeProject.getElementName(), "UTF-8");
							content += "&" + URLEncoder.encode("source_method_name", "UTF-8") + "=" + URLEncoder.encode(sourceMethodName, "UTF-8");
							content += "&" + URLEncoder.encode("variable_name", "UTF-8") + "=" + URLEncoder.encode(slice.getLocalVariableCriterion().resolveBinding().toString(), "UTF-8");
							content += "&" + URLEncoder.encode("block", "UTF-8") + "=" + URLEncoder.encode("B" + slice.getBoundaryBlock().getId(), "UTF-8");
							content += "&" + URLEncoder.encode("object_slice", "UTF-8") + "=" + URLEncoder.encode(slice.isObjectSlice() ? "1" : "0", "UTF-8");
							int numberOfSliceStatements = slice.getNumberOfSliceStatements();
							int numberOfDuplicatedStatements = slice.getNumberOfDuplicatedStatements();
							content += "&" + URLEncoder.encode("duplicated_statements", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(numberOfDuplicatedStatements), "UTF-8");
							content += "&" + URLEncoder.encode("extracted_statements", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(numberOfSliceStatements), "UTF-8");
							content += "&" + URLEncoder.encode("ranking_position", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(groupPosition), "UTF-8");
							content += "&" + URLEncoder.encode("total_opportunities", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(totalGroups), "UTF-8");
							if(allowSourceCodeReporting) {
								content += "&" + URLEncoder.encode("source_method_code", "UTF-8") + "=" + URLEncoder.encode(slice.getSourceMethodDeclaration().toString(), "UTF-8");
								content += "&" + URLEncoder.encode("slice_statements", "UTF-8") + "=" + URLEncoder.encode(slice.sliceToString(), "UTF-8");
							}
							content += "&" + URLEncoder.encode("rating", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(slice.getUserRate()), "UTF-8");
							content += "&" + URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(System.getProperty("user.name"), "UTF-8");
							content += "&" + URLEncoder.encode("tb", "UTF-8") + "=" + URLEncoder.encode("2", "UTF-8");
							URL url = new URL(Activator.RANK_URL);
							URLConnection urlConn = url.openConnection();
							urlConn.setDoInput(true);
							urlConn.setDoOutput(true);
							urlConn.setUseCaches(false);
							urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
							DataOutputStream printout = new DataOutputStream(urlConn.getOutputStream());
							printout.writeBytes(content);
							printout.flush();
							printout.close();
							DataInputStream input = new DataInputStream(urlConn.getInputStream());
							input.close();
						} catch (IOException ioe) {
							ioe.printStackTrace();
						}
					}
					treeViewer.update(data, null);
				}

			}

		});
		makeActions();
		hookDoubleClickAction();
		contributeToActionBars();
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);
		JavaCore.addElementChangedListener(ElementChangedListener.getInstance());
		getSite().getWorkbenchWindow().getWorkbench().getOperationSupport().getOperationHistory().addOperationHistoryListener(new IOperationHistoryListener() {
			public void historyNotification(OperationHistoryEvent event) {
				int eventType = event.getEventType();
				if(eventType == OperationHistoryEvent.UNDONE  || eventType == OperationHistoryEvent.REDONE ||
						eventType == OperationHistoryEvent.OPERATION_ADDED || eventType == OperationHistoryEvent.OPERATION_REMOVED) {
					if(activeProject != null && CompilationUnitCache.getInstance().getAffectedProjects().contains(activeProject)) {
						applyRefactoringAction.setEnabled(false);
					}
				}
			}
		});
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(identifyBadSmellsAction);
		//manager.add(applyRefactoringAction);
		//manager.add(evolutionAnalysisAction);
	}

	private void makeActions() {
		identifyBadSmellsAction = new Action() {
			public void run() {
				activeProject = selectedProject;
				CompilationUnitCache.getInstance().clearCache();

				setClassObjectToBeExamined();
				setSmellingClassEntries( _classObjectToBeExamined );

				treeViewer.setContentProvider(new ViewContentProvider());

				//applyRefactoringAction.setEnabled(true);
				//evolutionAnalysisAction.setEnabled(true);

				refactorButtonMaker.disposeButtons();

				Tree tree = treeViewer.getTree();
				refactorButtonMaker.setTree(tree);

				refactorButtonMaker.makeRefactoringButtons(4);
				tree.addListener(SWT.Expand, new Listener() {
					public void handleEvent(Event e) {
						refactorButtonMaker.makeChildrenRefactoringButtons(4);
					}
				});
			}
		};
		ImageDescriptor refactoringButtonImage = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, "/icons/search_button.png");
		identifyBadSmellsAction.setToolTipText("Identify Bad Smells");
		//identifyBadSmellsAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		identifyBadSmellsAction.setImageDescriptor(refactoringButtonImage);
		identifyBadSmellsAction.setEnabled(false);

		applyRefactoringAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
				if(selection != null && selection.getFirstElement() instanceof ClassObjectCandidate) {
					IFile sourceFile = ((ClassObjectCandidate) selection.getFirstElement()).getIFile();
				} else if(selection != null && selection.getFirstElement() instanceof MethodObject) {
				}
			}
		};

		applyRefactoringAction.setToolTipText("Apply Refactoring");
		applyRefactoringAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_DEF_VIEW));
		applyRefactoringAction.setEnabled(false);

		doubleClickAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
				if(selection.getFirstElement() instanceof ClassObjectCandidate) {
					ClassObjectCandidate slice = (ClassObjectCandidate)selection.getFirstElement();
					IFile sourceFile = slice.getIFile();
					try {
						IJavaElement sourceJavaElement = JavaCore.create(sourceFile);
						ITextEditor sourceEditor = (ITextEditor)JavaUI.openInEditor(sourceJavaElement);
						Object[] highlightPositionMaps = slice.getHighlightPositions();
						Map<Position, String> annotationMap = (Map<Position, String>)highlightPositionMaps[0];
						AnnotationModel annotationModel = (AnnotationModel)sourceEditor.getDocumentProvider().getAnnotationModel(sourceEditor.getEditorInput());
						Iterator<Annotation> annotationIterator = annotationModel.getAnnotationIterator();
						while(annotationIterator.hasNext()) {
							Annotation currentAnnotation = annotationIterator.next();
							if(currentAnnotation.getType().equals(SliceAnnotation.EXTRACTION) || currentAnnotation.getType().equals(SliceAnnotation.DUPLICATION)) {
								annotationModel.removeAnnotation(currentAnnotation);
							}
						}
						for(Position position : annotationMap.keySet()) {
							SliceAnnotation annotation = null;
							String annotationText = annotationMap.get(position);
							annotation = new SliceAnnotation(SliceAnnotation.EXTRACTION, annotationText);
							annotationModel.addAnnotation(annotation, position);
						}
						List<Position> positions = new ArrayList<Position>(annotationMap.keySet());
						Position firstPosition = positions.get(0);
						Position lastPosition = positions.get(positions.size()-1);
						int offset = firstPosition.getOffset();
						int length = lastPosition.getOffset() + lastPosition.getLength() - firstPosition.getOffset();
						sourceEditor.setHighlightRange(offset, length, true);
					} catch (PartInitException e) {
						e.printStackTrace();
					} catch (JavaModelException e) {
						e.printStackTrace();
					}

				}
				else if(selection.getFirstElement() instanceof MethodObject) {
		               MethodObject slice = (MethodObject)selection.getFirstElement();
		               IFile sourceFile = slice.getIFile();
		               try {
		                  IJavaElement sourceJavaElement = JavaCore.create(sourceFile);
		                  ITextEditor sourceEditor = (ITextEditor)JavaUI.openInEditor(sourceJavaElement);
		                  Object[] highlightPositionMaps = slice.getHighlightPositions();
		                  Map<Position, String> annotationMap = (Map<Position, String>)highlightPositionMaps[0];
		                  AnnotationModel annotationModel = (AnnotationModel)sourceEditor.getDocumentProvider().getAnnotationModel(sourceEditor.getEditorInput());
		                  Iterator<Annotation> annotationIterator = annotationModel.getAnnotationIterator();
		                  while(annotationIterator.hasNext()) {
		                     Annotation currentAnnotation = annotationIterator.next();
		                     if(currentAnnotation.getType().equals(SliceAnnotation.EXTRACTION) || currentAnnotation.getType().equals(SliceAnnotation.DUPLICATION)) {
		                        annotationModel.removeAnnotation(currentAnnotation);
		                     }
		                  }
		                  for(Position position : annotationMap.keySet()) {
		                     SliceAnnotation annotation = null;
		                     String annotationText = annotationMap.get(position);
		                     annotation = new SliceAnnotation(SliceAnnotation.EXTRACTION, annotationText);
		                     annotationModel.addAnnotation(annotation, position);
		                  }
		                  List<Position> positions = new ArrayList<Position>(annotationMap.keySet());
		                  Position firstPosition = positions.get(0);
		                  Position lastPosition = positions.get(positions.size()-1);
		                  int offset = firstPosition.getOffset();
		                  int length = lastPosition.getOffset() + lastPosition.getLength() - firstPosition.getOffset();
		                  sourceEditor.setHighlightRange(offset, length, true);
		               } catch (PartInitException e) {
		                  e.printStackTrace();
		               } catch (JavaModelException e) {
		                  e.printStackTrace();
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

	/**
	 * @author JaeYeop Lee, JuYong Lee
	 */
	public void setClassObjectToBeExamined() {
		try {
			IWorkbench wb = PlatformUI.getWorkbench();
			IProgressService ps = wb.getProgressService();

			if(ASTReader.getSystemObject() != null && activeProject.equals(ASTReader.getExaminedProject())) {
				new ASTReader(activeProject, ASTReader.getSystemObject(), null);
			}
			else {
				ps.busyCursorWhile(new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							new ASTReader(activeProject, monitor);
						} catch (CompilationErrorDetectedException e) {
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), MESSAGE_DIALOG_TITLE,
											"Compilation errors were detected in the project. Fix the errors before using JDeodorant.");
								}
							});
						}
					}
				});
			}


			SystemObject systemObject = ASTReader.getSystemObject();
			if(systemObject != null) {
				Set<ClassObject> classObjectsToBeExamined = new HashSet<ClassObject>();
				// Package Selected
				if(selectedPackageFragment != null) {
					classObjectsToBeExamined.addAll(systemObject.getClassObjects(selectedPackageFragment));
				}
				else {
					classObjectsToBeExamined.addAll(systemObject.getClassObjects());
				}

				// Duplicated Entry Deletion
				this._classObjectToBeExamined = new HashSet<ClassObject>();
				for (ClassObject co : classObjectsToBeExamined) {
					boolean flagExistence = false;
					for(ClassObject _co : this._classObjectToBeExamined) {
						if( co.getName().equals(_co.getName()) ) {
							flagExistence = true;
							break;
						}
					}

					if(!flagExistence) {
						this._classObjectToBeExamined.add(co);
					}
				}
			}
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (CompilationErrorDetectedException e) {
			MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), MESSAGE_DIALOG_TITLE,
					"Compilation errors were detected in the project. Fix the errors before using JDeodorant.");
		}


		return;
	}

	public void setClassObjectToBeExamined(Set<ClassObject> arg) {
		this._classObjectToBeExamined = arg;
	}

	/**
	 *  Get the class objects to be examined
	 *  @return classObjectToBeExamined
	 */
	public Set<ClassObject> getClassObjectToBeExamined() {
		return this._classObjectToBeExamined;
	}

	/**
	 *  Examine the class objects
	 *  @author JaeYeop Lee, JuYong Lee
	 *  @param classObjectsToBeExamined
	 *  @return Array of ClassObject
	 */
	public void setSmellingClassEntries(final Set<ClassObject> classObjectsToBeExamined){
		final List<ClassObjectCandidate> smellingClassObjectCandidates = new ArrayList<ClassObjectCandidate>();

		for(ClassObject targetClass : classObjectsToBeExamined) {
			// Abstract Class
			if(targetClass.isAbstract()) {
				ClassObjectCandidate target = new ClassObjectCandidate(targetClass);

				int childOfTargetNum = 0;

				for(ClassObject childCandidate : classObjectsToBeExamined) {
					TypeObject superClass = childCandidate.getSuperclass();
					if(superClass == null) {
						continue;
					}

					if(superClass.getClassType().equals(targetClass.getName())){
						childOfTargetNum++;
					}


					if(childOfTargetNum >= 2)
						break;
				}

				if(childOfTargetNum < 2) {
					target.setNumChild(childOfTargetNum);
					target.setCodeSmellType("Abstract Class");
					target.setRefactorType("Merge Class");
					smellingClassObjectCandidates.add(target);
				}
			}

			// Interface Class
			else if(targetClass.isInterface()) {
				ClassObjectCandidate target = new ClassObjectCandidate(targetClass);

				int childOfTargetNum = 0;

				for(ClassObject childCandidate : classObjectsToBeExamined) {
					ListIterator<TypeObject> myIter=childCandidate.getInterfaceIterator();
					while(myIter.hasNext()) {

						if(myIter.next().getClassType().equals(targetClass.getName())) {
							childOfTargetNum++;
							break;
						}
					}

					if(childOfTargetNum >= 2)
						break;
				}
				if(childOfTargetNum < 2) {

					target.setNumChild(childOfTargetNum);
					target.setCodeSmellType("Interface Class");
					target.setRefactorType("Merge Class");
					smellingClassObjectCandidates.add(target);
				}
			}

			// Unnecsary Parameter
			if(!targetClass.isEnum() && !targetClass.isInterface() && !targetClass.isGeneratedByParserGenenator()) {
				ClassObjectCandidate target = new ClassObjectCandidate(targetClass);
				target.setCodeSmellType("Unnecessary Parameters");

				List<MethodObject> _methodList = target.getMethodList();
				for(int i = 0; i < _methodList.size(); i++) {
					MethodObject targetMethod = _methodList.get(i);

					targetMethod.setparentClass(target);
					List<String> unusedPList = new ArrayList<String>();
					int unusedPNum = 0;

					// GetStatements
					String methodBodyStatements = null;
					MethodBodyObject _methodBody = targetMethod.getMethodBody();
					if (_methodBody != null) {
						CompositeStatementObject _compositeStatement = _methodBody.getCompositeStatement();
						if (_compositeStatement != null) {
							Statement _statement = _compositeStatement.getStatement();
							if (_statement != null) {
								methodBodyStatements = _statement.toString();
							}
						}
					}

					if (methodBodyStatements != null) {
						// Get Parameters
						int pNum = targetMethod.getParameterList().size();

						for (int p = 0; p < pNum; p++) {
							// Check the Usage
							String pTarget = targetMethod.getParameter(p).getName();

							if (!this.checkContainance(methodBodyStatements, pTarget)) {
								unusedPList.add(pTarget);
								unusedPNum++;
							}
						}
					}

					if(unusedPNum > 0) {
						// Check Overriding
						boolean flag_overriding = false;
						for(ClassObject parentCandidate : this._classObjectToBeExamined) {
							TypeObject superClass = targetClass.getSuperclass();

							if(superClass != null && targetClass.getSuperclass().getClassType().equals(parentCandidate.getName())) {
								for( Iterator<MethodObject> itr = parentCandidate.getMethodIterator(); itr.hasNext(); )	{
									MethodObject tmp = itr.next();
									if(tmp.getName().equals(targetMethod.getName())) {
										flag_overriding = true;
									}
								}
							}
						}
						ListIterator<TypeObject> parentClasses = targetClass.getInterfaceIterator();
						while (parentClasses.hasNext()) {
							TypeObject superClass = parentClasses.next();
							for(ClassObject parentCandidate : this._classObjectToBeExamined) {
								if (superClass != null && superClass.getClassType().equals(parentCandidate.getName())) {
									for (Iterator<MethodObject> itr = parentCandidate.getMethodIterator(); itr.hasNext();) {
										MethodObject tmp = itr.next();
										if (tmp.getName().equals(targetMethod.getName())) {
											flag_overriding = true;
										}
									}
								}
							}
						}

						if(!flag_overriding) {
							targetMethod.setSmellStart(targetMethod.getMethodDeclaration().getStartPosition());
							targetMethod.setSmellLength(targetMethod.getMethodDeclaration().getLength());
							target.addSmellingMethod(targetMethod);
						}

					}
				}

				if(target.getSmellingMethods().size() > 0) {
					target.setRefactorType("Parameter Method");
					smellingClassObjectCandidates.add(target);
				}
			}
		}

		this._smellingClassEntries = new ClassObjectCandidate[smellingClassObjectCandidates.size()];
		for(int i=0;i<smellingClassObjectCandidates.size();i++) {
			this._smellingClassEntries[i]= smellingClassObjectCandidates.get(i);
		}

		 return;
	}

	/**
	 *  Get the class objects
	 *  @return smellingClassEntries
	 */
	public ClassObjectCandidate[] getSmellingClassEntries() {
		return this._smellingClassEntries;
	}

	/**
	TestClass * Check "content" contains "target"
	 *
	 * @author JuYong Lee
	 * @param method
	 * @param var
	 * @return
	 */
	public boolean checkContainance(String content, String target) {
		String[] operator = { " ", "(", ")", "[", "]", ".",	"+", "-", "*", "/", "%",
				"!", "~", "++", "--", "<<", ">>", ">>>", ">", "<", ">= ", "<=", "==", "!=",
				"&", "^", "|", "&&", "||", "?", "=", "+=", "/=", "&=", "*=", "-=",
				"<<=", ">>=", ">>>=", "^=", "|=", "%=", ";"};

		for(int i = 0; i < operator.length; i++) {
			for(int j = 0; j < operator.length; j++) {
				if(content.contains(operator[i] + target + operator[j])) {
					return true;
				}
			}
		}

		return false;
	}
}

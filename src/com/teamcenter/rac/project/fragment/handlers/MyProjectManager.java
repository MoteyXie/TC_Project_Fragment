package com.teamcenter.rac.project.fragment.handlers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;

import com.teamcenter.rac.aifrcp.AIFUtility;
import com.teamcenter.rac.common.lov.view.controls.LOVDisplayer;
import com.teamcenter.rac.kernel.TCComponentProject;
import com.teamcenter.rac.kernel.TCComponentType;
import com.teamcenter.rac.kernel.TCException;
import com.teamcenter.rac.kernel.TCPropertyDescriptor;
import com.teamcenter.rac.kernel.TCSession;
import com.teamcenter.rac.project.views.ProjectDefinitionView;
import com.teamcenter.rac.util.AdapterUtil;
import com.teamcenter.rac.util.MessageBox;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public class MyProjectManager implements KeyListener, MouseListener, IPropertyChangeListener{
	
	public static final String PREFERENCE_NAME = "SIE_Project_Definition_Properties";
	public static final String HIDE_PROP_PREFERENCE_NAME = "SIE_Project_Hide_Properties";
	public static Map<ProjectDefinitionView, MyProjectManager> PROJECT_DEFINITION_MANAGER_MAP = new HashMap<>();
	
	private ProjectDefinitionView projectDefinitionView;
	
	public static MyProjectManager getProjectManager(ProjectDefinitionView projectDefinitionView) {
		
		if(PROJECT_DEFINITION_MANAGER_MAP.containsKey(projectDefinitionView)) {
			return PROJECT_DEFINITION_MANAGER_MAP.get(projectDefinitionView);
		}else {
			MyProjectManager pm = new MyProjectManager(projectDefinitionView);
			PROJECT_DEFINITION_MANAGER_MAP.put(projectDefinitionView, pm);
			return pm;
		}
	}

	public static void inject() throws Exception {
		
		ClassPool pool = ClassPool.getDefault();
		
		Class<?> cls = com.teamcenter.rac.project.plugin.Activator.class;
		
		ClassLoader loader = cls.getClassLoader();
		
		ClassClassPath cp = new ClassClassPath(cls);
		
		pool.insertClassPath(cp);
		
		pool.importPackage("java.util");
		pool.importPackage("com.teamcenter.rac.project.fragment.handlers.MyProjectManager");
		
		CtClass cc = pool.get("com.teamcenter.rac.project.views.ProjectDefinitionView");
		
		CtMethod ctMethod = cc.getDeclaredMethod("createTopForm", new CtClass[] {pool.get("org.eclipse.swt.widgets.Composite")});
		ctMethod.insertAfter("{MyProjectManager.getProjectManager(this).afterCreateTopForm($1);}");
		
		ctMethod = cc.getDeclaredMethod("processSetInput", new CtClass[] {pool.get("org.eclipse.ui.IWorkbenchPart"),pool.get("java.util.List")});
		ctMethod.insertAfter("{MyProjectManager.getProjectManager(this).afterProcessSetInput($1,$2);}");
		
		ctMethod = cc.getDeclaredMethod("validateButtons");
		ctMethod.insertAfter("{MyProjectManager.getProjectManager(this).afterValidateButtons();}");
		
		//给每个方法注入一个标记
//		CtMethod[] methods = cc.getDeclaredMethods();
//		for (CtMethod ctMethod2 : methods) {
//			try {
//				System.out.println(ctMethod2.getName());
//				ctMethod2.insertAfter("{MyProjectManager.test(this, \"" + ctMethod2.getName() + "\");}");
//			}catch(Exception e) {
//				System.out.println(e.getMessage());
//			}
//			
//		}
		cc.toClass(loader, null);
		
	}
	
	public static void test(Object obj, String str) {
		System.out.println(obj.getClass() + " : " + str);
	}
	
	private Map<String, Control> propertiesMap = new HashMap<>();
	private Map<String, Control> mandatoryPropertiesMap = new HashMap<>();

	public MyProjectManager(ProjectDefinitionView projectDefinitionView) {
		this.projectDefinitionView = projectDefinitionView;
		try {
			//创建和修改后操作在projectUIPostActions类中
			setValue(projectDefinitionView, "projectUIPostActions", new MyProjectUIPostActions(this));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setValue(Object obj, String name, Object value) throws Exception {
		Field field = obj.getClass().getDeclaredField(name);
	    field.setAccessible(true);
	    field.set(obj, value);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getValue(Object obj, String name) throws Exception {
		Field field = obj.getClass().getDeclaredField(name);
	    field.setAccessible(true);
	    return (T)field.get(obj);
	    
	}
	
	static void access$7(final ProjectDefinitionView projectDefinitionView, final boolean isDirty) throws Exception {
        Field field = projectDefinitionView.getClass().getDeclaredField("isDirty");
	    field.setAccessible(true);
	    field.set(projectDefinitionView, isDirty);
    }
	
	public void afterProcessSetInput(final IWorkbenchPart workbenchPart, final List<Object> list) {
		
		final Object o = list.isEmpty() ? null : list.get(0);
		
		TCComponentProject project = (TCComponentProject)AdapterUtil.getAdapter(o, TCComponentProject.class, true);
	
		try {
			loadSpecProperteies(project);
		} catch (TCException e) {
			e.printStackTrace();
			MessageBox.post(e);
		}
	}
	
	public void afterCreateTopForm(Composite var1) {
		
		try {
			
			Control[] controls = var1.getChildren();
			
			Composite composite = (Composite)controls[0];
			
			String[] hideProperties = getConfigHideProperties();
			
			if(hideProperties != null && hideProperties.length > 0) {
				
				List<String> hidePropList = new ArrayList<String>(Arrays.asList(hideProperties));
				
				Control[] childControls = composite.getChildren();
			    
				//隐藏指定的属性
			    for(int i = 0; i < childControls.length; i++) {
			    	
			    	if(!childControls[i].isDisposed() && childControls[i] instanceof Label) {
			    		if(hidePropList.contains(((Label)childControls[i]).getText())){
			    			childControls[i].dispose();
			    			for(i = i + 1;i < childControls.length; i++) {
			    				if(childControls[i] instanceof Label) {
			    					Label lb = (Label)childControls[i];
			    					String lbt = lb.getText();
			    					if(lbt != null && !lbt.equals("*") && !lbt.trim().isEmpty()) {
			    						break;
			    					}
			    					childControls[i].dispose();
			    				}
			    				childControls[i].dispose();
			    			}
			    		}
			    	}
			    	
			    }
			}
			
			//新增的属性
			String[] configProperties = getConfigProperties();
		    if(configProperties == null || configProperties.length == 0)return;
			
		    TCComponentType projectType = projectDefinitionView.getTCSession().getTypeComponent("TC_Project");
		    TCComponentProject project = getValue(projectDefinitionView, "project");
		    
		    for(int i = 0; i < configProperties.length; i++) {
		    	
		    	String configProperty = configProperties[i];
		    	boolean mandatory = false;
		    	if(configProperty.startsWith("*")) {
		    		configProperty = configProperty.substring(1, configProperty.length());
		    		mandatory = true;
		    	}
		    	TCPropertyDescriptor propDesc = projectType.getPropertyDescriptor(configProperty);
		    	if(propDesc == null) {
		    		continue;
		    	}
		    	
		    	Label nameLabel = projectDefinitionView.getToolkit().createLabel(composite, propDesc.getDisplayName());
		    	Label mandaryLabel = null;
		    	if(mandatory) {
		    		mandaryLabel = projectDefinitionView.getToolkit().createLabel(composite, "*");
		    		mandaryLabel.setForeground(composite.getDisplay().getSystemColor(3));
		    	}else {
		    		mandaryLabel = projectDefinitionView.getToolkit().createLabel(composite, "");
		    	}
		    	GridData var11 = new GridData(4, 4, false, false);
		    	mandaryLabel.setLayoutData(var11);
		    	
		    	Control propControl = null;
		    	if(propDesc.hasLOVAttached()) {
		    		LOVDisplayer lovDisplay = new LOVDisplayer(composite, 4);
		    		lovDisplay.initialize(project, propDesc, (Object)null);
				    projectDefinitionView.getToolkit().adapt(lovDisplay, true, true);
				    propControl = lovDisplay;
				    lovDisplay.addPropertyChangeListener(this);
				    
		    	}else {
		    		Text text = projectDefinitionView.getToolkit().createText(composite, "", 2626);;
		    		projectDefinitionView.getToolkit().adapt(text, true, true);
		    		propControl = text;
		    		text.addKeyListener(this);
		    		text.addMouseListener(this);
		    	}
		    	propertiesMap.put(configProperty, propControl);
		    	if(mandatory) {
		    		mandatoryPropertiesMap.put(configProperty, propControl);
		    	}
		    	
		    	GridData var8 = new GridData(16384, 16777216, true, false);
			    var8.widthHint = 250;
			    var8.heightHint = 25;
			    propControl.setLayoutData(var8);
		    }
		   
		    //加个空格控件
		    projectDefinitionView.getToolkit().createLabel(composite, "");
		    
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void afterValidateButtons() {
		TCComponentProject project = projectDefinitionView.getProject();
		if(project == null)return;
		boolean hasModified = false;
		try {
			for (Entry<String, Control> entry : propertiesMap.entrySet()) {
				
					String proValue = project.getProperty(entry.getKey());
					String inputValue = getControlValue(entry.getValue());
					
					if(!inputValue.equals(proValue)) {
						hasModified = true;
						break;
					}
			}
			if(hasModified) {
				Button button = getValue(projectDefinitionView, "m_modifyButton");
				button.setEnabled(true);
			}
		
			boolean hasNullMandatory = false;
			//必填项不得为空
			for (Entry<String, Control> entry : mandatoryPropertiesMap.entrySet()) {
				String inputValue = getControlValue(entry.getValue());
				if(inputValue == null || inputValue.isEmpty()) {
					hasNullMandatory = true;
					break;
				}
			}
			if(hasNullMandatory) {
				projectDefinitionView.disableButtons();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public void loadSpecProperteies(TCComponentProject project) throws TCException {
		if(project == null)return;
		for (Entry<String, Control> entry : propertiesMap.entrySet()) {
			String propName = entry.getKey();
			String value = project.getProperty(propName);
			setControlValue(entry.getValue(), value);
		}
	}
	
	public void saveSpecProperties(TCComponentProject project) throws TCException {
		if(propertiesMap.size() == 0)return;
		for (Entry<String, Control> entry : propertiesMap.entrySet()) {
			String propName = entry.getKey();
			String value = getControlValue(entry.getValue());
			project.setProperty(propName, value);
		}
	}
	
	public String getControlValue(Control control) {
		if(control == null)return null;
		if(control instanceof Text) {
			return ((Text) control).getText();
		}else if(control instanceof LOVDisplayer) {
			return (String) ((LOVDisplayer) control).getSelectedValue();
		}else {
			return "";
		}
	}
	
	public void setControlValue(Control control, String text) {
		if(control == null)return;
		if(control instanceof Text) {
			((Text) control).setText(text);
		}else if(control instanceof LOVDisplayer) {
			((LOVDisplayer) control).setSelectedValue(text);
		}
	}
	
	public String[] getConfigProperties() {
		TCSession session = (TCSession) AIFUtility.getDefaultSession();
		return session.getPreferenceService().getStringValues(PREFERENCE_NAME);
	}
	
	public String[] getConfigHideProperties() {
		TCSession session = (TCSession) AIFUtility.getDefaultSession();
		return session.getPreferenceService().getStringValues(HIDE_PROP_PREFERENCE_NAME);
	}
	
	
	public void keyReleased(final KeyEvent keyEvent) {
		try {
			if ((boolean)getValue(projectDefinitionView, "isPA") || (boolean)getValue(projectDefinitionView, "isPTA") || projectDefinitionView.isPTA()) {
	        	projectDefinitionView.validateButtons();
	        }
		}catch(Exception e) {
			e.printStackTrace();
		}
        
    }
    
    public void keyPressed(final KeyEvent keyEvent) {
        try {
			access$7(projectDefinitionView, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public void mouseMove(final MouseEvent mouseEvent) {
    	if(mouseEvent.widget instanceof Text) {
    		Text text = (Text) mouseEvent.widget;
    		if (text.getCharCount() > 50) {
    			text.setToolTipText(text.getText());
            }
            else {
            	text.setToolTipText("");
            }
    	}
    }
    
    public void propertyChange(final PropertyChangeEvent propertyChangeEvent) {
    	try {
        	boolean isPA = getValue(projectDefinitionView, "isPA");
		    boolean isPTA = getValue(projectDefinitionView, "isPTA");
            if (isPA || isPTA || projectDefinitionView.isPTA()) {
            	projectDefinitionView.validateButtons();
					access$7(projectDefinitionView, true);
            }
    	} catch (Exception e) {
			e.printStackTrace();
		}
    }

	@Override
	public void mouseDoubleClick(MouseEvent var1) {
		
	}

	@Override
	public void mouseDown(MouseEvent var1) {
		
	}

	@Override
	public void mouseUp(MouseEvent var1) {
		
	}
	
}

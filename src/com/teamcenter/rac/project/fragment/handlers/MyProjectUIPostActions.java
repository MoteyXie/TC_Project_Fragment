package com.teamcenter.rac.project.fragment.handlers;

import com.teamcenter.rac.kernel.TCComponentProject;
import com.teamcenter.rac.kernel.TCException;
import com.teamcenter.rac.project.ProjectUIPostActions;
import com.teamcenter.rac.util.MessageBox;

public class MyProjectUIPostActions extends ProjectUIPostActions {

	private MyProjectManager myProjectManager;
	
	public MyProjectUIPostActions(MyProjectManager myProjectManager) {
		this.myProjectManager = myProjectManager;
	}

	public void projectCreated(TCComponentProject var1) {
		System.out.println("afterProjectCreated");
		try {
			myProjectManager.saveSpecProperties(var1);
		} catch (Throwable e) {
			e.printStackTrace();
			MessageBox.post(e);
		}
	}

	public void projectModified(TCComponentProject var1, String var2, String var3) {
		System.out.println("afterProjectModified");
		try {
			myProjectManager.saveSpecProperties(var1);
		} catch (Throwable e) {
			e.printStackTrace();
			MessageBox.post(e);
		}
	}
	   
}

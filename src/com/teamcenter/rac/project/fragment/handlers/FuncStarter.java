package com.teamcenter.rac.project.fragment.handlers;

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IStartup;

public class FuncStarter implements IStartup {
	
	@Override
	public void earlyStartup() {
		try {
			MyProjectManager.inject();
			//MyPSEManager.inject();
			System.out.println("MyProjectManager action listener inject success!");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("MyPSEManager action listener inject fail!");
		}
	}
	
}

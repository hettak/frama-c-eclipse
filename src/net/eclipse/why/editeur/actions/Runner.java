package net.eclipse.why.editeur.actions;

import net.eclipse.why.editeur.FileInfos;
import net.eclipse.why.editeur.IConstants;
import net.eclipse.why.editeur.views.POViewer;
import net.eclipse.why.editeur.views.ProverView;
import net.eclipse.why.editeur.views.TraceView;
import net.eclipse.why.editeur.views.TraceView.MessageType;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;

/**
 * High level class used to initialize parameters, open views and run threads.
 * 
 * @author A. Oudot
 */
public class Runner implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;

	private static class MyJobListener implements IJobChangeListener {
		ProverView a;
		POViewer b;
		Composite parent;

		MyJobListener(ProverView a, POViewer b) {
			this.a = a;
			this.b = b;
			parent = a.getViewer().getParent();
		}

		public void aboutToRun(IJobChangeEvent event) {
			return;
		}

		public void awake(IJobChangeEvent event) {
			return;
		}

		public void done(IJobChangeEvent event) {
			parent.getDisplay().syncExec(new Runnable() {
				public void run() {
					a.updateView();
					b.update();
				}
			});
		}

		public void running(IJobChangeEvent event) {
			return;
		}

		public void scheduled(IJobChangeEvent event) {
			return;
		}

		public void sleeping(IJobChangeEvent event) {
			return;
		}
	}

	public void run(IAction action) {
		TraceView.init((IResource)window.getActivePage().getActiveEditor().getEditorInput().getAdapter(IResource.class));

		final VerificationToolExecutor cmd = new VerificationToolExecutor();
		// check the source file we're going to work on
		int result = cmd.checkOpenFile();
		if (result <= 0) {
			return;
		}

		// initialization
		FileInfos.reset();
		FileInfos.initProvers();

		try {
			ProverView proverView = showProverView();
			POViewer viewer = showPOViewer();

			// put and complete necessary fields
			FileInfos.setFile(cmd.getResource());
			FileInfos.complete();
			FileInfos.locateDoubleCharsInCFile();

			ConditionGeneratorJob verificationThread = new ConditionGeneratorJob(
					cmd);
			verificationThread.addJobChangeListener(new MyJobListener(
					proverView, viewer));
			verificationThread.schedule();
		} catch (Exception e) {
			TraceView
					.print(MessageType.ERROR, "Runner.run() can't open a view");
			e.printStackTrace();
		}
	}

	/**
	 * Show the Prover View<BR>
	 * 
	 * @return the current ProverView
	 * @throws PartInitException
	 */
	private ProverView showProverView() throws PartInitException {
		
		// get active page
		IWorkbenchPage activePage = window.getActivePage();
		// show view
		return (activePage == null) ? null : (ProverView) activePage
				.showView(IConstants.PROVER_VIEW_ID);
	}

	/**
	 * Show the POViewer<BR>
	 * 
	 * @return the opened POViewer
	 * @throws PartInitException
	 */
	private POViewer showPOViewer() throws PartInitException {
		// get active page
		IWorkbenchPage activePage = window.getActivePage();
		// show view
		return (activePage == null) ? null : (POViewer) activePage
				.showView(IConstants.PO_VIEW_ID);
	}

	/**
	 * Selection in the workbench has been changed. We can change the state of
	 * the 'real' action here if we want, but this can only happen after the
	 * delegate has been created.
	 * 
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system resources we previously
	 * allocated.
	 * 
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 * 
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}

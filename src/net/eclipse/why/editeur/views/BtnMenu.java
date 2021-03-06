package net.eclipse.why.editeur.views;

import net.eclipse.why.editeur.IConstants;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Menu for right-click actions on Prover View's goals' buttons
 * 
 * @author A. Oudot
 */
public class BtnMenu extends Menu {

	/**
	 * Menu constructor for right-click action
	 * on buttons of Prover View.
	 * 
	 * @param parent the parent Composite
	 */
	public BtnMenu(Control parent) {
		super(parent);
		
		MenuItem item1 = new MenuItem(this, SWT.NONE);
		item1.setText("Validate");
		item1.setImage(IConstants.IMAGE_BALL_GREEN);
		item1.setData("admit", Integer.valueOf(1));
		
		MenuItem item2 = new MenuItem(this, SWT.NONE);
		item2.setText("Invalidate");
		item2.setImage(IConstants.IMAGE_BALL_RED);
		item2.setData("admit", Integer.valueOf(2));
		
		//new Separator(IWorkbenchActionConstants.MB_ADDITIONS);
		new MenuItem(this, SWT.SEPARATOR);
		
		MenuItem item3 = new MenuItem(this, SWT.NONE);
		item3.setText("Reset");
		item3.setImage(IConstants.IMAGE_BALL_WHITE);
		item3.setData("admit", Integer.valueOf(0));
	}
	
	
	/**
	 * Suppress the subclassing exception overriding the
	 * Widget method and doing nothing into it
	 */
	protected void checkSubclass() {
		//do nothing!
	}
	
}

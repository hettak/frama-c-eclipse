package net.eclipse.why.editeur.views;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import net.eclipse.why.editeur.EditeurWHY;
import net.eclipse.why.editeur.FileInfos;
import net.eclipse.why.editeur.Function;
import net.eclipse.why.editeur.IConstants;
import net.eclipse.why.editeur.PO;
import net.eclipse.why.editeur.actions.Highlightor;
import net.eclipse.why.editeur.actions.ProverExecutor;
import net.eclipse.why.editeur.actions.ProverThread;
import net.eclipse.why.editeur.actions.ProverViewUpdater;
import net.eclipse.why.editeur.actions.XMLLoader;
import net.eclipse.why.editeur.actions.XMLSaver;
import net.eclipse.why.editeur.lexer.GoalDisplayModifier;
import net.eclipse.why.editeur.views.TraceView.MessageType;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

public class ProverView extends ViewPart {

	// Widgets
	private Tree viewer;

	// Actions
	private Action update;
	private Action reset;
	private Action showOnlyUnprovedGoals;
	private Action showAllGoals;
	private Action selectAction;
	private Action kill;
	private Action foldTree;
	private Action unfoldTree;
	private Action runAllProvers;
	private Action mark;
	private Action runAssistant;
	private Action save;
	private Action load;

	// Others
	private int proversNumber;
	private boolean showAllLines;
	private ArrayList<ProverThread> threads = new ArrayList<ProverThread>();
	private ArrayList<Integer> goalsInView = new ArrayList<Integer>();
	private ArrayList<Integer> functionsInView = new ArrayList<Integer>();
	private int columnSize = 220;

	/**
	 * Tree getter
	 */
	public Tree getViewer() {
		return viewer;
	}

	/**
	 * Prover SelectionListener : defines the action which is executed when the
	 * user clicks on a column button. The corresponding prover is executed on
	 * all goals. It gets the column and the prover number and if goal is marked
	 * it prove all goals beginning with this marked goal. If no goal is marked,
	 * all goals from the first one will be proved.
	 */
	private class ProverSelectionListener implements SelectionListener {

		public void widgetSelected(SelectionEvent e) {
			if (FileInfos.markedGoal > 0) {
				proveAll(false, !FileInfos.showOnlyUnprovedGoals);
			} else if (FileInfos.markedGoal == 0) { // if no goal is marked
				proveAll(true, !FileInfos.showOnlyUnprovedGoals);
			}
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			return;
		}
	}

	/**
	 * Goal button menu SelectionListener. It gets the clicked value : 0=reset,
	 * 1=admitted(proved), 2=unproved. Then it gets the menu's button and the
	 * corresponding goal, and prover. Then PO is extracted and changed.
	 */
	private class ButtonMenuSelectionListener implements SelectionListener {

		public void widgetSelected(SelectionEvent e) {

			try {

				MenuItem o = (MenuItem) e.widget;
				int a = (Integer) o.getData("admit");
				TreeItem p = viewer.getSelection()[0]; // gets the selected item
				Integer goal = (Integer)p.getData("goal");
				if (goal != null) {

					PO po = FileInfos.goals.get(goal - 1);
					po.setState(0, a);

					updateElementAt(goal);
					makeStats(0);
				}
			} catch (Exception exception) {
				TraceView.print(MessageType.ERROR,
						"ProverView.MenuItemSelectionListener.widgetSelected() :\n"
								+ exception);
			}
		}

		public void widgetDefaultSelected(SelectionEvent e) {

		}
	}

	/**
	 * Item listener for 'Collapse' action
	 */
	private class CollapseListener implements Listener {

		public void handleEvent(Event event) {
			TreeItem t = (TreeItem) event.item;
			int a = (Integer)t.getData("function");
			FileInfos.functions.get(a).collapse();
		}
	}

	/**
	 * Item listener for 'Expand' action
	 */
	private class ExpandListener implements Listener {

		public void handleEvent(Event event) {
			TreeItem t = (TreeItem) event.item;
			int a = (Integer)t.getData("function");
			FileInfos.functions.get(a).expand();
		}
	}

	/**
	 * Item Selection Listener
	 */
	private class CheckListener implements Listener {

		public void handleEvent(Event event) {

			selectAction.run();
		}
	}

	/**
	 * Keyboard listener : used when the user press F7, F8, F9, Ctrl-! or
	 * Ctrl-Shift-X keys
	 */
	private class PressListener implements KeyListener {

		public void keyPressed(KeyEvent e) {
		}

		public void keyReleased(KeyEvent e) {

			// use all provers to prove all unproved goals
			if (e.keyCode == SWT.F7) {
				runAllProvers.run();
			}

			// stop provers
			if (e.keyCode == SWT.F8) {
				kill.run();
			}

			// mark a goal
			if (e.keyCode == SWT.F9) {
				mark.run();
			}

			// update the view
			if (e.stateMask == SWT.CTRL) {
				if (e.keyCode == '!') {
					update.run();
				}
			}

			// reset all results
			if (e.stateMask == SWT.CTRL + SWT.SHIFT) {
				if (e.keyCode == 'X' || e.keyCode == 'x') {
					reset.run();
				}
			}
		}
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {

		this.viewer = new Tree(parent, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		this.viewer.setHeaderVisible(true);
		this.viewer.addListener(SWT.Collapse, new CollapseListener());
		this.viewer.addListener(SWT.Expand, new ExpandListener());
		this.viewer.addListener(SWT.Selection, new CheckListener());
		this.viewer.addKeyListener(new PressListener());

		FileInfos.initColumns();
		makeColumns();
		makeActions();
		initView();
		contributeToActionBars();
	}

	/**
	 * Creates all table columns (one by prover)
	 */
	public void makeColumns() {

		// the prover number
		proversNumber = FileInfos.provers.length;

		if (proversNumber == 1 && FileInfos.provers[0].equals("")) {
			proversNumber = 0;
		}

		// Delete the prover's columns from the table
		int num_of_columns = viewer.getColumnCount();
		for (int w = 0; w < num_of_columns; w++) {
			TreeColumn tcl = viewer.getColumn(0);
			tcl.dispose();
		}

		// Creates new columns : 1 column for goals, 1 column
		// for goal states and one column by prover
		final TreeColumn col;
		col = new TreeColumn(viewer, SWT.RIGHT);
		col.setWidth(columnSize);
		col.addControlListener(new ControlListener() {
			public void controlMoved(ControlEvent e) {
				/* nothing to do */
			}

			public void controlResized(ControlEvent e) {
				columnSize = col.getWidth(); // get the column size
			}
		});

		TreeColumn square = new TreeColumn(viewer, SWT.CENTER);
		square.setWidth(IConstants.IMAGE_BALL_RED.getImageData().width + 6);
		square.setResizable(false); // can't resize this column
		square.addSelectionListener(new ProverSelectionListener());
	}
	
	public void updateKillAction () {

		if (threads.size() == 0) {
			kill.setEnabled(false);
			reset.setEnabled(true);
		} else {
			kill.setEnabled(true);
			reset.setEnabled(false);			
		}
	}
	
	private void updateActions (boolean haveGoals) {
		if (haveGoals) {
			runAllProvers.setEnabled(true);
			mark.setEnabled(true);
			runAssistant.setEnabled(true);
			save.setEnabled(true);			
		} else {
			runAllProvers.setEnabled(false);
			mark.setEnabled(false);
			runAssistant.setEnabled(false);
			save.setEnabled(false);
		}
		updateKillAction ();
	}

	/**
	 * Creates all goal and function items and all buttons (1 by provers for all
	 * goals and for all functions)
	 */
	private void initView() {

		int nb_lines_to_show;

		// gets if all goals will be displayed and adjust the slider
		IPreferenceStore store = EditeurWHY.getDefault().getPreferenceStore();
		showAllLines = store.getBoolean(IConstants.PREF_SHOW_ALL_LINES);
		if (!showAllLines) {
			nb_lines_to_show = store.getInt(IConstants.PREF_SHOW_NB_LINES);
		} else {
			nb_lines_to_show = FileInfos.functions.size() + FileInfos.numberOfGoals() + 1;
		}
		// gets all goals and functions which have to be created in the view
		// and save them in array lists goalsInView and functionsInView
		getGoals(nb_lines_to_show - 1);

		// gets here the number of goals to create
		int numberOfGoals = goalsInView.size();
		int numberOfFunctions = functionsInView.size();
		
		if (numberOfGoals > 0)
			updateActions(true);
		else
			updateActions(false);
		
		// initializes buttons boards
		TreeEditor[][] goalsEditor = new TreeEditor[numberOfGoals][proversNumber];
		TreeEditor[][] functionsEditor = new TreeEditor[numberOfFunctions][proversNumber];
		
		int m = 0; /* function number */
		int n = 0; /* goal number */

		BtnMenu menu = new BtnMenu(viewer);
		for (int f = 0; f < menu.getItems().length; f++) {
			menu.getItem(f).addSelectionListener(
					new ButtonMenuSelectionListener());
		}
		viewer.setMenu(menu);
		
		String currentFunction;
		int current, current_M = 0;
		TreeItem func = null;

		// for all goals
		for (int i = 0; i < numberOfGoals; i++) {

			int g = goalsInView.get(n) - 1;
			int l = 0;

			currentFunction = FileInfos.goals.get(g).getFname();
			current = FileInfos.goals.get(g).getFnum();

			// if we meet this function for the first time
			if ((func == null) || (current != current_M)) {

				// new Function item
				func = new TreeItem(viewer, SWT.NONE);
				l = FileInfos.goals.get(g).getFnum() - 1;
				func.setText(currentFunction);
				func.setData("function", Integer.valueOf(l));

				Function f = FileInfos.functions.get(l);

				if (f.isProved()) {
					func.setImage(1, IConstants.IMAGE_BALL_GREEN);
				} else {
					func.setImage(1, IConstants.IMAGE_BALL_WHITE);
				}

				m++;
				current_M = current; // current_M <- current
			}

			// new PO item
			TreeItem item = new TreeItem(func, SWT.NONE);
			PO po = FileInfos.goals.get(g);
			item.setForeground(IConstants.COLOR_GREY);
			item.setText(0, po.getTitle());
			item.setImage(1, IConstants.IMAGE_BALL_WHITE);

			for (int y = 0; y < proversNumber; y++) {
				int state = po.getState(y);
				if (state == 1) {
					item.setImage(1, IConstants.IMAGE_BALL_GREEN);
					break;
				}
			}

			item.setData("goal", Integer.valueOf(g + 1));
			n++;
		}

		// Expands items
		if (viewer.getItemCount() > 0) {
			for (int r = 0; r < numberOfFunctions; r++) {
				int a = functionsInView.get(r);
				Function fct = FileInfos.functions.get(a - 1);
				if (fct.isItem_expanded()) {
					TreeItem tit = viewer.getItem(r);
					tit.setExpanded(true);
					for (int y = 0; y < tit.getItemCount(); y++) {
						int z = (Integer)tit.getItem(y).getData("goal");
						PO e = FileInfos.goals.get(z - 1);
						if (e.isItem_expanded()) {
							tit.getItem(y).setExpanded(true);
						}
					}
				} else {
					viewer.getItem(r).setExpanded(false);
				}
			}
		}

		// layout() for button's view update
		viewer.getParent().layout();

		for (int x = 0; x < numberOfFunctions; x++) {
			for (int y = 0; y < proversNumber; y++) {
				functionsEditor[x][y].layout();
			}
		}

		for (int x = 0; x < numberOfGoals; x++) {
			for (int y = 0; y < proversNumber; y++) {
				goalsEditor[x][y].layout();
			}
		}
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	// Rolling menu in the high right corner of the view
	// with actions and corresponding icons
	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(showOnlyUnprovedGoals);
		manager.add(showAllGoals);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(foldTree);
		manager.add(unfoldTree);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(runAllProvers);
		manager.add(kill);
		manager.add(mark);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(reset);
		manager.add(update);
	}


	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(runAllProvers);
		manager.add(kill);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(mark);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(runAssistant);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(load);
		manager.add(save);
	}

	/**
	 * Creates all actions and defines all corresponding run() methods.
	 */
	private void makeActions() {

		// updates the view
		update = new Action() {
			public void run() {
				FileInfos.initColumns();
				makeColumns();
				updateView();
			}
		};
		update.setText("Update");
		update.setAccelerator(SWT.CTRL + '!');

		// clears results
		reset = new Action() {
			public void run() {
				if (isEnabled()) {
					for (int y = 0; y < FileInfos.functions.size(); y++) {
						FileInfos.functions.get(y).init();
					}
					for (int z = 0; z < FileInfos.goals.size(); z++) {
						FileInfos.goals.get(z).init();
					}
					FileInfos.showOnlyUnprovedGoals = false;
					showAllGoals.setChecked(true);
					showOnlyUnprovedGoals.setChecked(false);
					updateView();
				}
			}
		};
		reset.setText("Reset");
		reset.setAccelerator(SWT.CTRL + SWT.SHIFT + 'X');

		// marks the selected goal
		mark = new Action() {
			public void run() {
				mark();
			}
		};

		mark.setToolTipText("Create/Delete a start mark");
		mark.setText("Mark/Unmark PO");
		mark.setImageDescriptor(ImageDescriptor
				.createFromURL(IConstants.URL_MARK_BTN));
		mark.setAccelerator(SWT.F9);

		// runs all provers on all unproved goals
		runAllProvers = new Action() {
			public void run() {
				if (FileInfos.markedGoal > 0) {
					proveAll(false, false);
				} else if (FileInfos.markedGoal == 0) {
					proveAll(true, false);
				}
			}
		};
		runAllProvers
				.setToolTipText("Prove using all provers without reproving");
		runAllProvers.setText("Prove using all provers without reproving");
		runAllProvers.setImageDescriptor(ImageDescriptor
				.createFromURL(IConstants.URL_RUN_ALL_PROVERS));
		runAllProvers.setAccelerator(SWT.F7);

		// stops the prover actions
		kill = new Action() {
			public void run() {
				for (int e = 0; e < threads.size(); e++) {
					((ProverThread) threads.get(e)).cease();
				}
				threads.clear();
				setEnabled(false);
			}
		};
		kill.setText("Stop provers");
		kill.setToolTipText("Stop provers");
		kill.setImageDescriptor(ImageDescriptor
				.createFromURL(IConstants.URL_KILL_BUTTON));
		kill.setEnabled(false);
		kill.setAccelerator(SWT.F8);

		// hide proved goals
		showOnlyUnprovedGoals = new Action("Show Unproved", IAction.AS_RADIO_BUTTON) {
			public void run() {
				if (!FileInfos.showOnlyUnprovedGoals) {
					FileInfos.showOnlyUnprovedGoals = true;
					updateView();
				}
			}
		};
		showOnlyUnprovedGoals.setToolTipText("Show only unproved goals");
		
		// show proved goals
		showAllGoals = new Action("Show All", IAction.AS_RADIO_BUTTON) {
			
			public void run() {
				if (FileInfos.showOnlyUnprovedGoals) {
					FileInfos.showOnlyUnprovedGoals = false;
					updateView();
				}
			}
		};
		showAllGoals.setChecked(true);
		showAllGoals.setToolTipText("Show proved and unproved goals");

		// fold the tree viewer
		foldTree = new Action() {
			public void run() {
				TreeItem[] y = viewer.getItems();
				for (int p = 0; p < y.length; p++) {
					y[p].setExpanded(false);
				}
				for (int p = 0; p < FileInfos.functions.size(); p++) {
					FileInfos.functions.get(p).collapse();
				}
				updateView();
			}
		};
		foldTree.setImageDescriptor(ImageDescriptor
				.createFromURL(IConstants.URL_COLLAPSE_BTN));
		foldTree.setText("Collapse");
		foldTree.setToolTipText("Collapse");

		// unfold the tree viewer
		unfoldTree = new Action() {
			public void run() {
				TreeItem[] y = viewer.getItems();
				for (int p = 0; p < y.length; p++) {
					y[p].setExpanded(true);
				}
				for (int p = 0; p < FileInfos.functions.size(); p++) {
					FileInfos.functions.get(p).expand();
				}

				updateView();
			}
		};
		unfoldTree.setImageDescriptor(ImageDescriptor
				.createFromURL(IConstants.URL_EXPAND_BTN));
		unfoldTree.setText("Expand");
		unfoldTree.setToolTipText("Expand");

		// when a goal is selected
		selectAction = new Action() {
			public void run() {
				searchAndSelect();
			}
		};

		runAssistant = new Action() {
			public void run() {
					proveManully();
					updateView();
			}
		};
		runAssistant.setImageDescriptor(ImageDescriptor
				.createFromURL(IConstants.URL_ASSISTANT_BTN));
		runAssistant.setToolTipText("Prove manually");

		// saves the results into a XML file
		save = new Action() {
			public void run() {
				XMLSaver.save();
			}
		};
		save.setImageDescriptor(ImageDescriptor
				.createFromURL(IConstants.URL_SAVE_BTN));
		save.setToolTipText("Save results");

		// loads results from a XML file
		load = new Action() {
			public void run() {
				load();
			}
		};
		load.setImageDescriptor(ImageDescriptor
				.createFromURL(IConstants.URL_LOAD_BTN));
		load.setToolTipText("Load archived results");

	}

	/**
	 * Initialize the contents of the view
	 */
	public void updateView() {

		// remove all items
		viewer.removeAll();

		// reinitialize the view => build new buttons and items
		initView();
	}

	/**
	 * Updates a goal state : changes the image or the color of the
	 * corresponding button in the view in accordance with its state.
	 * 
	 * @param goalNum
	 *            the goal number
	 * @param proverNum
	 *            the prover number
	 * @throws SWTException
	 */
	public synchronized void updateElementAt(int goalNum) throws SWTException {
		updateState (goalNum);

		
		int state = FileInfos.goals.get(goalNum - 1).getState(0);

		switch (state) {
		case 0:
			updateFunctionState(goalNum, false);
			break;
		case 1:
			updateFunctionState(goalNum, true);
			break;
		case 2:
			updateFunctionState(goalNum, false);
			break;
		case 3:
			updateFunctionState(goalNum, false);
			break;
		case 4:
			updateFunctionState(goalNum, false);
			break;
		case 5:
			updateFunctionState(goalNum, false);
			break;
		default:
			break;
		}
	}
	// we get the goal item
	private TreeItem getGoalItem (int goal) {
		TreeItem gitem = null;;
		for (TreeItem item : viewer.getItems()) {
				for (TreeItem subitem: item.getItems()) {
					if ((Integer)subitem.getData("goal") == goal) {
						gitem = subitem;
						break;
					}
			}
		}
		return gitem;
	}
	/**
	 * Modifies the goals items in the view (proved/unproved) and
	 * updates the colors and images of buttons.
	 * 
	 * @param goalNumber
	 *            the goal number
	 * @param proverNumber
	 *            the prover number
	 * @throws SWTException
	 */
	private void updateState(int goalNumber)
			throws SWTException {

		TreeItem gitem = getGoalItem(goalNumber);

		// is the goal proved?
		boolean is_proved = FileInfos.goals.get(goalNumber - 1).isProved();

		if (is_proved) { // if the goal has been proved
			if (gitem != null) {
				gitem.setImage(1, IConstants.IMAGE_BALL_GREEN);
			}
		}

		if (!is_proved) { // if the goal hasn't been proved
				if (gitem != null) {
					if (!is_proved) {
						int errno = FileInfos.goals.get(goalNumber - 1).getState(0);

						Image image;
						switch(errno) {
							case 2:
								image = IConstants.IMAGE_INVALID;
								break;
							case 3:
								image = IConstants.IMAGE_UNKNOWN;
								break;
							case 4:
								image = IConstants.IMAGE_TIME_OUT;
								break;
							case 5:
								image = IConstants.IMAGE_FAILURE;
								break;
							default:
								image = IConstants.IMAGE_BALL_RED;
								break;
						}
						gitem.setImage(1, image);
					}
			}
		}
	}

	/**
	 * Modifies the functions items in the proving view (proved/unproved) and
	 * updates the colors and images of buttons.
	 * 
	 * @param goalNumber
	 *            The goal number
	 * @param proverNumber
	 *            The prover number
	 * @param proved
	 *            true if the goal has been proved by the prover, false
	 *            otherwise.
	 * @throws SWTException
	 */
	private void updateFunctionState (int goalNumber, boolean proved)
			throws SWTException {

		int g = getFunctionRow(goalNumber);

		if (g < 0)
			return;

		PO po = FileInfos.goals.get(goalNumber - 1);
		int fnum = po.getFnum();
		Function fc = FileInfos.functions.get(fnum - 1);

		TreeItem fitem = viewer.getItem(g);
		if (fitem != null) {
			if (fc.isProved()) { // if the function is proved
				fitem.setImage(1, IConstants.IMAGE_BALL_GREEN);
			} else { // else
				fitem.setImage(1, IConstants.IMAGE_BALL_RED);
			}
		}
	}

	/**
	 * Puts purple ball in the goal and function
	 * buttons to show that the prover is working on.
	 * 
	 * @param goalNumber
	 *            the goal number
	 */
	public synchronized void working (int goalNumber) {
		int frow = getFunctionRow(goalNumber);
		TreeItem funcItem = viewer.getItem(frow);
		TreeItem goalItem = getGoalItem(goalNumber);
		funcItem.setImage(1, IConstants.IMAGE_BALL_PURP);
		if (goalItem != null)
			goalItem.setImage(1, IConstants.IMAGE_BALL_PURP);
	}

	/**
	 * Gets the row of a goal in the goals' board
	 * 
	 * @param goalNum
	 *            goal number
	 * @return int index of the goal, -1 if it isn't in
	 */
	private int getGoalRow(int goalNum) {

		int line = -1;
		for (int p = 0; p < goalsInView.size(); p++) {
			if (goalsInView.get(p) == goalNum) {
				line = p;
				break;
			}
		}
		return line;
	}

	/**
	 * Gets the row of a function in the functions' board, giving a goal number
	 * which belongs to the function.
	 * 
	 * @param goalNum
	 *            a goal number
	 * @return index of the function in the view, -1 if it's not found
	 */
	private int getFunctionRow(int goalNum) {

		int line = -1; // function's row in the ArrayList functionsInView
		int frow = -1; // the function number (cf. FileInfos.functions[])

		PO po = FileInfos.goals.get(goalNum - 1);
		frow = po.getFnum();

		if (frow == 0) {
			TraceView.print(MessageType.WARNING,
					"Function '" + po.getFname() + "' unknown");
			return -1;
		}

		for (int p = 0; p < functionsInView.size(); p++) {
			if (functionsInView.get(p)== frow) {
				line = p;
				break;
			}
		}
		return line;
	}

	/**
	 * Removes a thread from the list of working jobs. This function is called
	 * when a thread concerning the execution of verification tools has
	 * finished.
	 * 
	 * @param id
	 *            the thread id
	 */
	public synchronized void removeThread(long id) {
		for (int r = 0; r < threads.size(); r++) {
			if (((ProverThread) threads.get(r)).getIdentity() == id) {
				threads.remove(r);
			}
		}
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.setFocus();
	}

	/**
	 * Function called when the view is opened
	 */
	public void setInput() {
		FileInfos.initColumns();
	}

	/**
	 * This function gets the selected file in the view and runs the
	 * <code>Highlightor.highlight()</code> method to highlight the source code
	 * which corresponds to the goal.
	 */
	private void searchAndSelect() {

		int goalNum = 0;
		int funcNum = 0;
		// String kind = "";
		String message;
		Image image;
		TreeItem m = null;

		try {
			m = viewer.getSelection()[0]; // gets the selected item
			Object data = m.getData("goal");
			if (data == null) {
				getViewSite().getActionBars().getStatusLineManager()
						.setMessage(null);
				return;
			}
			goalNum = (Integer)data; // goal number
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (goalNum > 0) {
			String file = FileInfos.goals.get(goalNum - 1).getWhyFile();
			String xplFile = FileInfos.goals.get(goalNum - 1).getXplFile();

			Highlightor.setGoal(goalNum);
			Highlightor.selectFromXPL(xplFile);
			showGoalInViewer(goalNum, file); // sets the pretty printed goal in
											 // the PO
			// Viewer
			message = FileInfos.goals.get(goalNum - 1).getTitle();
			image = IConstants.IMAGE_PO;
		} else {
			// this is a function
			showGoalInViewer(-1, null); // clean the PO Viewer
			funcNum = (Integer)m.getData("function");
			message = FileInfos.functions.get(funcNum).getBehavior();
			image = IConstants.IMAGE_FUNC;
		}

		if (message != null && !message.trim().equals("")) {
			// sets the item information in the low bar
			getViewSite().getActionBars().getStatusLineManager().setMessage(
					image, message);
		} else {
			getViewSite().getActionBars().getStatusLineManager().setMessage(
					null);
		}
		viewer.forceFocus();
	}

	/**
	 * Load a set of proving results associated with a program from a XML file
	 * to this view.
	 */
	private void load() {
		XMLLoader xload = new XMLLoader(this);
		xload.load();
	}

	/**
	 * Creates a thread which will run a prover on a set of goals
	 * 
	 * @param a
	 *            set of goals to prove
	 * @param proverNumber
	 *            the prover number, -1 to use all provers
	 * @param all
	 *            false to prove unproved goals only, true to prove all goals
	 * 
	 */
	private void prove(ArrayList<Integer> goals, boolean all) {
		ProverViewUpdater uvw = new ProverViewUpdater((ProverView) this);
		ProverThread m = new ProverThread(goals, all, uvw);
		threads.add(m);
	}

	/**
	 * Runs the prove() function on all goals
	 * 
	 * @param prover
	 *            the prover number, -1 to use all provers
	 * @param begin_first
	 *            true to begin with the first goal, false to begin with the
	 *            marked goal
	 * @param notall
	 *            true to prove unproved goals only, false to prove all goals
	 */
	private void proveAll (boolean begin_first, boolean all) {

		int first = 1;
		int last = FileInfos.numberOfGoals();
		if (last == 0) { // empty set
			return;
		}

		if (!begin_first) { // we begin with a marked goal
			first = FileInfos.markedGoal;
			if (first == 0) {
				return;
			}
		}

		ArrayList<Integer> goals = new ArrayList<Integer>();

		for (int i = first; i <= last; i++) {
				if (!FileInfos.goals.get(i - 1).isProved() || all) {
					goals.add(i);
				}
		}
			
		prove(goals, all); // prove all the goals we've selected
	}

	/**
	 * Puts a mark on the goal selected in the Prover View
	 */
	private void mark() {

		TreeItem[] items = viewer.getSelection();
		TreeItem fitem = null;
		TreeItem gitem = null;

		if (items != null && items.length == 1) {
			// we get, from the selected item, the function or goal number
			Integer goal;
			Integer function = null;
			boolean is_function_item = false;
			goal = (Integer)items[0].getData("goal");
			if (goal == null) {
				is_function_item = true;
				function = (Integer)items[0].getData("function");
				if (function == null) {
					TraceView
							.print(MessageType.ERROR,
									"ProverView.mark() : selected item represents neither a goal nor a function !");
					return;
				}
			}

			// if it's a function item, we get the first goal of
			// this function which appears in the view
			if (is_function_item) {
				goal = (Integer)items[0].getItem(0).getData("goal");
				fitem = items[0];
				gitem = (items[0].getItems())[0];
			} else {
				fitem = items[0].getParentItem();
				gitem = items[0];
			}

			// if the function was ever marked, we can consider that the
			// selected
			// goal is the marked goal => we remove the marks of the goal and of
			// the function
			if (is_function_item && (FileInfos.markedGoal > 0)) {
				int fmarked = FileInfos.goals.get(FileInfos.markedGoal - 1).getFnum();
				if (fmarked == function) {
					goal = FileInfos.markedGoal;
					for (int s = 0; s < fitem.getItems().length; s++) {
						int t = (Integer)fitem.getItem(s).getData("goal");
						if (t == goal) {
							// with the corresponding item
							gitem = fitem.getItems()[s];
							break;
						}
					}
				}
			}

			// we begin with the goal we recovered
			if (goal != null) {
				// if the goal was ever marked
				if (FileInfos.markedGoal == goal) {
					// we unmark it
					fitem.setForeground(0, IConstants.COLOR_BLACK);
					gitem.setForeground(0, IConstants.COLOR_GREY);
					FileInfos.markedGoal = 0;
				}
				// else
				else {
					// we mark the goal
					gitem.setForeground(0, IConstants.COLOR_GREEN);
					// if a goal was marked before
					if (FileInfos.markedGoal > 0) {
						int g = FileInfos.markedGoal;
						int i = getGoalRow(g);
						// and if the item of this goal was in the view
						if (i != -1) {
							int j = getFunctionRow(g);
							if (j != -1) {
								TreeItem t = viewer.getItem(j);
								TreeItem[] tprim = t.getItems();
								for (int v = 0; v < tprim.length; v++) {
									TreeItem t2 = tprim[v];
									int gprim = (Integer)t2.getData("goal");
									if (gprim == g) {
										// we remove the mark
										t2.setForeground(0,
												IConstants.COLOR_GREY);
										t2.getParentItem().setForeground(0,
												IConstants.COLOR_BLACK);
									}
								}
							}
						}
					}
					FileInfos.markedGoal = goal;
					fitem.setForeground(0, IConstants.COLOR_GREEN);
				}
			}
		}
	}

	/**
	 * Splits the selected po into sub-pos creating files and items in the view
	 * 
	 * @return the number of sub-po which have just been created, -1 if
	 *         impossible to create them
	 */
	private int proveManully() {
		TreeItem[] items = viewer.getSelection();
		if (items != null && items.length == 1) {
			int num = (Integer)items[0].getData("goal");
			ProverExecutor ex = new ProverExecutor();
			ex.prove(num, 1);
		}
		return -1;
	}

	/**
	 * Prints in PO Viewer a pretty printed goal
	 * 
	 * @param gnum
	 *            the goal number to make the view title
	 * @param whyFileName
	 *            the .why file name to print in the view
	 */
	private void showGoalInViewer(int gnum, String whyFileName) {

		try {
			String file = null;
			if (whyFileName != null) {
				file = FileInfos.getRoot() + "why" + File.separator
						+ whyFileName;
			}

			IWorkbenchPage page = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage();
			POViewer v = (POViewer) page.showView(IConstants.PO_VIEW_ID);
			v.setTText(gnum);

			GoalDisplayModifier display = new GoalDisplayModifier();
			display.whyToView(file);
			v.inputPO();

		} catch (PartInitException e) {
			TraceView.print(MessageType.ERROR, "ProverView.afficheGV() : " + e);
		} catch (IOException e) {
			TraceView.print(MessageType.ERROR, "ProverView.afficheGV() : " + e);
		}
	}

	/**
	 * Fills the ArrayList <code>goalsInView</code> and
	 * <code>functionsInView</code>. These object must contain respectively the
	 * numbers of all goals shown in the view and all functions items appearing
	 * in the tree viewer.
	 * 
	 * @param startGoal
	 *            the first goal (hidden or not) which belongs to the view
	 * @param lastLine
	 *            the number of lines to show in the view
	 */
	private void getGoals(int lastLine) {
		int i = 0;
		int g = 1;
		int max = FileInfos.numberOfGoals();
		int numberOfFunctions;

		goalsInView.clear();
		functionsInView.clear();

		if (FileInfos.goals.size() == 0) {
			return;
		}

		numberOfFunctions = FileInfos.goals.get(g - 1).getFnum();

		String currentFunction = "";

		// while we haven't exceed the number of lines to show
		// and while there are again goals
		while ((i <= lastLine) && (g <= max)) {

			PO po = FileInfos.goals.get(g - 1);

			// if we show all goals or if we show only unproved
			// goals and this goal's unproved
			if (!FileInfos.showOnlyUnprovedGoals
					|| (FileInfos.showOnlyUnprovedGoals && !po.isProved())) {
				String functionName = po.getFname();
				if (!functionName.equals(currentFunction)) { // unknown function : creates a new one
					currentFunction = functionName;
					numberOfFunctions = po.getFnum();
					i++;
					if (i <= lastLine + 1)
						functionsInView.add(Integer.valueOf(numberOfFunctions));
				}
				if (i <= lastLine + 1) {
					goalsInView.add(Integer.valueOf(g));
					i++;
					g++;
				}
			} else {
				// Search for the next unproved goal
				while ((g <= max) && po.isProved()) {
					g++;
					if (g > max)
						break;
					po = FileInfos.goals.get(g - 1);
				}
			}
		}
	}

	/**
	 * Makes prover's statistics
	 * 
	 * @param proverNum
	 *            the prover number
	 */
	public synchronized void makeStats(int proverNum) {

		int goalNum = FileInfos.numberOfGoals();

		int state;
		int proved = 0;
		int invalid = 0;
		int unknown = 0;
		int timeout = 0;
		int failure = 0;

		// gets the number of proved, invalid, unknown, timeout and
		// failure results for all goals for this prover
		for (int g = 1; g <= goalNum; g++) {
			state = FileInfos.goals.get(g - 1).getState(proverNum);
			switch (state) {
			case 1:
				proved++;
				break;
			case 2:
				invalid++;
				break;
			case 3:
				unknown++;
				break;
			case 4:
				timeout++;
				break;
			case 5:
				failure++;
				break;
			default:
				break;
			}
		}

		// makes the statistics string
		String stats = "PROVED : " + proved + "/" + goalNum + "\n";
		stats += "INVALID : " + invalid + "/" + goalNum + "\n";
		stats += "UNKNOWN : " + unknown + "/" + goalNum + "\n";
		stats += "TIMEOUT : " + timeout + "/" + goalNum + "\n";
		stats += "FAILURE : " + failure + "/" + goalNum;

		// puts this results into a FileInfos field and
		// into the column tooltip text
		FileInfos.proverStats[proverNum] = stats;
		viewer.getColumn(1).setToolTipText(stats);
	}

}

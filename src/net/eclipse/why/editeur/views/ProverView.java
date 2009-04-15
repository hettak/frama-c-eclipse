package net.eclipse.why.editeur.views;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import net.eclipse.why.editeur.EditeurWHY;
import net.eclipse.why.editeur.FileInfos;
import net.eclipse.why.editeur.Function;
import net.eclipse.why.editeur.IConstants;
import net.eclipse.why.editeur.PO;
import net.eclipse.why.editeur.actions.Highlightor;
import net.eclipse.why.editeur.actions.ProverThread;
import net.eclipse.why.editeur.actions.ProverViewUpdator;
import net.eclipse.why.editeur.actions.Splitter;
import net.eclipse.why.editeur.actions.TraceDisplay;
import net.eclipse.why.editeur.actions.XMLLoader;
import net.eclipse.why.editeur.actions.XMLSaver;
import net.eclipse.why.editeur.actions.TraceDisplay.MessageType;
import net.eclipse.why.editeur.lexer.GoalDisplayModifier;
import net.eclipse.why.editeur.lexer.ast.Pointer;
import net.eclipse.why.editeur.menu.BtnMenu;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
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
	private Button[][] goalsButton;
	private Button[][] functionsButton;
	private Button[][] subGoalsButton;

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
	private Action split;
	private Action save;
	private Action load;

	// Colors
	private Color goalBtColor;
	private Color subGoalBtColor;
	private Color funcBtColor;
	private Color assistantGoalBgColor, assistantSubGoalBgColor,
			assistantFuncBgColor;
	private Color markedGoalColor, markedFuncColor;

	// Others
	private int proversNumber;
	private boolean showAllLines;
	public int CURSOR = 0;
	private Vector<ProverThread> threads = new Vector<ProverThread>();
	private ArrayList<int[]> goalsInView = new ArrayList<int[]>();
	private ArrayList<int[]> functionsInView = new ArrayList<int[]>();
	private int columnSize = 150;

	/**
	 * Tree getter
	 */
	public Tree getViewer() {
		return viewer;
	}

	/**
	 * Prover SelectionListener : defines the action which is executed when the
	 * user clicks on a column button. The corresponding prover is executed on all
	 * goals. It gets the column and the prover number and if goal is marked
	 * it prove all goals beginning with this marked goal. If no goal is marked,
	 * all goals from the first one will be proved.
	 */
	private class ProverSelectionListener implements SelectionListener {

		public void widgetSelected(SelectionEvent e) {
			TreeColumn t = (TreeColumn) e.getSource();
			int pNum = ((int[]) t.getData())[0];
			if (FileInfos.markedGoal > 0) { 
				proveAll(pNum, false, FileInfos.showOnlyUnprovedGoals);
			} else if (FileInfos.markedGoal == 0) { // if no goal is marked
				proveAll(pNum, true, FileInfos.showOnlyUnprovedGoals);
			}
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			return;
		}
	}

	/**
	 * The mouse listener. Defines the action which is executed when the user
	 * clicks on a goal button.
	 */
	private class BtnListener implements MouseListener {

		public void mouseDoubleClick(MouseEvent e) {
			return;
		}

		public void mouseDown(MouseEvent e) {

			if (e.button == 1) { // left click
				Button b = (Button) e.widget; // the clicked button
				int line = ((int[]) b.getData("goal"))[0];
				int subline = ((int[]) b.getData("goal"))[1];
				int column = ((Integer) b.getData("prover")).intValue();

				// Creates the goals set to prove : here it's just the
				// goal corresponding to the clicked button.
				// So, the first and the last goals to prove are the same one:
				ArrayList<String[]> array = new ArrayList<String[]>();
				String[] goal = new String[2];
				if (subline <= 0) {
					goal[0] = "" + line;
					goal[1] = "" + line;
					array.add(goal);
				} else {
					goal[0] = line + "-" + subline;
					goal[1] = line + "-" + subline;
					array.add(goal);
				}
				prove(array, column, !FileInfos.showOnlyUnprovedGoals);
			}

			if (e.button == 3) { // right click
				// expand the menu which propose to modify manually
				// the state of the goal
				Button b = (Button) e.widget;
				b.getMenu().setData(b);
				b.getMenu().setVisible(true);
			}
		}

		public void mouseUp(MouseEvent e) {
			/* do nothing */
		}
	}

	/**
	 * Goal button menu SelectionListener. It gets the clicked value : 0=reset,
	 * 1=admitted(proved), 2=unproved. Then it gets the menu's button and the
	 * corresponding goal, subgoal and prover. Then PO is extracted and changed.
	 */
	private class ButtonMenuSelectionListener implements SelectionListener {

		public void widgetSelected(SelectionEvent e) {

			try {
				
				MenuItem o = (MenuItem) e.widget;
				int a = ((Integer) o.getData("admit")).intValue();
				Button p = (Button) o.getParent().getData();
				int goal = ((int[]) p.getData("goal"))[0];
				int subgoal = ((int[]) p.getData("goal"))[1];
				int prover = ((Integer) p.getData("prover")).intValue();

				PO op = (PO) FileInfos.goals.get(goal - 1);
				if (subgoal > 0)
					op = op.getSubGoal(subgoal);
				op.setState(prover, a);

				updateElementAt(goal, subgoal, prover);
				makeStats(prover);

			} catch (Exception exception) {
				TraceDisplay.print(MessageType.ERROR,
						"ProverView.MenuItemSelectionListener.widgetSelected() :\n"
								+ exception);
			}
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			
		}
	}

	/**
	 * Function button's SelectionListener : defines the action which is
	 * executed when the user clicks on a function's button. The prover of the
	 * corresponding column is executed on all goals included into the function.
	 */
	private class FunctionSelectionListener implements SelectionListener {

		public void widgetSelected(SelectionEvent e) {

			// get the button and, the prover number and the function name
			Button b = (Button) e.widget;
			int prover = ((Integer) b.getData("prover")).intValue();
			String function = (String) b.getData("function");

			int fgoal = 1; // the first goal to prove
			int lgoal = 1; // the last goal to prove

			// for all functions :
			for (int c = 0; c < FileInfos.functions.size(); c++) {
				Function f = (Function) FileInfos.functions.get(c);
				// if we have found our function
				if (f.getName().equals(function)) {
					// the first goal is ok
					// we've just to increment the last goal number
					lgoal = fgoal;
					lgoal += f.getPo();
					lgoal--;
					break;
				} else { // if it's a previous function
					fgoal += f.getPo(); // we increment the first goal number to
										// prove
				}
			}

			// we've to make now the set of goals to prove
			ArrayList<String[]> array = new ArrayList<String[]>();
			String[] goalsSet = new String[2];

			// if we must prove all goals, it's simple
			if (!FileInfos.showOnlyUnprovedGoals) {
				goalsSet = new String[2];
				goalsSet[0] = "" + fgoal;
				goalsSet[1] = "" + lgoal;
				array.add(goalsSet);
			} else { // but if we must prove only unproved goals
				// we have to make sets(=intervals) of goals to prove
				boolean inASet = false;
				int nSet = 0; // nb of goals in a set
				int w;
				// for all goals
				for (w = (fgoal - 1); w < lgoal; w++) {
					inASet = false;
					// if the goals is unproved
					if (!((PO) FileInfos.goals.get(w)).isProved()) {
						// new set()
						inASet = true;
						nSet++;
					}
					// if it's the beginning of a set
					if (inASet && (nSet == 1)) {
						// record the first goal
						goalsSet = new String[2];
						goalsSet[0] = "" + (w + 1);
					}
					// if we go out of a set and if this set is not empty
					if (!inASet && (nSet > 0)) {
						// record the last goal and save the set
						nSet = 0;
						goalsSet[1] = "" + w;
						array.add(goalsSet);
					}
				}
				// if we were in an unclosed and unsaved set before stopping
				if (inASet) {
					// record the last goal and save the set
					goalsSet[1] = "" + w;
					array.add(goalsSet);
				}
			}

			// prove now the set of sets of goals
			prove(array, prover, !FileInfos.showOnlyUnprovedGoals);

		}

		public void widgetDefaultSelected(SelectionEvent e) {
			// do nothing
		}
	}

	/**
	 * Item listener for 'Collapse' action
	 */
	private class CollapseListener implements Listener {

		public void handleEvent(Event event) {
			TreeItem t = (TreeItem) event.item;
			if (t.getData("function") != null) { // function tree item
				int a = ((int[]) t.getData("function"))[0];
				// collapse function item
				((Function) FileInfos.functions.get(a)).collapse();
				TreeItem[] tprim = t.getItems();
				for (int y = 0; y < tprim.length; y++) {
					// collapse all goal items
					int aprim = ((int[]) tprim[y].getData("goal"))[0];
					((PO) FileInfos.goals.get(aprim - 1)).collapse();
				}
				// a possible update of the slider if we don't
				// have to show all goals
				if (!showAllLines)
					updateSlider(true);
			} else if (t.getData("goal") != null) { // goal tree item
				int a = ((int[]) t.getData("goal"))[0];
				((PO) FileInfos.goals.get(a - 1)).collapse();
			}
		}
	}

	/**
	 * Item listener for 'Expand' action
	 */
	private class ExpandListener implements Listener {

		public void handleEvent(Event event) {
			TreeItem t = (TreeItem) event.item;
			if (t.getData("function") != null) { // function tree item
				int a = ((int[]) t.getData("function"))[0];
				// expand function's item
				((Function) FileInfos.functions.get(a)).expand();
				// a possible update of the slider if we don't
				// have to show all goals
				if (!showAllLines)
					updateSlider(true);
			} else if (t.getData("goal") != null) { // goal tree item
				int a = ((int[]) t.getData("goal"))[0];
				((PO) FileInfos.goals.get(a - 1)).expand();
			}
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
		initView();
		makeActions();
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

		TreeColumn column;
		// for all provers
		for (int i = 0; i < proversNumber; i++) {
			int[] pNum = new int[1];
			pNum[0] = i;
			column = new TreeColumn(viewer, SWT.CENTER);
			column.setText(FileInfos.provers[i]);
			// sets statistics in the tooltip text
			column.setToolTipText(FileInfos.proverStats[i]);
			// sets the prover number into widget's data
			column.setData(pNum);
			if (FileInfos.status[i].equals("prover"))
				column.addSelectionListener(new ProverSelectionListener());
			column.pack();
		}
	}

	/**
	 * Creates all goal and function items and all buttons (1 by provers for all
	 * goals and for all functions)
	 */
	private void initView() {

		int nb_lines_to_show;

		// displays a warning message if the number of lines
		// to show in the view is higher than a maximum number
		// fixed to 150
		warn();

		// gets if all goals will be displayed and adjust the slider
		IPreferenceStore store = EditeurWHY.getDefault().getPreferenceStore();
		showAllLines = store.getBoolean(IConstants.PREF_SHOW_ALL_LINES);
		if (!showAllLines) {
			nb_lines_to_show = store.getInt(IConstants.PREF_SHOW_NB_LINES);
			updateSlider(false);
		} else {
			nb_lines_to_show = FileInfos.functions.size()
					+ FileInfos.whyFileNumber + 1;
			CURSOR = 0;
		}

		int goal_of_reference = getCursorGoal(); // first goal of the view

		// gets all goals and functions which have to be created in the view
		// and save them in array lists goalsInView and functionsInView
		getGoals(goal_of_reference, nb_lines_to_show - 1);

		// gets here the number of subgoals to create
		int NUMBER_OF_GOALS = goalsInView.size();
		int NUMBER_OF_FUNCTIONS = functionsInView.size();
		int NUMBER_OF_SUBGOALS = 0;
		for (int b = 0; b < goalsInView.size(); b++) {
			int a = ((int[]) goalsInView.get(b))[0] - 1;
			PO poa = (PO) FileInfos.goals.get(a);
			if (FileInfos.showOnlyUnprovedGoals)
				NUMBER_OF_SUBGOALS += poa.getNbUnprovedSubGoals();
			else
				NUMBER_OF_SUBGOALS += poa.getNbSubGoals();
		}

		// Gets out from preferences values the background colors for buttons
		RGB rgb = PreferenceConverter.getColor(store,
				IConstants.PREF_GOALS_BUTTON_BG_COLOR);
		goalBtColor = new Color(null, rgb.red, rgb.green, rgb.blue);
		rgb = PreferenceConverter.getColor(store,
				IConstants.PREF_SUBGOALS_BUTTON_BG_COLOR);
		subGoalBtColor = new Color(null, rgb.red, rgb.green, rgb.blue);
		rgb = PreferenceConverter.getColor(store,
				IConstants.PREF_FUNCS_BUTTON_BG_COLOR);
		funcBtColor = new Color(null, rgb.red, rgb.green, rgb.blue);
		rgb = PreferenceConverter.getColor(store,
				IConstants.PREF_GOALS_ASSISTANT_BUTTON_BG_COLOR);
		assistantGoalBgColor = new Color(null, rgb.red, rgb.green, rgb.blue);
		rgb = PreferenceConverter.getColor(store,
				IConstants.PREF_SUBGOALS_ASSISTANT_BUTTON_BG_COLOR);
		assistantSubGoalBgColor = new Color(null, rgb.red, rgb.green, rgb.blue);
		rgb = PreferenceConverter.getColor(store,
				IConstants.PREF_FUNCS_ASSISTANT_BUTTON_BG_COLOR);
		assistantFuncBgColor = new Color(null, rgb.red, rgb.green, rgb.blue);
		rgb = PreferenceConverter.getColor(store,
				IConstants.PREF_MARKED_GOAL_TEXT_COLOR);
		markedGoalColor = new Color(null, rgb.red, rgb.green, rgb.blue);
		rgb = PreferenceConverter.getColor(store,
				IConstants.PREF_MARKED_FUNC_TEXT_COLOR);
		markedFuncColor = new Color(null, rgb.red, rgb.green, rgb.blue);

		// initializes buttons boards
		goalsButton = new Button[NUMBER_OF_GOALS][proversNumber];
		TreeEditor[][] goalsEditor = new TreeEditor[NUMBER_OF_GOALS][proversNumber];
		functionsButton = new Button[NUMBER_OF_FUNCTIONS][proversNumber];
		TreeEditor[][] functionsEditor = new TreeEditor[NUMBER_OF_FUNCTIONS][proversNumber];
		subGoalsButton = new Button[NUMBER_OF_SUBGOALS][proversNumber];
		TreeEditor[][] subGoalsEditor = new TreeEditor[NUMBER_OF_SUBGOALS][proversNumber];

		int m = 0; /* function number */
		int n = 0; /* goal number */
		int p = 0; /* subgoal number */

		BtnMenu menu = new BtnMenu(viewer);
		for (int f = 0; f < menu.getItems().length; f++) {
			menu.getItem(f).addSelectionListener(
					new ButtonMenuSelectionListener());
		}

		String currentFunction;
		int current, current_M = 0;
		TreeItem func = null;

		// for all goals
		for (int i = 0; i < NUMBER_OF_GOALS; i++) {

			int g = ((int[]) goalsInView.get(n))[0] - 1;
			int l = 0;

			currentFunction = ((PO) FileInfos.goals.get(g)).getFname(); // name
			current = ((PO) FileInfos.goals.get(g)).getFnum(); // num

			// if we meet this function for the first time
			if ((func == null) || (current != current_M)) {

				// new Function item
				func = new TreeItem(viewer, SWT.NONE);
				l = ((PO) FileInfos.goals.get(g)).getFnum() - 1;
				func.setText(currentFunction);
				func.setData("function", new int[] { l });

				Function f = (Function) FileInfos.functions.get(l);
				// for all provers
				for (int j = 0; j < proversNumber; j++) {

					boolean b_assistant;
					// create new editors and new functions
					functionsEditor[m][j] = new TreeEditor(viewer);
					functionsButton[m][j] = new Button(viewer, SWT.PUSH);

					if (FileInfos.status[j].equals("prover")) {
						functionsButton[m][j].setBackground(funcBtColor);
						b_assistant = false;
					} else {
						functionsButton[m][j]
								.setBackground(assistantFuncBgColor);
						b_assistant = true;
					}

					int x = f.getPo(); // number of PO
					int y = f.getFirst_po() - 1; // number of the first PO
					boolean proved = true; // is the function proved?
					boolean zero = true; // is the function reset?

					// for all PO in the function
					for (int c = 0; c < x; c++) {
						PO potmp = (PO) FileInfos.goals.get(y + c);
						int a = potmp.getState(j);
						if (a != 0) { // proved or unproved, not null
							zero = false;
							if (!proved)
								break; // unproved
						}
						if (a != 1) { // not proved
							proved = false;
							if (!zero)
								break; // unproved
						}
					}

					if (proved)
						setButtonProved(functionsButton[m][j], false, false);
					else if (zero)
						setButtonStart(functionsButton[m][j], false, false,
								!b_assistant);
					else
						setButtonUnproved(functionsButton[m][j], false, false,
								0);

					functionsButton[m][j].computeSize(SWT.DEFAULT, viewer
							.getItemHeight());
					if (FileInfos.status[j].equals("prover"))
						functionsButton[m][j]
								.addSelectionListener(new FunctionSelectionListener());

					functionsEditor[m][j].grabHorizontal = true;
					functionsEditor[m][j].minimumHeight = functionsButton[m][j]
							.getSize().y;
					functionsEditor[m][j].minimumWidth = functionsButton[m][j]
							.getSize().x;
					functionsEditor[m][j].setEditor(functionsButton[m][j],
							func, j + 2);

					// sets function's name, prover number and item number in
					// button's data
					functionsButton[m][j].setData("function", currentFunction);
					functionsButton[m][j].setData("prover", new Integer(j));
					functionsButton[m][j].setData("item", new int[] { m });

					functionsButton[m][j].setRedraw(true);

				}

				if (f.isProved()) {
					func.setImage(1, IConstants.IMAGE_BALL_GREEN);
				} else {
					func.setImage(1, IConstants.IMAGE_BALL_RED);
				}

				m++;
				current_M = current; // current_M <- current
			}

			// new PO item
			TreeItem item = new TreeItem(func, SWT.NONE);
			PO po = (PO) FileInfos.goals.get(g);
			item.setForeground(IConstants.COLOR_GREY);
			item.setText(0, po.getName());
			item.setImage(1, IConstants.IMAGE_BALL_RED);

			for (int y = 0; y < proversNumber; y++) {
				int state = po.getState(y);
				if (state == 1) {
					item.setImage(1, IConstants.IMAGE_BALL_GREEN);
					break;
				}
			}

			item.setData("goal", new int[] { g + 1, 0 });
			if ((g + 1) == FileInfos.markedGoal) {
				item.setForeground(0, markedGoalColor);
				item.getParentItem().setForeground(0, markedFuncColor);
			}

			for (int j = 0; j < proversNumber; j++) {

				boolean b_prover;

				goalsEditor[n][j] = new TreeEditor(viewer);
				goalsButton[n][j] = new Button(viewer, SWT.PUSH);

				if (FileInfos.status[j].equals("prover")) {
					goalsButton[n][j].setBackground(goalBtColor);
					b_prover = true;
				} else {
					goalsButton[n][j].setBackground(assistantGoalBgColor);
					b_prover = false;
				}

				int state = po.getState(j);
				switch (state) {
				case 0:
					setButtonStart(goalsButton[n][j], true, false, b_prover);
					break;
				case 1:
					setButtonProved(goalsButton[n][j], true, false);
					break;
				case 2:
					setButtonUnproved(goalsButton[n][j], true, false, state);
					goalsButton[n][j].setToolTipText("invalid");
					break;
				case 3:
					setButtonUnproved(goalsButton[n][j], true, false, state);
					goalsButton[n][j].setToolTipText("unknown");
					break;
				case 4:
					setButtonUnproved(goalsButton[n][j], true, false, state);
					goalsButton[n][j].setToolTipText("timeout");
					break;
				case 5:
					setButtonUnproved(goalsButton[n][j], true, false, state);
					goalsButton[n][j].setToolTipText("failure");
					break;
				default:
					break;
				}

				goalsButton[n][j].computeSize(SWT.DEFAULT, viewer
						.getItemHeight());
				goalsButton[n][j].addMouseListener(new BtnListener());
				goalsButton[n][j].setMenu(menu);

				goalsEditor[n][j].grabHorizontal = true;
				goalsEditor[n][j].minimumHeight = goalsButton[n][j].getSize().y;
				goalsEditor[n][j].minimumWidth = goalsButton[n][j].getSize().x;
				goalsEditor[n][j].setEditor(goalsButton[n][j], item, j + 2);

				// sets goal/subgoal numbers and prover number in button's datas
				goalsButton[n][j].setData("goal", new int[] { g + 1, 0 });
				goalsButton[n][j].setData("prover", new Integer(j));
				goalsButton[n][j].setRedraw(true);
			}

			int nbSub = po.getNbSubGoals();
			for (int yy = 0; yy < nbSub; yy++) {

				PO subop = po.getSubGoal(yy + 1);

				if ((FileInfos.showOnlyUnprovedGoals && !subop.isProved())
						|| !FileInfos.showOnlyUnprovedGoals) {

					TreeItem subitem = new TreeItem(item, SWT.NONE);
					subitem.setForeground(IConstants.COLOR_GREY);
					subitem.setText(0, subop.getName());
					subitem.setImage(1, IConstants.IMAGE_BALL_RED);

					for (int y = 0; y < proversNumber; y++) {
						if (subop.getState(y) == 1) {
							subitem.setImage(1, IConstants.IMAGE_BALL_GREEN);
							break;
						}
					}

					subitem.setData("goal", new int[] { g + 1, yy + 1 });

					for (int j = 0; j < proversNumber; j++) {

						subGoalsEditor[p][j] = new TreeEditor(viewer);
						subGoalsButton[p][j] = new Button(viewer, SWT.PUSH);

						boolean b_prover;
						if (FileInfos.status[j].equals("prover")) {
							subGoalsButton[p][j].setBackground(subGoalBtColor);
							b_prover = true;
						} else {
							subGoalsButton[p][j]
									.setBackground(assistantSubGoalBgColor);
							b_prover = false;
						}

						int state = subop.getState(j);
						switch (state) {
						case 0:
							setButtonStart(subGoalsButton[p][j], true, true,
									b_prover);
							break;
						case 1:
							setButtonProved(subGoalsButton[p][j], true, true);
							break;
						case 2:
							setButtonUnproved(subGoalsButton[p][j], true, true,
									state);
							subGoalsButton[p][j].setToolTipText("invalid");
							break;
						case 3:
							setButtonUnproved(subGoalsButton[p][j], true, true,
									state);
							subGoalsButton[p][j].setToolTipText("unknown");
							break;
						case 4:
							setButtonUnproved(subGoalsButton[p][j], true, true,
									state);
							subGoalsButton[p][j].setToolTipText("timeout");
							break;
						case 5:
							setButtonUnproved(subGoalsButton[p][j], true, true,
									state);
							subGoalsButton[p][j].setToolTipText("failure");
							break;
						default:
							break;
						}

						subGoalsButton[p][j].computeSize(SWT.DEFAULT, viewer
								.getItemHeight());
						subGoalsButton[p][j]
								.addMouseListener(new BtnListener());
						subGoalsButton[p][j].setMenu(menu);

						subGoalsEditor[p][j].grabHorizontal = true;
						subGoalsEditor[p][j].minimumHeight = subGoalsButton[p][j]
								.getSize().y;
						subGoalsEditor[p][j].minimumWidth = subGoalsButton[p][j]
								.getSize().x;
						subGoalsEditor[p][j].setEditor(subGoalsButton[p][j],
								subitem, j + 2);

						// sets goal/subgoal numbers and prover number in
						// button's datas
						subGoalsButton[p][j].setData("goal", new int[] { g + 1,
								yy + 1 });
						subGoalsButton[p][j].setData("prover", new Integer(j));
						subGoalsButton[p][j].setRedraw(true);
					}
					p++;
				}
			}
			n++;
		}

		// Expands items
		if (viewer.getItemCount() > 0) {
			for (int r = 0; r < NUMBER_OF_FUNCTIONS; r++) {
				int a = ((int[]) functionsInView.get(r))[0];
				Function fct = (Function) FileInfos.functions.get(a - 1);
				if (fct.isItem_expanded()) {
					TreeItem tit = viewer.getItem(r);
					tit.setExpanded(true);
					for (int y = 0; y < tit.getItemCount(); y++) {
						int z = ((int[]) tit.getItem(y).getData("goal"))[0];
						PO e = (PO) FileInfos.goals.get(z - 1);
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

		for (int x = 0; x < NUMBER_OF_FUNCTIONS; x++) {
			for (int y = 0; y < proversNumber; y++) {
				functionsEditor[x][y].layout();
				functionsButton[x][y].getParent().layout();
			}
		}

		for (int x = 0; x < NUMBER_OF_GOALS; x++) {
			for (int y = 0; y < proversNumber; y++) {
				goalsEditor[x][y].layout();
				goalsButton[x][y].getParent().layout();
			}
		}

		for (int x = 0; x < NUMBER_OF_SUBGOALS; x++) {
			for (int y = 0; y < proversNumber; y++) {
				subGoalsEditor[x][y].layout();
				subGoalsButton[x][y].getParent().layout();
			}
		}
	}

	@SuppressWarnings("unused")
	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				ProverView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer);
		viewer.setMenu(menu);
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

	private void fillContextMenu(IMenuManager manager) {
		// manager.add(action);
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(runAllProvers);
		manager.add(kill);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(mark);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(split);
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
				split.setEnabled(false);
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
					proveAll(-1, false, true);
				} else if (FileInfos.markedGoal == 0) {
					proveAll(-1, true, true);
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
		showOnlyUnprovedGoals = new Action() {
			public void run() {
				if (!FileInfos.showOnlyUnprovedGoals) {
					FileInfos.showOnlyUnprovedGoals = true;
					if (!showAllLines) {
						CURSOR = 0;
						updateSlider(true);
					} else {
						updateView();
					}
				}
				showAllGoals.setChecked(false);
				setChecked(true);
			}
		};
		showOnlyUnprovedGoals.setChecked(false);
		showOnlyUnprovedGoals.setText("Show Unproved");
		showOnlyUnprovedGoals.setToolTipText("Show only unproved goals");

		// show proved goals
		showAllGoals = new Action() {
			public void run() {
				if (FileInfos.showOnlyUnprovedGoals) {
					FileInfos.showOnlyUnprovedGoals = false;
					if (!showAllLines) {
						CURSOR = 0;
						updateSlider(true);
					} else {
						updateView();
					}
				}
				showOnlyUnprovedGoals.setChecked(false);
				setChecked(true);
			}
		};
		showAllGoals.setChecked(true);
		showAllGoals.setText("Show All");
		showAllGoals.setToolTipText("Show proved and unproved goals");

		// fold the tree viewer
		foldTree = new Action() {
			public void run() {
				TreeItem[] y = viewer.getItems();
				for (int p = 0; p < y.length; p++) {
					y[p].setExpanded(false);
				}
				for (int p = 0; p < FileInfos.functions.size(); p++) {
					((Function) FileInfos.functions.get(p)).collapse();
				}
				if (!showAllLines)
					updateSlider(true);
				else
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
					((Function) FileInfos.functions.get(p)).expand();
				}
				if (!showAllLines)
					updateSlider(true);
				else
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

		// splits a goal
		split = new Action() {
			public void run() {
				int spl = split();
				if (spl > 1) {
					int gnum = Splitter.getNumG();
					PO popo = (PO) FileInfos.goals.get(gnum - 1);
					popo.cleanSubGoals();
					for (int l = 1; l <= spl; l++) {
						popo.addSubGoal();
					}
					updateView();
				}
			}
		};
		split.setImageDescriptor(ImageDescriptor
				.createFromURL(IConstants.URL_SPLIT_BTN));
		split.setToolTipText("Split the selected po");
		split.setEnabled(false);

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

		int lignes, colonnes;

		lignes = subGoalsButton.length;
		try {
			colonnes = subGoalsButton[0].length;
		} catch (Exception e) {
			colonnes = 0;
		}

		// All buttons dispose :
		for (int p = 0; p < lignes; p++) {
			for (int q = 0; q < colonnes; q++) {
				try {
					subGoalsButton[p][q].dispose();
				} catch (Exception e) {
				}
			}
		}

		lignes = goalsButton.length;
		try {
			colonnes = goalsButton[0].length;
		} catch (Exception e) {
			colonnes = 0;
		}

		// All buttons dispose :
		for (int p = 0; p < lignes; p++) {
			for (int q = 0; q < colonnes; q++) {
				try {
					goalsButton[p][q].dispose();
				} catch (Exception e) {
				}
			}
		}

		// item with the third board of buttons
		lignes = functionsButton.length;
		try {
			colonnes = functionsButton[0].length;
		} catch (Exception e) {
			colonnes = 0;
		}

		for (int p = 0; p < lignes; p++) {
			for (int q = 0; q < colonnes; q++) {
				try {
					functionsButton[p][q].dispose();
				} catch (Exception e) {
				}
			}
		}

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
	 * @param sgoalNum
	 *            the subgoal number
	 * @param proverNum
	 *            the prover number
	 * @throws SWTException
	 */
	public synchronized void updateElementAt(int goalNum, int sgoalNum,
			int proverNum) throws SWTException {

		viewer.getColumn(proverNum + 2).setToolTipText("");

		boolean subGoal = sgoalNum > 0;
		boolean p = FileInfos.status[proverNum].equals("prover");

		int colonne = proverNum;
		int ligne = -1;
		int state = 0;
		Button b;

		if (subGoal) {
			ligne = getSubGoalRow(goalNum, sgoalNum);
			if (ligne >= 0) {
				state = ((PO) FileInfos.goals.get(goalNum - 1)).getSubGoal(
						sgoalNum).getState(proverNum);

				b = subGoalsButton[ligne][colonne];

				switch (state) {
				case 0:
					setButtonStart(b, true, true, p);
					b.setToolTipText("");
					break;
				case 1:
					setButtonProved(b, true, true);
					b.setToolTipText("");
					updateSubButtons(goalNum, sgoalNum, proverNum);
					break;
				case 2:
					setButtonUnproved(b, true, true, state);
					b.setToolTipText("invalid");
					updateSubButtons(goalNum, sgoalNum, proverNum);
					break;
				case 3:
					setButtonUnproved(b, true, true, state);
					b.setToolTipText("unknown");
					updateSubButtons(goalNum, sgoalNum, proverNum);
					break;
				case 4:
					setButtonUnproved(b, true, true, state);
					b.setToolTipText("timeout");
					updateSubButtons(goalNum, sgoalNum, proverNum);
					break;
				case 5:
					setButtonUnproved(b, true, true, state);
					b.setToolTipText("failure");
					updateSubButtons(goalNum, sgoalNum, proverNum);
					break;
				default:
					break;
				}
			}
		}

		state = ((PO) FileInfos.goals.get(goalNum - 1)).getState(proverNum);

		ligne = getGoalRow(goalNum);
		if (ligne >= 0) {

			b = goalsButton[ligne][colonne];

			switch (state) {
			case 0:
				setButtonStart(b, true, false, p);
				b.setToolTipText("");
				updateButtons(goalNum, proverNum);
				break;
			case 1:
				setButtonProved(b, true, false);
				b.setToolTipText("");
				updateButtons(goalNum, proverNum);
				break;
			case 2:
				setButtonUnproved(b, true, false, state);
				b.setToolTipText("invalid");
				updateButtons(goalNum, proverNum);
				break;
			case 3:
				setButtonUnproved(b, true, false, state);
				b.setToolTipText("unknown");
				updateButtons(goalNum, proverNum);
				break;
			case 4:
				setButtonUnproved(b, true, false, state);
				b.setToolTipText("timeout");
				updateButtons(goalNum, proverNum);
				break;
			case 5:
				setButtonUnproved(b, true, false, state);
				b.setToolTipText("failure");
				updateButtons(goalNum, proverNum);
				break;
			default:
				break;
			}
		}

		switch (state) {
		case 0:
			updateFButton(goalNum, proverNum, false);
			break;
		case 1:
			updateFButton(goalNum, proverNum, true);
			break;
		case 2:
			updateFButton(goalNum, proverNum, false);
			break;
		case 3:
			updateFButton(goalNum, proverNum, false);
			break;
		case 4:
			updateFButton(goalNum, proverNum, false);
			break;
		case 5:
			updateFButton(goalNum, proverNum, false);
			break;
		default:
			break;
		}

	}

	/**
	 * Modifies the goals items in the proving view (proved/unproved) and
	 * updates the colors and images of buttons.
	 * 
	 * @param goalNumber
	 *            the goal number
	 * @param proverNumber
	 *            the prover number
	 * @throws SWTException
	 */
	private void updateButtons(int goalNumber, int proverNumber)
			throws SWTException {

		boolean stop = false;

		TreeItem gitem = null;
		// we get the goal item
		TreeItem[] content = viewer.getItems();
		forone: for (int u = 0; u < content.length; u++) {
			if (content[u].getExpanded()
					|| (!content[u].getExpanded() && showAllLines)) {
				TreeItem[] underContent = content[u].getItems();
				for (int o = 0; o < underContent.length; o++) {
					if (((int[]) underContent[o].getData("goal"))[0] == goalNumber) {
						gitem = underContent[o];
						break forone;
					}
				}
			}
		}

		// is the goal proved?
		boolean is_proved = ((PO) FileInfos.goals.get(goalNumber - 1))
				.isProved();

		if (is_proved) { // if the goal has been proved
			if (gitem != null) {
				gitem.setImage(1, IConstants.IMAGE_BALL_GREEN);
			}
		}

		if (!is_proved) { // if the goal hasn't been proved
			// for all other provers
			bouclefor: for (int b = 0; b < FileInfos.provers.length; b++) {
				int etat = ((PO) FileInfos.goals.get(goalNumber - 1))
						.getState(b);
				if (etat == 1) { // if the goal is proved
					// we can stop, the goal is proved
					stop = true;
					break bouclefor;
				}
			}
			if (!stop) { // if we didn't stop, it means that the goal is
							// unproved
				if (gitem != null) {
					if (!is_proved)
						gitem.setImage(1, IConstants.IMAGE_BALL_RED);
				}
			}
		}
	}

	/**
	 * Modifies the subgoals items in the proving view (proved/unproved) and
	 * updates the colors and images of buttons.
	 * 
	 * @param goalNumber
	 *            the goal number
	 * @param sgoalNumber
	 *            the subgoal number
	 * @param proverNumber
	 *            the prover number
	 * @throws SWTException
	 */
	private void updateSubButtons(int goalNumber, int sgoalNumber,
			int proverNumber) throws SWTException {

		boolean stop = false;

		TreeItem gitem = null;

		// we gets the item
		TreeItem[] content = viewer.getItems();
		forone: for (int u = 0; u < content.length; u++) {
			TreeItem[] underContent = content[u].getItems();
			for (int o = 0; o < underContent.length; o++) {
				if (((int[]) underContent[o].getData("goal"))[0] == goalNumber) {
					TreeItem[] underUnderContent = underContent[o].getItems();
					for (int d = 0; d < underUnderContent.length; d++) {
						if (((int[]) underUnderContent[d].getData("goal"))[1] == sgoalNumber) {
							gitem = underUnderContent[d];
							break forone;
						}
					}
				}
			}
		}

		// is the subgoal proved?
		boolean is_proved = ((PO) FileInfos.goals.get(goalNumber - 1))
				.getSubGoal(sgoalNumber).isProved();

		if (is_proved) { // if the subgoal has been proved
			if (gitem != null) {
				gitem.setImage(1, IConstants.IMAGE_BALL_GREEN);
			}
		} else { // if the subgoal hasn't been proved
			bouclefor: for (int b = 0; b < FileInfos.provers.length; b++) { // for
																			// all
																			// other
																			// provers
				int etat = ((PO) FileInfos.goals.get(goalNumber - 1))
						.getSubGoal(sgoalNumber).getState(b);
				if (etat == 1) { // if the subgoal is proved
					stop = true;
					break bouclefor; // stop
				}
			}
			if (!stop) { // if stop is true, the subgoal is unproved
				if (gitem != null) {
					gitem.setImage(1, IConstants.IMAGE_BALL_RED);
					gitem.setChecked(false);
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
	private void updateFButton(int goalNumber, int proverNumber, boolean proved)
			throws SWTException {

		boolean stop = false;
		PO po = (PO) FileInfos.goals.get(goalNumber - 1);
		int fnum = po.getFnum();
		Function fc = (Function) FileInfos.functions.get(fnum - 1);

		if (!proved) { // if the goal hasn't been proved
			bouclefor: for (int b = 0; b < FileInfos.provers.length; b++) { // for
																			// all
																			// other
																			// provers
				int etat = ((PO) FileInfos.goals.get(goalNumber - 1))
						.getState(b);
				if (etat == 1) { // if goal is proved
					stop = true;
					break bouclefor; // stop
				}
			}
		}

		// get the function's position
		int g = getFunctionRow(goalNumber);

		if (g >= 0) { // if the function is in the view

			if (!stop) { // the goal is unproved
				TreeItem fitem = null;
				if (g >= 0) {
					fitem = viewer
							.getItem(((int[]) functionsButton[g][proverNumber]
									.getData("item"))[0]);
				}

				if (fitem != null) {
					if (fc.isProved()) { // if the function is proved
						fitem.setImage(1, IConstants.IMAGE_BALL_GREEN);
					} else { // else
						fitem.setImage(1, IConstants.IMAGE_BALL_RED);
					}
				}
			}

			int firstGoal = goalNumber;
			boolean functionProvedForProver = true;
			boolean start = true;

			// gets the first goal of the function
			while (firstGoal > 0
					&& ((PO) FileInfos.goals.get(firstGoal - 1)).getFnum() == fnum) {
				firstGoal--;
			}
			firstGoal++;
			if (firstGoal < 1) {
				TraceDisplay.print(MessageType.WARNING,
						"ProverView.updateButtons() : goal " + goalNumber
								+ " unknown for function '" + fc.getName()
								+ "'");
				return;
			}

			// for all goals of the function
			while ((firstGoal <= FileInfos.whyFileNumber)
					&& (((PO) FileInfos.goals.get(firstGoal - 1)).getFnum() == fnum)) {
				int state = ((PO) FileInfos.goals.get(firstGoal - 1))
						.getState(proverNumber);
				// one of goals is unproved
				if (state != 1) {
					functionProvedForProver = false;
					// if the function has been touched before, we can break
					if (!start)
						break;
				}
				// one of the goals has been touched before
				if (state != 0) {
					start = false;
					// if the function was proved but is not proved now
					// we can break
					if (!functionProvedForProver)
						break;
				}
				firstGoal++;
			}

			// changes the color of the function's button
			if (functionProvedForProver)
				setButtonProved(functionsButton[g][proverNumber], false, false);
			else if (start)
				setButtonStart(functionsButton[g][proverNumber], false, false,
						FileInfos.status[proverNumber].equals("prover"));
			else
				setButtonUnproved(functionsButton[g][proverNumber], false,
						false, 0);
		}
	}

	/**
	 * Puts orange color or an orange ball in the goal/subgoals and function
	 * buttons to show that the prover is working on.
	 * 
	 * @param goalNum
	 *            the goal number
	 * @param subgoal
	 *            the subgoal number
	 * @param proverNum
	 *            the prover number
	 */
	public synchronized void working(int goalNum, int subgoal, int proverNum) {

		int colonne = proverNum;
		int ligne = getSubGoalRow(goalNum, subgoal);
		if (ligne >= 0) {
			Button b = subGoalsButton[ligne][colonne];
			setButtonWorking(b, true, true);
		}

		ligne = getGoalRow(goalNum);
		if (ligne >= 0) {
			Button b = goalsButton[ligne][colonne];
			setButtonWorking(b, true, false);
		}

		ligne = getFunctionRow(goalNum);
		if (ligne >= 0) {
			Button b = functionsButton[ligne][colonne];
			setButtonWorking(b, false, false);
		}

		viewer.getColumn(proverNum + 2).setToolTipText(
				"Running on goal " + goalNum + "...");
	}

	/**
	 * Gets the row of a subgoal in the subgoals buttons' board
	 * 
	 * @param goalNum
	 *            goal number
	 * @param subGoalNum
	 *            subgoal number
	 * @return int index of the subgoal, -1 if it isn't in
	 */
	private int getSubGoalRow(int goalNum, int subGoalNum) {

		int ligne = -1;
		for (int r = 0; r < subGoalsButton.length; r++) {
			int[] beuh = (int[]) subGoalsButton[r][0].getData("goal");
			if (goalNum == beuh[0] && subGoalNum == beuh[1]) {
				ligne = r;
				break;
			}
		}
		return ligne;
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
			if (((int[]) goalsInView.get(p))[0] == goalNum) {
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
	 * @return int index of the function in the view, -1 if it isn't in
	 */
	private int getFunctionRow(int goalNum) {

		int line = -1; // function's row in the ArrayList functionsInView
		int frow = -1; // the function number (cf. FileInfos.functions[])

		PO po = (PO) FileInfos.goals.get(goalNum - 1);
		frow = po.getFnum();

		if (frow == 0) {
			TraceDisplay.print(MessageType.WARNING,
					"WhyView.getFunctionRow() : function '" + po.getFname()
							+ "' unknown...");
			return -1;
		}

		for (int p = 0; p < functionsInView.size(); p++) {
			if (((int[]) functionsInView.get(p))[0] == frow) {
				line = p;
				break;
			}
		}
		return line;
	}

	/**
	 * Enables/Disables the button to stop provers
	 * 
	 * @param b
	 *            : true => enables the button ; false => disables the button
	 */
	public synchronized void killButton(boolean b) {

		if (b) {
			kill.setEnabled(true);
			reset.setEnabled(false);
		} else {
			if (threads.size() == 0)
				kill.setEnabled(false);
			reset.setEnabled(true);
		}
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
		int goalNumPrim = 0;
		int funcNum = 0;
		// String kind = "";
		String message;
		Image image;
		TreeItem m = null;

		try {
			m = viewer.getSelection()[0]; // gets the selected item
			goalNum = ((int[]) m.getData("goal"))[0]; // goal num
			goalNumPrim = ((int[]) m.getData("goal"))[1]; // subgoalnum
		} catch (Exception e) {
		}

		if (goalNum > 0) {
			String file;
			if (goalNumPrim > 0) {
				// this is a subgoal
				file = FileInfos.getName() + "_po" + (goalNum) + "-"
						+ (goalNumPrim) + ".why";
			} else {
				// this is a goal
				file = FileInfos.getName() + "_po" + (goalNum) + ".why";
			}
			Highlightor.setGoal(goalNum);
			// gets fields from the .xpl file
			// kind = Highlightor.selectFromXPL();
			afficheGV(goalNum, file); // sets the pretty printed goal in the PO
										// Viewer
			message = ((PO) FileInfos.goals.get(goalNum - 1)).getTitle();
			image = IConstants.IMAGE_PO;
			split.setEnabled(true);
		} else {
			// this is a function
			afficheGV(-1, null); // clean the PO Viewer
			funcNum = ((int[]) m.getData("function"))[0];
			message = ((Function) FileInfos.functions.get(funcNum))
					.getBehavior();
			image = IConstants.IMAGE_FUNC;
			split.setEnabled(false);
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
	private void prove(ArrayList<String[]> a, int proverNumber, boolean all) {
		ProverViewUpdator uvw = new ProverViewUpdator((ProverView) this);
		ProverThread m = new ProverThread(a, proverNumber, all, uvw);
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
	private void proveAll(int prover, boolean begin_first, boolean notall) {

		int first = 1;
		int ntot = FileInfos.whyFileNumber;
		if (ntot == 0) { // empty set
			return;
		}

		if (!begin_first) { // we begin with a marked goal
			first = FileInfos.markedGoal;
			if (first == 0) {
				return;
			}
		}

		// Build the set of goals to prove
		ArrayList<String[]> array = new ArrayList<String[]>(); // the list of
																// goals sets
		String[] goalsSet = new String[2]; // goals set : the first and the last
											// of a set of consecutive goals

		if (!notall) {
			// simple : a set with all goals
			goalsSet = new String[2];
			goalsSet[0] = "" + first;
			goalsSet[1] = "" + ntot;
			array.add(goalsSet);
		} else {
			// less simple : only unproved goals!
			boolean inASet = false;
			int nSet = 0; // nb of goals in the current set
			int w;
			for (w = first - 1; w < ntot; w++) {
				inASet = false;
				if (!((PO) FileInfos.goals.get(w)).isProved()) { // the goal
																	// isn't
																	// proved :
																	// begin a
																	// set!
					inASet = true;
					nSet++;
				}
				if (inASet && (nSet == 1)) { // beginning a set
					goalsSet = new String[2];
					goalsSet[0] = "" + (w + 1);
				}
				if (!inASet && (nSet > 0)) { // ending a set
					nSet = 0;
					goalsSet[1] = "" + w;
					array.add(goalsSet); // save the set
				}
			}
			if (inASet) { // we were checking goals at the end of the loop
				goalsSet[1] = "" + w; // end the set and
				array.add(goalsSet); // save the set
			}
		}

		prove(array, prover, !notall); // prove all the goals we've selected
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
			int[] goal;
			int[] function = null;
			boolean is_function_item = false;
			goal = (int[]) items[0].getData("goal");
			if (goal == null) {
				is_function_item = true;
				function = (int[]) items[0].getData("function");
				if (function == null) {
					TraceDisplay
							.print(MessageType.ERROR,
									"ProverView.mark() : selected item represents neither a goal nor a function !");
					return;
				}
			}

			// if it's a function item, we get the first goal of
			// this function which appears in the view
			if (is_function_item) {
				goal = (int[]) items[0].getItem(0).getData("goal");
				fitem = items[0];
				gitem = (items[0].getItems())[0];
			} else {
				if (goal[1] > 0) {
					fitem = items[0].getParentItem().getParentItem();
					gitem = items[0].getParentItem();
				} else {
					fitem = items[0].getParentItem();
					gitem = items[0];
				}
			}

			// if the function was ever marked, wew can consider that the
			// selected
			// goal is the marked goal => we remove the marks of the goal and of
			// the function
			if (is_function_item && (FileInfos.markedGoal > 0)) {
				int fmarked = ((PO) FileInfos.goals
						.get(FileInfos.markedGoal - 1)).getFnum();
				if (fmarked == function[0]) {
					goal = new int[1];
					// selected goal = marked goal
					goal[0] = FileInfos.markedGoal;
					for (int s = 0; s < fitem.getItems().length; s++) {
						int t = ((int[]) fitem.getItem(s).getData("goal"))[0];
						if (t == goal[0]) {
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
				if (FileInfos.markedGoal == goal[0]) {
					// we unmark it
					fitem.setForeground(0, IConstants.COLOR_BLACK);
					gitem.setForeground(0, IConstants.COLOR_GREY);
					FileInfos.markedGoal = 0;
				}
				// else
				else {
					// we mark the goal
					gitem.setForeground(0, markedGoalColor);
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
									int gprim = ((int[]) t2.getData("goal"))[0];
									if (gprim == g) {
										// we unmark it!
										t2.setForeground(0,
												IConstants.COLOR_GREY);
										t2.getParentItem().setForeground(0,
												IConstants.COLOR_BLACK);
									}
								}
							}
						}
					}
					FileInfos.markedGoal = goal[0];
					fitem.setForeground(0, markedFuncColor);
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
	private int split() {
		TreeItem[] items = viewer.getSelection();
		if (items != null && items.length == 1) {
			int num = ((int[]) items[0].getData("goal"))[0];
			int snum = ((int[]) items[0].getData("goal"))[1];
			if (snum == 0) {
				Splitter.reset();
				Splitter.setNumG(num);
				Pointer.breakUp();
				return Splitter.split();
			}
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
	private void afficheGV(int gnum, String whyFileName) {

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
			TraceDisplay.print(MessageType.ERROR, "ProverView.afficheGV() : "
					+ e);
		} catch (IOException e) {
			TraceDisplay.print(MessageType.ERROR, "ProverView.afficheGV() : "
					+ e);
		}
	}

	/**
	 * Changes the slider draw in the view. It depends on items to show in the
	 * tree viewer (goals, expanded or not functions) and on the cursor.
	 * 
	 * @param updateView
	 *            do we have to call the <code>updateView()</code> function
	 *            after having executed this one
	 */
	public void updateSlider(boolean updateView) {

		int display;
		int maxima = 1;
		boolean pursuit = true;

		IPreferenceStore store = EditeurWHY.getDefault().getPreferenceStore();
		if (!store.getBoolean(IConstants.PREF_SHOW_ALL_LINES)) {
			display = store.getInt(IConstants.PREF_SHOW_NB_LINES);
		} else {
			display = 1;
			pursuit = false;
		}

		if (pursuit) {

			int i = FileInfos.functions.size() - 1;
			int res = 0;
			// we get the first function again visible in the view
			// when the slider is low
			bouclefor: while (i >= 0) {
				Function fctn = (Function) FileInfos.functions.get(i);
				if (!FileInfos.showOnlyUnprovedGoals
						|| (FileInfos.showOnlyUnprovedGoals && !fctn
								.isProved() /* isChecked() */)) {
					if (fctn.isItem_expanded()) {
						if (FileInfos.showOnlyUnprovedGoals)
							res += fctn.getPo_unproved();
						else
							res += fctn.getPo();
						res++;
					} else {
						res++;
					}
					if (res >= display) {
						break bouclefor;
					}
				}
				i--;
			}
			// we count now the number of expanded functions which
			// precede the previously recovered function
			int ne = 0;
			for (int y = 0; y < i; y++) {
				Function fctn = (Function) FileInfos.functions.get(y);
				if (!FileInfos.showOnlyUnprovedGoals
						|| (FileInfos.showOnlyUnprovedGoals && !fctn
								.isProved() /* isChecked() */)) {
					if (fctn.isItem_expanded()) {
						ne++;
					}
				}
			}

			// we count the existing number of lines in the functions' tree
			int pp = 0;
			int e = FileInfos.functions.size();
			for (int a = 0; a < e; a++) {
				Function fctn = (Function) FileInfos.functions.get(a);
				if (!FileInfos.showOnlyUnprovedGoals
						|| (FileInfos.showOnlyUnprovedGoals && !fctn
								.isProved() /* isChecked() */)) {
					if (fctn.isItem_expanded()) {
						if (FileInfos.showOnlyUnprovedGoals)
							pp += fctn.getPo_unproved();
						else
							pp += fctn.getPo();
						pp++;
					} else {
						pp++;
					}
				}
			}
			maxima = pp - ne;
		}

		// updates view if necessary
		if (updateView)
			updateView();
	}

	/**
	 * This function calculates the number of the first goal included in the
	 * view (even if this goal is hidden because its function's item is
	 * collapsed)
	 * 
	 * @return int the first goal number
	 */
	private int getCursorGoal() {

		int g;
		int cursor = 1;

		if (FileInfos.goals.size() == 0) {
			return 1;
		}

		// we search the first goal
		g = 1;
		if (FileInfos.showOnlyUnprovedGoals) {
			// in this case, the goal mustn't be proved
			while (((PO) FileInfos.goals.get(g - 1)).isProved()) {
				g++;
			}
		}

		// while we haven't joined up with the main cursor
		while (cursor <= CURSOR) {

			g++;

			PO po = (PO) FileInfos.goals.get(g - 1);

			while (!isObjectInView((Function) FileInfos.functions.get(po
					.getFnum() - 1))) {
				// the first goal of the next function
				g = ((Function) FileInfos.functions.get(po.getFnum()))
						.getFirst_po();
				po = (PO) FileInfos.goals.get(g - 1);
			}

			while (!isObjectInView(po)) {
				g++;
				po = (PO) FileInfos.goals.get(g - 1);
			}

			cursor++;
		}

		return g;
	}

	/**
	 * <p>
	 * Sets the ArrayList <code>goalsInView</code> and
	 * <code>functionsInView</code> datas. These object must contain
	 * respectively the numbers of all goals shown in the view and all functions
	 * items appearing in the tree viewer.
	 * </p>
	 * 
	 * @param startGoal
	 *            the first goal (hidden or not) which belongs to the view
	 * @param nbLine
	 *            the number of lines to show in the view
	 */
	private void getGoals(int startGoal, int nbLine) {

		int i = 0;
		int g = startGoal;
		int max = FileInfos.whyFileNumber;
		int y;

		goalsInView.clear();
		functionsInView.clear();

		if (FileInfos.goals.size() == 0) {
			return;
		}

		PO po = (PO) FileInfos.goals.get(g - 1);
		y = po.getFnum();

		String f = "";

		// while we haven't exceed the nb of lines to show
		// and whie there are again goals
		while ((i <= nbLine) && (g <= max)) {

			PO op = (PO) FileInfos.goals.get(g - 1);

			// if we show all goals or if we show only unproved
			// goals and this goal's unproved
			if (!FileInfos.showOnlyUnprovedGoals
					|| (FileInfos.showOnlyUnprovedGoals && !op.isProved())) {
				String fn = op.getFname();
				if (!fn.equals(f)) { // unknown function : creates a new one
					f = fn;
					y = op.getFnum();
					i++;
					if (i <= nbLine + 1)
						functionsInView.add(new int[] { y });
				}
				// the function is expanded => we record the goal
				if ((i <= nbLine + 1)
						&& ((Function) FileInfos.functions.get(y - 1))
								.isItem_expanded()) {
					goalsInView.add(new int[] { g });
					i++;
					g++;
				}
				// else
				if ((i <= nbLine + 1)
						&& !((Function) FileInfos.functions.get(y - 1))
								.isItem_expanded()) {
					goalsInView.add(new int[] { g });
					if (showAllLines) { // we have either keep all goals
						g++;
					} else { // or search the next goal into the next function
						while ((g <= max)
								&& ((PO) FileInfos.goals.get(g - 1)).getFname()
										.equals(f)) {
							g++;
						}
					}
				}
			} else {
				// here, we go to search the next unproved goal
				while ((g <= max) && op.isProved()) {
					g++;
					if (g > max)
						break;
					op = (PO) FileInfos.goals.get(g - 1);
				}
			}
		}
	}

	/**
	 * Returns true if a PO or Function objectshould appear in the Prover View
	 * 
	 * @param obj
	 *            a PO or Function object
	 * @return boolean true if it appears in the view, false otherwise
	 */
	private boolean isObjectInView(Object obj) {

		if (obj instanceof Function) { // for a Function
			Function f = (Function) obj;
			if (FileInfos.showOnlyUnprovedGoals) { // only unproved functions
				if (f.isProved()) {
					return false;
				} else {
					return true;
				}
			} else { // all functions
				return true;
			}
		} else if (obj instanceof PO) { // for a PO
			PO p = (PO) obj;
			Function f = (Function) FileInfos.functions.get(p.getFnum() - 1);
			if (FileInfos.showOnlyUnprovedGoals) {
				// proved POs are rejected
				if (p.isProved()) {
					return false;
				} else {
					// for unproved POs :
					if (f.isItem_expanded()) {
						// if the function's item is expanded, the PO is shown
						return true;
					} else {
						// but if the function's item is collapsed
						int a = f.getFirst_po();
						int z = a + f.getPo() - 1;
						int e;
						// we search the first goal which should appear
						for (e = a; e < z; e++) {
							if (e > p.getNum()) {
								break;
							}
							PO q = (PO) FileInfos.goals.get(e - 1);
							// thus, the first unproved goal
							if (!q.isProved()) {
								break;
							}
						}
						if (e == p.getNum()) { // if it's our goal : OK
							return true;
						} else { // else, too bad
							return false;
						}
					}
				}
			} else { // proved and unproved POs are accepted
				if (f.isItem_expanded()) {
					// if the function's item is expanded, the PO is shown
					return true;
				} else {
					// but if the item is expanded
					if (p.getNum() == f.getFirst_po()) {
						// our goal must be the first into the function
						return true;
					} else {
						return false;
					}
				}
			}
		} else {
			return false;
		}
	}

	/**
	 * Makes prover's statistics
	 * 
	 * @param proverNum
	 *            the prover number
	 */
	public synchronized void makeStats(int proverNum) {

		int goalNum = FileInfos.whyFileNumber;

		int state;
		int proved = 0;
		int invalid = 0;
		int unknown = 0;
		int timeout = 0;
		int failure = 0;

		// gets the number of proved, invalid, unknown, timeout and
		// failure results for all goals for this prover
		for (int g = 1; g <= goalNum; g++) {
			state = ((PO) FileInfos.goals.get(g - 1)).getState(proverNum);
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
		viewer.getColumn(proverNum + 2).setToolTipText(stats);
	}

	/**
	 * Displays a warning message if the number of lines to show in the view is
	 * higher than a maximum number fixed to 150
	 */
	public void warn() {

		IPreferenceStore store = EditeurWHY.getDefault().getPreferenceStore();
		boolean all = store.getBoolean(IConstants.PREF_SHOW_ALL_LINES);
		int max = IConstants.PREF_SHOW_NB_LINES_MAX;

		int lines;

		if (all) {
			lines = FileInfos.functions.size() + FileInfos.whyFileNumber + 1;
			if (lines > max) {
				MessageDialog.openWarning(new Shell(), "Overflow",
						"Beware, the number of goals and functions that will be displayed,\n"
								+ "estimated to " + lines
								+ ", exceeds the automatic line limitation\n"
								+ "of the proving view which is fixed to "
								+ max + ".\n"
								+ "This value can be manually defined in the\n"
								+ "'WHY' main preferences page\n");
				store.setValue(IConstants.PREF_SHOW_ALL_LINES, false);
				store.setValue(IConstants.PREF_SHOW_NB_LINES, max);
				EditeurWHY.getDefault().savePluginPreferences();
			}
		}
	}

	/**
	 * Puts the appropriate color or image in a goal or function button which
	 * has never been tried by provers or assistants.
	 * 
	 * @param b
	 *            the button
	 * @param is_goal
	 *            is it a goal button (false => a function button)
	 * @param is_subGoal
	 *            is it a subgoal button
	 * @param is_prover
	 *            is it in a prover column (false => in an assistant column)
	 */
	private void setButtonStart(Button b, boolean is_goal, boolean is_subGoal,
			boolean is_prover) {
		if (is_prover) {
				b.setImage(IConstants.IMAGE_START);
		} else {
			b.setImage(null);
			if (is_goal) {
				if (is_subGoal)
					b.setBackground(assistantSubGoalBgColor);
				else
					b.setBackground(assistantGoalBgColor);
			} else {
				b.setBackground(assistantFuncBgColor);
			}
		}
	}

	/**
	 * Puts the appropriate color or image in a goal or function button which
	 * has been proved by a prover or with an assistant.
	 * 
	 * @param b
	 *            the button
	 * @param is_goal
	 *            is it a goal button (false => a function button)
	 * @param is_subGoal
	 *            is it a subgoal button
	 */
	private void setButtonProved(Button b, boolean is_goal, boolean is_subGoal) {
			b.setImage(IConstants.IMAGE_VALID);
	}

	/**
	 * Puts the appropriate color or image in a goal or function button which
	 * has not been proved by a prover or by an assistant.
	 * 
	 * @param b
	 *            the button
	 * @param is_goal
	 *            is it a goal button (false => a function button)
	 * @param is_subGoal
	 *            is it a subgoal button
	 * @param errno
	 *            the number of the error returned by the prover/assistant
	 */
	private void setButtonUnproved(Button b, boolean is_goal,
			boolean is_subGoal, int errno) {
			switch (errno) {
			case 2:
				b.setImage(IConstants.IMAGE_INVALID);
				break;
			case 3:
				b.setImage(IConstants.IMAGE_UNKNOWN);
				break;
			case 4:
				b.setImage(IConstants.IMAGE_TIME_OUT);
				break;
			case 5:
				b.setImage(IConstants.IMAGE_FAILURE);
				break;
			default:
				b.setImage(IConstants.IMAGE_UNPROVED);
				break;
			}
	}

	/**
	 * Puts the appropriate color or image in a goal or function button on which
	 * a prover or an assistant is working.
	 * 
	 * @param b
	 *            the button
	 * @param is_goal
	 *            is it a goal button (false => a function button)
	 * @param is_subGoal
	 *            is it a subgoal button
	 */
	private void setButtonWorking(Button b, boolean is_goal, boolean is_subGoal) {
			b.setImage(IConstants.IMAGE_WORKING);
	}
}

package net.eclipse.why.editeur;

import java.net.URL;

import net.eclipse.why.editeur.actions.DP;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

/**
 * Interface which contains all identifying chains and constants values
 * 
 * @author A.Oudot
 */
public interface IConstants {

	
	//Plugin and views IDs
	public final static String PO_VIEW_ID			=	"net.eclipse.why.editeur.views.POViewer";
	public final static String PO_VIEW_TITLE		=	"PO Viewer";
	public final static String COMMANDS_VIEW_ID		=	"net.eclipse.why.editeur.views.CommandsView";
	public final static String COMMANDS_VIEW_TITLE	=	"Commands";
	public final static String PROVER_VIEW_ID		=	"net.eclipse.why.editeur.views.ProverView";
	public final static String PROVER_VIEW_TITLE	=	"Prover View";
	public final static String PLUGIN_ID			=	"net.eclipse.why.editeur";
	
	//Separation and ID Strings
	public final static String FILE_DEMONSTRATOR	=	"%s";
	public final static String DIR_DEMONSTRATOR		=	"%r";
	public final static String LINE_SEPARATOR		=	"NEXT.";
	public final static String ELEMENT_SEPARATOR	=	"#";
	
	
	// Images of buttons in Prover View
	//final static URL url0							=	EditeurWHY.getDefault().getBundle().getEntry("icons/empty.gif");
	final static URL url1							=	EditeurWHY.getDefault().getBundle().getEntry("icons/start.gif");
	final static URL url2							=	EditeurWHY.getDefault().getBundle().getEntry("icons/balls/ballgree.gif");
	final static URL url3							=	EditeurWHY.getDefault().getBundle().getEntry("icons/balls/ballred.gif");
	final static URL url4							=	EditeurWHY.getDefault().getBundle().getEntry("icons/golden_arrow.gif");
	final static URL url5							=	EditeurWHY.getDefault().getBundle().getEntry("icons/balls/ballpink.gif");
	final static URL url6							=	EditeurWHY.getDefault().getBundle().getEntry("icons/ist2_2366446-hourglass.gif");
	final static URL url7							=	EditeurWHY.getDefault().getBundle().getEntry("icons/balls/ballpurp.gif");
	final static URL url8							=	EditeurWHY.getDefault().getBundle().getEntry("icons/cross.gif");
	final static URL url9							=	EditeurWHY.getDefault().getBundle().getEntry("icons/hourglass.gif");
	final static URL url10							=	EditeurWHY.getDefault().getBundle().getEntry("icons/unknown.gif");
	final static URL url11							=	EditeurWHY.getDefault().getBundle().getEntry("icons/green_check.gif");
	final static URL url12							=	EditeurWHY.getDefault().getBundle().getEntry("icons/balls/ballwhit.gif");
	final static URL url13							=	EditeurWHY.getDefault().getBundle().getEntry("icons/x.gif");
	final static URL url14							=	EditeurWHY.getDefault().getBundle().getEntry("icons/goal.gif");
	final static URL url15							=	EditeurWHY.getDefault().getBundle().getEntry("icons/func.gif");
	//public final static Image IMAGE_EMPTY			=	new Image(null, ImageDescriptor.createFromURL(url0).getImageData());
	public final static Image IMAGE_START			=	new Image(null, ImageDescriptor.createFromURL(url1).getImageData());
	public final static Image IMAGE_VALID			=	new Image(null, ImageDescriptor.createFromURL(url11).getImageData());
	public final static Image IMAGE_INVALID			=	new Image(null, ImageDescriptor.createFromURL(url13).getImageData());
	public final static Image IMAGE_WORKING			=	new Image(null, ImageDescriptor.createFromURL(url4).getImageData());
	public final static Image IMAGE_BALL_PINK		=	new Image(null, ImageDescriptor.createFromURL(url5).getImageData());
	public final static Image IMAGE_WAITING			=	new Image(null, ImageDescriptor.createFromURL(url6).getImageData());
	public final static Image IMAGE_BALL_PURP		=	new Image(null, ImageDescriptor.createFromURL(url7).getImageData());
	public final static Image IMAGE_FAILURE			=	new Image(null, ImageDescriptor.createFromURL(url8).getImageData());
	public final static Image IMAGE_TIME_OUT		=	new Image(null, ImageDescriptor.createFromURL(url9).getImageData());
	public final static Image IMAGE_UNKNOWN			=	new Image(null, ImageDescriptor.createFromURL(url10).getImageData());
	public final static Image IMAGE_UNPROVED		=	new Image(null, ImageDescriptor.createFromURL(url13).getImageData());
	public final static Image IMAGE_BALL_WHITE		=	new Image(null, ImageDescriptor.createFromURL(url12).getImageData());
	public final static Image IMAGE_BALL_GREEN		=	new Image(null, ImageDescriptor.createFromURL(url2).getImageData());
	public final static Image IMAGE_BALL_RED		=	new Image(null, ImageDescriptor.createFromURL(url3).getImageData());
	public final static Image IMAGE_PO				=	new Image(null, ImageDescriptor.createFromURL(url14).getImageData());
	public final static Image IMAGE_FUNC			=	new Image(null, ImageDescriptor.createFromURL(url15).getImageData());
	
	
	
	// URLs of images for Prover View functions
	final static URL URL_KILL_BUTTON				=   EditeurWHY.getDefault().getBundle().getEntry("icons/cone_17.png");
	final static URL URL_RUN_ALL_PROVERS			=   EditeurWHY.getDefault().getBundle().getEntry("icons/321.gif");
	final static URL URL_COLLAPSE_BTN				=   EditeurWHY.getDefault().getBundle().getEntry("icons/collapse.gif");
	final static URL URL_EXPAND_BTN					=   EditeurWHY.getDefault().getBundle().getEntry("icons/expand.gif");
	final static URL URL_MARK_BTN					=   EditeurWHY.getDefault().getBundle().getEntry("icons/mark.gif");
	final static URL URL_SPLIT_BTN					=   EditeurWHY.getDefault().getBundle().getEntry("icons/prism.gif");
	final static URL URL_SAVE_BTN					=   EditeurWHY.getDefault().getBundle().getEntry("icons/floppy_disk.gif");
	final static URL URL_LOAD_BTN					=   EditeurWHY.getDefault().getBundle().getEntry("icons/fld312.gif");
	
	
	//Few defined colors
	final static Color COLOR_GREEN  				=	new Color( null,   0,    120,   60 );
	final static Color COLOR_RED    				=	new Color( null,   250,   0,   100 );
	final static Color COLOR_GREY					=	new Color( null,   100,  100,  100 );
	final static Color COLOR_BLACK					=	new Color( null,   0,     0,     0 );
	
	//Highlight colors
	final static Color HIGHLIGHT_GREEN				=	new Color(null, 144, 238, 144);
	final static Color HIGHLIGHT_RED				=	new Color(null, 200, 0, 0);
	
	
	
	//Property Page
	public static final String PROP_WHYOPT			=	"OPT_PROPERTY";
	public static final String PROP_WHYOPT_DEFAULT	=	"1000";
	
	
	//Preferences Page ID Strings
	public static final String PREF_POV_BACKGROUND_COLOR      							=	"POV_BACKGROUND_COLOR";
	public static final String PREF_GOALS_BUTTON_BG_COLOR       						=	"GOALS_BUTTON_BG_COLOR";
	public static final String PREF_SUBGOALS_BUTTON_BG_COLOR       						=	"SUBGOALS_BUTTON_BG_COLOR";
	public static final String PREF_FUNCS_BUTTON_BG_COLOR   							=	"FUNCS_BUTTON_BG_COLOR";
	public static final String PREF_GOALS_BUTTON_PROVED_COLOR							=	"GOALS_BUTTON_PROVED_COLOR";
	public static final String PREF_SUBGOALS_BUTTON_PROVED_COLOR						=	"SUBGOALS_BUTTON_PROVED_COLOR";
	public static final String PREF_FUNCS_BUTTON_PROVED_COLOR							=	"FUNCS_BUTTON_PROVED_COLOR";
	public static final String PREF_GOALS_BUTTON_WORKING_COLOR	 						=	"GOALS_BUTTON_WORKING_COLOR";
	public static final String PREF_SUBGOALS_BUTTON_WORKING_COLOR	 					=	"SUBGOALS_BUTTON_WORKING_COLOR";
	public static final String PREF_FUNCS_BUTTON_WORKING_COLOR	 						=	"FUNCS_BUTTON_WORKING_COLOR";
	public static final String PREF_GOALS_BUTTON_UNPROVED_COLOR	 						=	"GOALS_BUTTON_UNPROVED_COLOR";
	public static final String PREF_SUBGOALS_BUTTON_UNPROVED_COLOR 						=	"SUBGOALS_BUTTON_UNPROVED_COLOR";
	public static final String PREF_FUNCS_BUTTON_UNPROVED_COLOR	 						=	"FUNCS_BUTTON_UNPROVED_COLOR";
	public static final String PREF_GOALS_ASSISTANT_BUTTON_BG_COLOR 					=	"GOALS_ASSISTANT_BUTTON_BG_COLOR";
	public static final String PREF_SUBGOALS_ASSISTANT_BUTTON_BG_COLOR					=	"SUBGOALS_ASSISTANT_BUTTON_BG_COLOR";
	public static final String PREF_FUNCS_ASSISTANT_BUTTON_BG_COLOR 					=	"FUNCS_ASSISTANT_BUTTON_BG_COLOR";
	public static final String PREF_MARKED_GOAL_TEXT_COLOR								=	"MARKED_GOAL_TEXT_COLOR";
	public static final String PREF_MARKED_FUNC_TEXT_COLOR								=	"MARKED_FUNC_TEXT_COLOR";
	public static final String PREF_SHOW_NB_LINES            							=	"SHOW_NB_LINES";
	public static final String PREF_SHOW_ALL_LINES           							=	"SHOW_ALL_LINES";
	public static final String PREF_LIST_OF_COMMANDS         							=	"LIST_OF_COMMANDS";
	public static final String PREF_LIST_OF_PROOF_COMMANDS								=	"LIST_OF_PROOF_COMMANDS";
	public static final String PREF_LIST_OF_STATUS										=	"LIST_OF_STATUS";
	public static final String PREF_LIST_OF_RECOGNIZED_FILES 							=	"LIST_OF_RECOGNIZED_FILES";
	public static final String PREF_CLEAN_FILES1										=	"CLEAN_FILES1";
	public static final String PREF_RUN_OPTIONS											=	"RUN_OPTIONS";
	public static final String PREF_RUN_OPTIONS_NORMAL_MODE								=	"RUN_OPTIONS_NORMAL_MODE";
	public static final String PREF_RUN_OPTIONS_ADVANCED_MODE							=	"RUN_OPTIONS_ADVANCED_MODE";
	public static final String PREF_DTD_USING_FILE										=	"DTD_USING_FILE";
	public static final String PREF_DTD_FILE_LOCATION									=	"DTD_FILE_LOCATION";

	
	
	
	// I] PREFERENCES DEFAULT VALUES
	
	//of provers status
	public static final String PREF_LIST_OF_STATUS_DEFAULT_VALUE						=
		"prover" 	+	ELEMENT_SEPARATOR +
		"prover" 	+	ELEMENT_SEPARATOR +
		"prover" 	+	ELEMENT_SEPARATOR +
		"assistant"	+	ELEMENT_SEPARATOR
	;
	
	public static final String dp = DP.get(); //of dp and others commands for provers
	public static final String PREF_LIST_OF_PROOF_COMMANDS_DEFAULT_VALUE				=
		"prover"																	+ ELEMENT_SEPARATOR +
		"Simplify"																	+ ELEMENT_SEPARATOR +
		"why --simplify -dir simplify -no-prelude why/%s_ctx.why why/%s_po%n.why"	+ ELEMENT_SEPARATOR	+
		dp + " simplify/%s_po%n_why.sx"												+ ELEMENT_SEPARATOR	+ LINE_SEPARATOR +
		"prover"																	+ ELEMENT_SEPARATOR +
		"Alt-Ergo"																	+ ELEMENT_SEPARATOR +
		"why --why -dir ergo -no-prelude why/%s_ctx.why why/%s_po%n.why"			+ ELEMENT_SEPARATOR	+
		dp + " ergo/%s_po%n_why.why"												+ ELEMENT_SEPARATOR	+ LINE_SEPARATOR +
		"prover"																	+ ELEMENT_SEPARATOR +
		"Yices"																		+ ELEMENT_SEPARATOR +
		"why --smtlib -dir yices -no-prelude why/%s_ctx.why why/%s_po%n.why" 		+ ELEMENT_SEPARATOR	+
		dp + " yices/%s_po%n_why.smt"												+ ELEMENT_SEPARATOR	+ LINE_SEPARATOR +
		"assistant"																	+ ELEMENT_SEPARATOR +
		"CoqIDE"																	+ ELEMENT_SEPARATOR +
		"make -f %s.makefile coq/%s_ctx_why.vo"										+ ELEMENT_SEPARATOR	+
		"make -f %s.makefile coq/%s_po%n_why.v"										+ ELEMENT_SEPARATOR	+
		"coqc -I coq coq/%s_po%n_why.v"												+ ELEMENT_SEPARATOR	+
		"coqide -I coq coq/%s_po%n_why.v"											+ ELEMENT_SEPARATOR	+ LINE_SEPARATOR
	;
	
	//of verification tools commands
	public static final String PREF_LIST_OF_COMMANDS_DEFAULT_VALUE						=
		"c"																								+ LINE_SEPARATOR +
		"frama-c -jessie-analysis -jc-opt -separation %s.c"													+ ELEMENT_SEPARATOR + LINE_SEPARATOR +
		"make -C %s.jessie -f %s.makefile goals"													+ ELEMENT_SEPARATOR + LINE_SEPARATOR +
		"java"																							+ LINE_SEPARATOR +
		"krakatoa %s.java"															+ ELEMENT_SEPARATOR + LINE_SEPARATOR +
		"jessie -locs %s.jloc %s.jc"												+ ELEMENT_SEPARATOR + LINE_SEPARATOR +
		"make -f %s.makefile goals"													+ ELEMENT_SEPARATOR + LINE_SEPARATOR +
		"jc"																							+ LINE_SEPARATOR +
		"jessie -locs %s.jloc %s.jc"												+ ELEMENT_SEPARATOR + LINE_SEPARATOR +
		"make -f %s.makefile goals"													+ ELEMENT_SEPARATOR + LINE_SEPARATOR
	;
	
	//of recognized types of files
	public static final String PREF_LIST_OF_RECOGNIZED_FILES_DEFAULT_VALUE  			=
		"c" + ELEMENT_SEPARATOR + "java" + ELEMENT_SEPARATOR + "jc";
	
	
	//of PO Viewer background Color
	public static final RGB     PREF_POV_BACKGROUND_COLOR_DEFAULT_VALUE      	  		=   new RGB( 253, 245, 230 );
	
	
	//of cleaning of files why/xxx.why
	public static final boolean PREF_CLEAN_FILES1_DEFAULT_VALUE							=	true;
	
	
	
	// 	II] PREFERENCES DEFAULT VALUES : PROVER VIEW
	
	
	//prover column : goal, subgoal and function buttons color
	public static final RGB     PREF_GOALS_BUTTON_BG_COLOR_DEFAULT_VALUE          		=   new RGB(180, 180, 180) /*new RGB(0, 0, 179)*/;
	public static final RGB     PREF_SUBGOALS_BUTTON_BG_COLOR_DEFAULT_VALUE          	=   new RGB(255, 255, 255) /*new RGB(46, 152, 212)*/;
	public static final RGB     PREF_FUNCS_BUTTON_BG_COLOR_DEFAULT_VALUE   		  		=   new RGB(25, 25, 25) /*new RGB(0, 0, 98)*/;
	
	//proved, unproved and working colors for subgoals buttons
	public static final RGB		PREF_SUBGOALS_BUTTON_PROVED_COLOR_DEFAULT_VALUE      	=   new RGB(194, 246, 72);
	public static final RGB		PREF_SUBGOALS_BUTTON_WORKING_COLOR_DEFAULT_VALUE     	=   new RGB(255, 228, 0);
	public static final RGB		PREF_SUBGOALS_BUTTON_UNPROVED_COLOR_DEFAULT_VALUE    	=   new RGB(255, 85, 85);
	
	//proved, unproved and working colors for goals buttons
	public static final RGB		PREF_GOALS_BUTTON_PROVED_COLOR_DEFAULT_VALUE      		=   new RGB( 0, 215, 65 );
	public static final RGB		PREF_GOALS_BUTTON_WORKING_COLOR_DEFAULT_VALUE     		=   new RGB( 255, 177, 0 );
	public static final RGB		PREF_GOALS_BUTTON_UNPROVED_COLOR_DEFAULT_VALUE    		=   new RGB(211, 0, 38);
	
	//proved, unproved and working colors for function buttons
	public static final RGB		PREF_FUNCS_BUTTON_PROVED_COLOR_DEFAULT_VALUE      		=   new RGB(0, 145, 25);
	public static final RGB		PREF_FUNCS_BUTTON_WORKING_COLOR_DEFAULT_VALUE 			=   new RGB(212, 112, 0);
	public static final RGB		PREF_FUNCS_BUTTON_UNPROVED_COLOR_DEFAULT_VALUE    		=   new RGB(150, 0, 0);
	
	//assistant column : goal, subgoal and function buttons color
	public static final RGB		PREF_GOALS_ASSISTANT_BUTTON_BG_COLOR_DEFAULT_VALUE 		=   new RGB(180, 180, 180) /*new RGB(77, 77, 77)*/;
	public static final RGB		PREF_SUBGOALS_ASSISTANT_BUTTON_BG_COLOR_DEFAULT_VALUE 	=   new RGB(255, 255, 255) /*new RGB(255, 255, 255)*/;
	public static final RGB		PREF_FUNCS_ASSISTANT_BUTTON_BG_COLOR_DEFAULT_VALUE		=   new RGB(25, 25, 25) /*new RGB(0, 0, 0)*/;
	
	//marked goal and function text color 
	public static final RGB		PREF_MARKED_GOAL_TEXT_COLOR_DEFAULT_VALUE				=	new RGB(255, 118, 0);
	public static final RGB		PREF_MARKED_FUNC_TEXT_COLOR_DEFAULT_VALUE				=	new RGB(237, 0, 38);
	
	//Parameter for nb lines to show
	public static final int     PREF_SHOW_NB_LINES_DEFAULT_VALUE            			=   25;
	public static final int     PREF_SHOW_NB_LINES_MAX			           				=   150;
	public static final boolean PREF_SHOW_ALL_LINES_DEFAULT_VALUE           			=   true;
	
	//dtd file location for load and save functions
	public static final boolean PREF_DTD_USING_FILE_DEFAULT_VALUE						=	true;
	public static final String  PREF_DTD_FILE_LOCATION_DEFAULT_VALUE					=	"/usr/local/lib/why/why.dtd";
	
}

// **********************************************************************
// 
// <copyright>
// 
//  BBN Technologies, a Verizon Company
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
// 
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
// 
// </copyright>
// **********************************************************************
// 
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/plugin/esri/EsriPlugIn.java,v $
// $RCSfile: EsriPlugIn.java,v $
// $Revision: 1.1.1.1 $
// $Date: 2003/02/14 21:35:49 $
// $Author: dietrick $
// 
// **********************************************************************


package com.bbn.openmap.plugin.esri;

import com.bbn.openmap.Layer;
import com.bbn.openmap.dataAccess.shape.*;
import com.bbn.openmap.dataAccess.shape.*;
import com.bbn.openmap.dataAccess.shape.input.*;
import com.bbn.openmap.dataAccess.shape.output.*;
import com.bbn.openmap.event.ProjectionEvent;
import com.bbn.openmap.layer.util.LayerUtils;
import com.bbn.openmap.omGraphics.*;
import com.bbn.openmap.plugin.*;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.util.Debug;
import com.bbn.openmap.util.PropUtils;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

/**
 * EsriPlugIn loads Esri shape file sets from web servers or local
 * file systems, and it enables the creation of shape file sets.  It
 * needs to be inserted into a PlugInLayer to use within OpenMap.
 * <P>
 * To create a file from a remote location:
 * <code><pre>
 *
 *   URL shp = new URL("http://www.webserver.com/file.shp");
 *   URL dbf = new URL("http://www.webserver.com/file.dbf");
 *   URL shx = new URL("http://www.webserver.com/file.shx");
 *   EsriPlugIn epi = new EsriPlugIn("name", dbf, shp, shx);
 *   PlugInLayer pil = new PlugInLayer();
 *   pil.setPlugIn(epi);
 *
 * </pre></code>
 *
 * To open a shape file set from the local file system:
 * <code><pre>
 *
 *   File dbf = new File("c:/data/file.dbf");
 *   File shp = new File("c:/data/file.shp");
 *   File shx = new File("c:/data/file.shx");
 *   EsriPlugIn epi = new EsriPlugIn("name", dbf.toURL(), shp.toURL(), shx.toURL());
 *   PlugInLayer pil = new PlugInLayer();
 *   pil.setPlugIn(epi);
 *
 * </pre></code>
 *
 * To create a zero content shape file set from which the user can add
 * shapes at runtime:
 * <code><pre>
 *
 *   EsriPlugIn epi = new EsriPlugIn("name", EsriLayer.TYPE_POLYLINE);
 *
 * </pre></code>
 *
 * To add features to an EsriLayer:
 * <code><pre>
 *
 *   OMGraphicList shapeData = new OMGraphicList();
 *   ArrayList tabularData = new ArrayList();
 *   float[] part0 = new float[]{35.0f, -120.0f, -25.0f, -95.0f, 56.0f, -30.0f};
 *   float[] part1 = new float[]{-15.0f, -110.0f, 13.0f, -80.0f, -25.0f, 10.0f};
 *   OMPoly poly0 = new OMPoly(part0, OMGraphic.DECIMAL_DEGREES, OMGraphic.LINETYPE_RHUMB);
 *   OMPoly poly1 = new OMPoly(part1, OMGraphic.DECIMAL_DEGREES, OMGraphic.LINETYPE_RHUMB);
 *   shapeData.add(poly0);  //part 1
 *   shapeData.add(poly1);  //part 2
 *   shapeData.generate(_mapBean.getProjection());
 *   tabularData.add(0, "a value");
 *   plugin.addRecord(shapeData, tabularData);
 *   plugin.repaint(); // assumes that plugin added to PlugInLayer
 *
 * </pre></code>
 *
 * To configure an EsriLayer through a properties file, specify file
 * references in terms of URLs, full or relative file paths.
 *
 * To reference a file on Windows 2000:
 * <code><pre>
 *
 *   esri.class = com.bbn.openmap.plugin.esri.EsriPlugIn
 *   esri.prettyName = Esri Example
 *   esri.shp = file:///c:/data/shapefile.shp
 * # -or-
 *   esri.shp = c:/data/shapefile.shp
 *
 *   esri.dbf = file:///c:/data/shapefile.dbf
 *   esri.shx = file:///c:/data/shapefile.shx
 *
 * </pre></code>
 *
 * To reference a file on RedHat Linux 6.2:
 * <code><pre>
 *
 *   esri.class = com.bbn.openmap.plugin.esri.EsriPlugIn
 *   esri.prettyName = Esri Example
 *   esri.shp = file:///home/dvanauke/resources/shapefile.shp
 * # - or -
 *   esri.shp = /home/dvanauke/resources/shapefile.shp
 *
 *   esri.dbf = file:///home/dvanauke/resources/shapefile.dbf
 *   esri.shx = file:///home/dvanauke/resources/shapefile.shx
 *
 * </pre></code>
 *
 * To reference a file on a web server:
 * <code><pre>
 *
 *   esri.class = com.bbn.openmap.plugin.esri.EsriPlugIn
 *   esri.prettyName = Esri Example
 *   esri.shp = http://www.webserver.com/shapefile.shp
 *   esri.dbf = http://www.webserver.com/shapefile.dbf
 *   esri.shx = http://www.webserver.com/shapefile.shx
 *
 * </pre></code>
 *
 * The PlugIn has been updated to use the properties from the
 * DrawingAttributes object in order to specify how it's objects
 * should be rendered:
 * <code><pre>
 *
 *   esri.class = com.bbn.openmap.plugin.esri.EsriPlugIn
 *   esri.prettyName = Esri Example
 *   esri.lineColor = AARRGGBB (hex ARGB color)
 *   esri.fillColor = AARRGGBB (hex ARGB color)
 *   esri.selectColor = AARRGGBB (hex ARGB color)
 *   esri.lineWidth = AARRGGBB (hex ARGB color)
 *
 * </pre></code>
 *
 * See DrawingAttributes for more options.  Also, as of OpenMap 4.5.4,
 * you don't have to specify the location of the .dbf and .shx files.
 * If you don't, the plugin assumes that those files are next to the
 * .shp file.
 *
 * @author Doug Van Auken 
 * @author Don Dietrick 
 * @author Lonnie Goad from OptiMetrics provided selection bug
 * solution and GUI interaction.
 */
public class EsriPlugIn extends AbstractPlugIn implements ShapeConstants {

    private EsriGraphicList _list = null;
    private DbfTableModel _model = null;
    private JFrame _tableFrame = null;
    private JScrollPane _pane = null;
    private int _type = -1;

    private String dbf;
    private String shx;
    private String shp;
    
    /** The last projection. */
    protected Projection proj;

    protected DrawingAttributes drawingAttributes = 
            DrawingAttributes.getDefaultClone();

    /**
     * Creates an EsriPlugIn that will be configured through the
     * <code>setProperties()</code> method
     */
    public EsriPlugIn() {
	Debug.message("esri", "EsriPlugIn: default constructor");
    }
    
    /**
     * Creates an empty EsriPlugIn, useable for adding features at
     * run-time.
     * @param name The name of the layer
     * @param type The type of layer
     * @param columnCount The number of columns in the dbf model 
     */
    public EsriPlugIn(String name, int type, int columnCount) 
	throws Exception {

	switch (type) {
	case SHAPE_TYPE_POINT:
	    _list = new EsriPointList();
	    break;
	case SHAPE_TYPE_POLYGON:
	    _list = new EsriPolygonList();
	    break;
	case SHAPE_TYPE_POLYLINE:
	    _list = new EsriPolylineList();
	    break;
	default:
	    _list = null;
	}

	_model = new DbfTableModel(columnCount);
	this.name = name;
    }
    
    /**
     * Creates an EsriPlugIn from a set of shape files
     * @param label The name of the layer that may be used to
     * reference the layer
     * @param dbf The url referencing the dbf extension file
     * @param shp The url referencing the shp extension file
     * @param shx The url referencing the shx extension file 
     */
    public EsriPlugIn(String name, URL dbf, URL shp, URL shx) {

	this.dbf = dbf.toString();
	this.shp = shp.toString();
	this.shx = shx.toString();

	_list = getGeometry(shp, shx);
	_model = getDbfTableModel(dbf);
	this.name = name;
    }

    /**
     * Overridden to check to see if the parent component is a layer,
     * and if it is, set the pretty name to the plugin's name.
     */
    public void setComponent(Component comp) {
	super.setComponent(comp);
    }

    /**
     * Set the name of the plugin.  If the parent component is a
     * layer, set its pretty name as well.
     */
    public void setName(String name) {
	this.name = name;
	Component comp = getComponent();
	if (comp instanceof Layer) {
	    ((Layer)comp).setName(name);
	}
    }

    /**
     * Get the name of the plugin.  If the parent component is a
     * layer, get the pretty name from it instead.
     */
    public String getName() {
	Component comp = getComponent();
	if (comp instanceof Layer) {
	    return ((Layer)comp).getName();
	}
	return name;
    }
    
    /**
     * Set the drawing attributes for the graphics on the list.
     */
    public void setDrawingAttributes(DrawingAttributes da) {
	drawingAttributes = da;

	if (_list != null) {
	    drawingAttributes.setTo(_list);
	}
    }

    /**
     * Get the drawing attributes for the graphics on the list.
     */
    public DrawingAttributes getDrawingAttributes() {
	return drawingAttributes;
    }

    /**
     * Handles adding records to the geometry list and the
     * DbfTableModel.
     * @param graphic An OMGraphic to add the graphics list
     * @param record A record to add to the DbfTableModel 
     */
    public void addRecord(OMGraphic graphic, ArrayList record) {
	OMGraphicList list = getEsriGraphicList();

	// If list == null, model will be too.
	if (list != null) {
	    list.add(graphic);
	    _model.addRecord(record);
	} else {
	    Debug.error("EsriPlugIn.addRecord(): invalid data files!");
	}
    }
    
    /**
     * Creates a DbfTableModel for a given .dbf file
     * @param dbf The url of the file to retrieve.
     * @return The DbfTableModel for this layer, null if something
     * went badly.
     */
    private DbfTableModel getDbfTableModel(URL dbf) {
	return DbfTableModel.getDbfTableModel(dbf);
    }
    
    /**
     * Returns the EsriGraphicList for this layer
     * @return The EsriGraphicList for this layer
     */
    public EsriGraphicList getEsriGraphicList() {
	if (_list == null) {
	    try {
// 		_model = getDbfTableModel(new URL(dbf));
// 		_list = getGeometry(new URL(shp), new URL(shx));

		// Changed so that shp, dbf and shx can be named as
		// resource, a file path, or a URL.  Also, if the dbf
		// and shx file are not provided, look for them next
		// to the shape file. - DFD

		if (shx == null && shp != null) {
		    shx = shp.substring(0, shp.lastIndexOf('.') + 1) + PARAM_SHX;
		}

		if (dbf == null && shp != null) {
		    dbf = shp.substring(0, shp.lastIndexOf('.') + 1) + PARAM_DBF;
		}

		_model = getDbfTableModel(LayerUtils.getResourceOrFileOrURL(null, dbf));
		_list = getGeometry(LayerUtils.getResourceOrFileOrURL(null, shp), 
				    LayerUtils.getResourceOrFileOrURL(null, shx));

		if (_model != null) {
  		    DrawingAttributesUtility.setDrawingAttributes(_list, _model, getDrawingAttributes());
		}
	    } catch (MalformedURLException murle) {
		Debug.error("EsriPlugIn|" + getName() + " Malformed URL Exception\n" + murle.getMessage());
	    } catch (Exception exception) {
		Debug.error("EsriPlugIn|" + getName() + " Exception\n" + exception.getMessage());
		exception.printStackTrace();
	    }
	}

	return _list;
    }

    public static void main(String[] argv) {
	if (argv.length == 0) {
	    System.out.println("Give EsriPlugIn a path to a shape file, and it'll print out the graphics.");
	    System.exit(0);
	}

	Debug.init();

	EsriPlugIn epi = new EsriPlugIn();
	Properties props = new Properties();
	props.put(PARAM_SHP, argv[0]);
	epi.setProperties(props);
	
	OMGraphicList list = epi.getEsriGraphicList();

	String dbfFileName = argv[0].substring(0, argv[0].lastIndexOf('.') + 1)+ "dbf";

	try {
	    list.setAppObject(epi.getDbfTableModel(LayerUtils.getResourceOrFileOrURL(epi, dbfFileName)));
	    Debug.output("Set list in table");

	} catch (Exception e) {
	    Debug.error("Can't read .dbf file for .shp file: " + dbfFileName + "\n" + e.getMessage());
	    System.exit(0);
	}
	
	EsriShapeExport ese = new EsriShapeExport(list, null, "/Users/dietrick/Desktop/ese");
	Debug.output("Exporting...");
	ese.export();
	Debug.output("Done.");
	System.exit(0);
    }

    /**
     * The getRectangle call is the main call into the PlugIn module.
     * The module is expected to fill a graphics list with objects
     * that are within the screen parameters passed. It's assumed that
     * the PlugIn will call generate(projection) on the OMGraphics
     * returned!  If you don't call generate on the OMGraphics, they
     * will not be displayed on the map.
     * 
     * @param p projection of the screen, holding scale, center
     * coords, height, width.  May be null if the parent component
     * hasn't been given a projection.
     */
    public OMGraphicList getRectangle(Projection p) {
	OMGraphicList list = getEsriGraphicList();
	proj = p;
	if (list != null) {
	    list.generate(p);
	}
	return list;
    }

    /*
     * Reads the contents of the SHX and SHP files.  The SHX file will
     * be read first by utilizing the ShapeIndex.open method.  This
     * method will return a list of offsets, which the
     * AbstractSupport.open method will use to iterate through the
     * contents of the SHP file.
     * @param sho The url of the SHP file
     * @param shx The url of the SHX file
     * @return A new EsriGraphicList, null if something went badly.
     */
    public EsriGraphicList getGeometry(URL shp, URL shx) {
	return EsriGraphicList.getEsriGraphicList(shp, shx, getDrawingAttributes(), null);
    }
    
    /**
     * Returns the associated table model for this layer
     * @return The associated table model for this layer
     */
    public DbfTableModel getModel() {
	return _model;
    }
    
    /**
     * Returns whether this layer is of type 0 (point), 3 (polyline),
     * or 5(polygon)
     * @return An int representing the type of layer, as specified in
     * Esri's shape file format specification 
     */
    public int getType() {
	return _type;
    }
    
    /**
     * Filters the DbfTableModel given a SQL like string
     * @param query A SQL like string to filter the DbfTableModel
     */
    public void query(String query) {
	//to be implemented
    }
    
    /**
     * Sets the DbfTableModel
     * @param DbfTableModel The DbfModel to set for this layer
     */
    public void setModel(DbfTableModel model) {
	if (_model != null) {
	    _model = model;
	}
    }
    
    /**
     * Sets the properties for the <code>Layer</code>.
     * @param prefix the token to prefix the property names
     * @param props the <code>Properties</code> object
     */
    public void setProperties(String prefix, Properties properties) {
	super.setProperties(prefix, properties);
	
	drawingAttributes.setProperties(prefix, properties);

	// This fixes a hole that was exposed when the PlugIn had the
	// files set directly, and then had properties set for drawing
	// attributes later.
	if (_list != null) {
	    if (_model != null) {
		DrawingAttributesUtility.setDrawingAttributes(_list, _model, drawingAttributes);
	    } else {
		drawingAttributes.setTo(_list);
	    }
	}

	prefix = PropUtils.getScopedPropertyPrefix(prefix);

	shp = properties.getProperty(prefix + PARAM_SHP);
	shx = properties.getProperty(prefix + PARAM_SHX);
	dbf = properties.getProperty(prefix + PARAM_DBF);

//  	try {
//  	    setName("testing");
//  	    _list = getGeometry(new URL(shp), new URL(shx));
//  	    _model = getDbfTableModel(new URL(dbf));
//  	} catch(Exception exception) {
//  	    System.out.println(exception);
//  	}
    }

    public Properties getProperties(Properties props) {
	props = super.getProperties(props);

	String prefix = PropUtils.getScopedPropertyPrefix(this);
	props.put(prefix + PARAM_SHP, PropUtils.unnull(shp));
	props.put(prefix + PARAM_SHX, PropUtils.unnull(shx));
	props.put(prefix + PARAM_DBF, PropUtils.unnull(dbf));

	drawingAttributes.setPropertyPrefix(getPropertyPrefix());
	drawingAttributes.getProperties(props);
	return props;
    }

    public Properties getPropertyInfo(Properties props) {
	props = super.getPropertyInfo(props);

	props.put(initPropertiesProperty, PARAM_SHP + " " + PARAM_DBF + " " + PARAM_SHX + drawingAttributes.getInitPropertiesOrder());

	props.put(PARAM_SHP, "Location of a shape (.shp) file (path or URL)");
	props.put(PARAM_SHX, "Location of a index file (.shx) for the shape file (path or URL, optional)");
	props.put(PARAM_DBF, "Location of a database file (.dbf) for the shape file (path or URL, optional)");
	props.put(PARAM_SHP + ScopedEditorProperty, 
		 "com.bbn.openmap.util.propertyEditor.FDUPropertyEditor");
	props.put(PARAM_DBF + ScopedEditorProperty, 
		 "com.bbn.openmap.util.propertyEditor.FDUPropertyEditor");
	props.put(PARAM_SHX + ScopedEditorProperty, 
		 "com.bbn.openmap.util.propertyEditor.FDUPropertyEditor");

	drawingAttributes.getPropertyInfo(props);

	return props;
    }

    public Component getGUI() {

	JPanel holder = new JPanel(new BorderLayout());

	JPanel daGUI = (JPanel)drawingAttributes.getGUI();
	holder.add(daGUI, BorderLayout.CENTER);

	JPanel btnPanel = new JPanel(new GridLayout(3, 1));

	JButton redrawSelected = new JButton("Set Colors for Selected");
	btnPanel.add(redrawSelected);

	JButton redrawAll = new JButton("Set Colors For All");
	btnPanel.add(redrawAll);

	JButton tableTrigger = new JButton("Show Data Table");
	btnPanel.add(tableTrigger);

	holder.add(btnPanel, BorderLayout.SOUTH);

	redrawSelected.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    if (!(graphicIndex < 0)) {
			OMGraphic omg = getEsriGraphicList().getOMGraphicAt(graphicIndex);
			repaintGraphics(omg);
		    }
		}
	    });

	redrawAll.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    repaintGraphics(getEsriGraphicList());
		}
	    });


	tableTrigger.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent ae) {
		    showTable();
		}
	    });
	return holder;
    }

    /**
     * Sets the drawing attributes to those of a particular OMGraphic.
     * Note: This is supposed to be able to be done with the
     * DrawingAttributes .setFrom(OMGraphic) method but problems with
     * that caused the creation of this 
     */
    private void setDrawingAttributes(OMGraphic omg) {
	drawingAttributes.setLinePaint(omg.getLinePaint());
	drawingAttributes.setFillPaint(omg.getFillPaint());
	drawingAttributes.setSelectPaint(omg.getSelectPaint());
	drawingAttributes.setStroke((BasicStroke)(omg.getStroke()));
    }

    /**
     * Repaints the currently selected OMGraphic or the OMGraphicList
     * to the current DrawingAttributes
     *
     * @param	omg		the OMGraphic to repaint
     */
    private void repaintGraphics(OMGraphic omg) {
	drawingAttributes.setTo(omg);
	repaint();
    }

    protected JTable table = null;
    protected ListSelectionModel lsm = null;

    /**
     * Needs to be called before displaying the DbfTableModel.
     */
    public JTable getTable() {

	if (table == null) {
	    lsm = new DefaultListSelectionModel();
	    table = new JTable();
	    table.setModel(getModel());
	    table.setSelectionModel(lsm);
	    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

	    lsm.addListSelectionListener(new ListSelectionListener() {
		    public void valueChanged(ListSelectionEvent e) {
			//Ignore extra messages.
			if (e.getValueIsAdjusting()) {
			    return;
			}
			ListSelectionModel lsm2 = (ListSelectionModel)e.getSource();
			if (lsm2.isSelectionEmpty()) {
			    //no rows are selected
			}
			else {
			    int index = lsm2.getMinSelectionIndex();
			    selectGraphic(index);
			    getComponent().repaint();
			}
		    }
		});
	}

	return table;
    }

    /**
     * Mark a graphic as selected on the map.
     * @param index the index, from 0, of the graphic on the list.
     */
    public void selectGraphic(int index) {
	EsriGraphicList list = getEsriGraphicList();
	list.deselectAll();
	selectGraphic(list.getOMGraphicAt(index));
	graphicIndex = index;
	list.regenerate(proj);
    }

    /**
     * Mark the graphic as selected, and generate if necessary.
     */
    public void selectGraphic(OMGraphic graphic) {
	if (graphic != null) {
	    graphic.select();
	    graphic.regenerate(proj);
	}
    }

    /**
     * Given a graphic, highlight its entry in the table.
     */
    public void selectEntry(OMGraphic graphic) {
//  	Object obj = graphic.getAppObject();

	if (lsm == null) {
	    getTable();
	}

	lsm.setSelectionInterval(graphicIndex, graphicIndex);
	//scroll to the appropriate row in the table
	getTable().scrollRectToVisible(getTable().getCellRect(graphicIndex, 0, true));

//  	if (obj != null) {
//  	    if (obj instanceof Integer) {
//  		int index = ((Integer)obj).intValue();
//  		lsm.setSelectionInterval(index-1, index-1);
//  		getTable().scrollRectToVisible(getTable().getCellRect(index, 0, true));
//  	    }
//  	} else {
//  	    lsm.clearSelection();
//  	}
    }
    
    /**
     * Show the table in its own frame.
     */
    public void showTable() {
	if (tableFrame == null) {
	    String tableTitle = (this.name != null) ? this.name : "";
	    tableFrame = new JFrame(tableTitle + " Shape Data Attributes");

	    JScrollPane pane = new JScrollPane(getTable(), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

	    tableFrame.getContentPane().add(pane, BorderLayout.CENTER);

	    tableFrame.setSize(400, 300);
	}

	tableFrame.setVisible(true);
	tableFrame.toFront();
    }

    /**
     * Handle a mouse click on the map.
     */
    public boolean mouseClicked(MouseEvent e) {
	EsriGraphicList list = getEsriGraphicList();
	boolean ret = false;
	graphicIndex = -1;

	if (list != null) {
	    OMGraphic omg = list.selectClosest(e.getX(), e.getY(), 4);
	    if (omg != null) {
		// graphicIndex has to be set before selectEntry called.
		graphicIndex = list.indexOf(omg);
		selectEntry(omg);

//  		Object obj = omg.getAppObject();
//  		if (obj instanceof String) {
//  		    if (component instanceof Layer) {
//  			((Layer)component).fireRequestInfoLine((String) obj);
//  		    }
//  		}

		ret = true;
	    } else {
		if (lsm == null) getTable();
		lsm.clearSelection();
		list.deselect();
		repaint();
	    }
	}
	return ret;
    }

    protected Layer parentLayer = null;

    /**
     * Handle mouse moved events (Used for firing tool tip descriptions over graphics)
     */
    public boolean mouseMoved(MouseEvent e) {
	EsriGraphicList list = getEsriGraphicList();
	boolean ret = false;
	if (list != null) {
	    OMGraphic omg = list.findClosest(e.getX(), e.getY(), 4);
	    if (omg != null) {
		int index = list.indexOf(omg);
		    
		if (parentLayer == null) {
		    Component comp = getComponent();
		    if (comp != null && comp instanceof Layer) {
			parentLayer = (Layer)comp;
		    }
		}

		if (parentLayer != null) {
		    parentLayer.fireRequestToolTip(e, getDescription(index));
		}

		ret = true;
	    } else if (parentLayer != null) {
		parentLayer.fireHideToolTip(e);
	    }
	}
	return ret;
    }

    /**
     * Builds a description in HTML for a tool tip for the specified OMGraphic
     *
     * @param	index	the index of the graphic in the table
     */
    public String getDescription(int index) {
	Vector v = new Vector();
	String description = "";

	v.add("<HTML><BODY>");
	for (int i = 0; i < getTable().getColumnCount(); i++) {
	    try {
		String column = getTable().getColumnName(i);
		String value = (String)(getTable().getValueAt(index, i) + "");
		v.add((i==0?"<b>":"<BR><b>") + column + ":</b> " + value);
	    } catch (NullPointerException npe) { 
	    } catch (IndexOutOfBoundsException obe) { 
	    }
	}

	v.add("</BODY></HTML>");

	for (int i = 0; i < v.size(); i++) {
	    description += (String)(v.elementAt(i));
	}

	return description;
    }

    protected JPanel daGUI = null;
    protected JFrame tableFrame = null;
    /** This marks the index of the OMGraphic that is "selected" */
    protected int graphicIndex = -1;
}
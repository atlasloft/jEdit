/*
 * PanelWindowContainer.java - holds dockable windows
 * Copyright (C) 2000, 2001 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.gui;

import javax.swing.border.*;
import javax.swing.plaf.metal.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;

/**
 * A container for dockable windows. This class should never be used
 * directly.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public class PanelWindowContainer extends JPanel implements DockableWindowContainer
{
	public PanelWindowContainer(DockableWindowManager wm, String position)
	{
		super(new BorderLayout());

		this.wm = wm;
		this.position = position;

		ResizeMouseHandler resizeMouseHandler = new ResizeMouseHandler();
		addMouseListener(resizeMouseHandler);
		addMouseMotionListener(resizeMouseHandler);

		buttons = new Box(
			(position.equals(DockableWindowManager.TOP)
			|| position.equals(DockableWindowManager.BOTTOM))
			? BoxLayout.X_AXIS
			: BoxLayout.Y_AXIS
		);

		closeBox = new JButton(CLOSE_BOX);
		closeBox.setMargin(new Insets(0,0,0,0));
		closeBox.setBorderPainted(false);
		closeBox.addActionListener(new ActionHandler());
		closeBox.addMouseListener(new ButtonMouseHandler());
		closeBox.setRequestFocusEnabled(false);
		buttons.add(closeBox);

		if(position.equals(DockableWindowManager.TOP))
			add(BorderLayout.NORTH,buttons);
		else if(position.equals(DockableWindowManager.LEFT))
			add(BorderLayout.EAST,buttons);
		else if(position.equals(DockableWindowManager.BOTTOM))
			add(BorderLayout.SOUTH,buttons);
		else if(position.equals(DockableWindowManager.RIGHT))
			add(BorderLayout.WEST,buttons);

		buttonGroup = new ButtonGroup();
		dockables = new Vector();

		dimension = jEdit.getIntegerProperty(
			"view.dock." + position + ".dimension",0);
	}

	public void saveDimension()
	{
		jEdit.setIntegerProperty("view.dock." + position + ".dimension",
			dimension);
	}

	public Dimension getMinimumSize()
	{
		return new Dimension(0,0);
	}

	public Dimension getPreferredSize()
	{
		if(dockables.size() == 0)
			return new Dimension(0,0);
		else
		{
			Dimension dim = buttons.getPreferredSize();
			if(current == null)
				return dim;
			else
			{
				if(position.equals(DockableWindowManager.TOP)
					|| position.equals(DockableWindowManager.BOTTOM))
					dim.height += dimension + SPLITTER_WIDTH;
				else
					dim.width += dimension + SPLITTER_WIDTH;
				return dim;
			}
		}
	}

	public void add(DockableWindowManager.Entry entry)
	{
		dockables.addElement(entry);

		int rotation;
		if(position.equals(DockableWindowManager.TOP)
			|| position.equals(DockableWindowManager.TOP))
			rotation = CustomButton.NONE;
		else if(position.equals(DockableWindowManager.LEFT))
			rotation = CustomButton.CCW;
		else if(position.equals(DockableWindowManager.RIGHT))
			rotation = CustomButton.CW;
		else
			throw new InternalError("Invalid position: " + position);

		CustomButton button = new CustomButton(rotation,entry.title);
		button.addMouseListener(new ButtonMouseHandler());
		buttonGroup.add(button);
		buttons.add(button);

		revalidate();
	}

	public void save(DockableWindowManager.Entry entry) {}

	public void remove(DockableWindowManager.Entry entry)
	{
		int index = dockables.indexOf(entry);
		buttons.remove(index + 1);

		dockables.removeElement(entry);
		remove(entry.win.getComponent());

		if(current == entry)
		{
			current = null;
			show(entry);
		}
		else
			revalidate();
	}

	public void show(DockableWindowManager.Entry entry)
	{
		if(current != null)
			remove(current.win.getComponent());
		else
		{
			// we didn't have a component previously, so create a border
			setBorder(new DockBorder(position));
		}

		if(entry != null)
		{
			add(entry.win.getComponent(),BorderLayout.CENTER);
			this.current = entry;

			int index = dockables.indexOf(entry);
			((JToggleButton)buttons.getComponent(index + 1)).setSelected(true);

			entry.win.getComponent().requestFocus();

			revalidate();
		}
		else
		{
			buttonGroup.setSelected(null,true);
			// removing last component, so remove border
			setBorder(null);
		}
	}

	public boolean isVisible(DockableWindowManager.Entry entry)
	{
		return current == entry;
	}

	// private members
	private static final Icon CLOSE_BOX
		 = GUIUtilities.loadIcon("closebox.gif");
	private static final int SPLITTER_WIDTH = 10;

	private DockableWindowManager wm;
	private String position;
	private Box buttons;
	private JButton closeBox;
	private ButtonGroup buttonGroup;
	private int dimension;
	private Vector dockables;
	private DockableWindowManager.Entry current;

	static class DockBorder implements Border
	{
		String position;
		Insets insets;
		Color color1;
		Color color2;
		Color color3;

		DockBorder(String position)
		{
			if(UIManager.getLookAndFeel() instanceof MetalLookAndFeel)
			{
				color1 = MetalLookAndFeel.getControlHighlight();
				color2 = MetalLookAndFeel.getControlDarkShadow();
				color3 = MetalLookAndFeel.getControl();
			}

			this.position = position;
			insets = new Insets(
				position.equals(DockableWindowManager.BOTTOM)
					? SPLITTER_WIDTH : 0,
				position.equals(DockableWindowManager.RIGHT)
					? SPLITTER_WIDTH : 0,
				position.equals(DockableWindowManager.TOP)
					? SPLITTER_WIDTH : 0,
				position.equals(DockableWindowManager.LEFT)
					? SPLITTER_WIDTH : 0);
		}

		public void paintBorder(Component c, Graphics g,
			int x, int y, int width, int height)
		{
			if(color1 == null || color2 == null || color3 == null)
				return;

			if(position.equals(DockableWindowManager.BOTTOM))
				paintHorizBorder(g,x,y,width);
			else if(position.equals(DockableWindowManager.RIGHT))
				paintVertBorder(g,x,y,height);
			else if(position.equals(DockableWindowManager.TOP))
			{
				paintHorizBorder(g,x,y + height
					- SPLITTER_WIDTH,width);
			}
			else if(position.equals(DockableWindowManager.LEFT))
			{
				paintVertBorder(g,x + width
					- SPLITTER_WIDTH,y,height);
			}
		}

		public Insets getBorderInsets(Component c)
		{
			return insets;
		}

		public boolean isBorderOpaque()
		{
			return false;
		}

		private void paintHorizBorder(Graphics g, int x, int y, int width)
		{
			g.setColor(color3);
			g.fillRect(x,y,width,SPLITTER_WIDTH);

			for(int i = 0; i < width / 4 - 1; i++)
			{
				g.setColor(color1);
				g.drawLine(x + i * 4 + 2,y + 3,
					x + i * 4 + 2,y + 3);
				g.setColor(color2);
				g.drawLine(x + i * 4 + 3,y + 4,
					x + i * 4 + 3,y + 4);
				g.setColor(color1);
				g.drawLine(x + i * 4 + 4,y + 5,
					x + i * 4 + 4,y + 5);
				g.setColor(color2);
				g.drawLine(x + i * 4 + 5,y + 6,
					x + i * 4 + 5,y + 6);
			}
		}

		private void paintVertBorder(Graphics g, int x, int y, int height)
		{
			g.setColor(color3);
			g.fillRect(x,y,SPLITTER_WIDTH,height);

			for(int i = 0; i < height / 4 - 1; i++)
			{
				g.setColor(color1);
				g.drawLine(x + 3,y + i * 4 + 2,
					x + 3,y + i * 4 + 2);
				g.setColor(color2);
				g.drawLine(x + 4,y + i * 4 + 3,
					x + 4,y + i * 4 + 3);
				g.setColor(color1);
				g.drawLine(x + 5,y + i * 4 + 4,
					x + 5,y + i * 4 + 4);
				g.setColor(color2);
				g.drawLine(x + 6,y + i * 4 + 5,
					x + 6,y + i * 4 + 5);
			}
		}
	}

	public static class CustomButton extends JToggleButton
	{
		static final int NONE = 0;
		static final int CW = 1;
		static final int CCW = 2;

		Color background = UIManager.getColor("Button.background");
		Color lighter = background.brighter();

		public CustomButton(int rotate, String text)
		{
			this();
			setIcon(new RotatedTextIcon(rotate,text));
		}

		public CustomButton()
		{
			setBorderPainted(false);
			setMargin(new Insets(0,0,0,0));
			setRequestFocusEnabled(false);
		}

		static int maxWidth = 0;
		static int maxHeight = 1;
		static Image rotImg;
		static Graphics rotGfx;

		class RotatedTextIcon implements Icon
		{
			int rotate;
			String text;
			int width;
			int height;

			RotatedTextIcon(int rotate, String text)
			{
				this.rotate = rotate;
				this.text = text;
				FontMetrics fm = getFontMetrics(getFont());
				width = fm.stringWidth(text);
				height = fm.getHeight();

				/* if(rotate == CW || rotate == CCW)
				{
					if(width > maxWidth
						|| height > maxHeight)
					{
						maxWidth = width;
						maxHeight = height;
						rotImg = null;
						rotGfx = null;
					}

					int tmp = width;
					width = height;
					height = tmp;
				} */
			}

			public int getIconWidth()
			{
				return width;
			}

			public int getIconHeight()
			{
				return height;
			}

			public void paintIcon(Component c, Graphics g, int x, int y)
			{
				FontMetrics fm = g.getFontMetrics();

				//if(rotate == NONE)
				{
					g.setColor(c.getForeground());
					g.drawString(text,x,y + fm.getAscent());
					return;
				}

				/*if(rotImg == null)
				{
					rotImg = c.createImage(maxWidth,maxHeight);
					rotGfx = rotImg.getGraphics();
				}

				rotGfx.setColor(c.getBackground());
				rotGfx.fillRect(0,0,width,height);
				rotGfx.setColor(c.getForeground());

				rotGfx.drawString(text,0,fm.getAscent());

				ImageFilter filter = new RotationFilter(rotate,width,height);
				Image rotated = createImage(new FilteredImageSource(
					rotImg.getSource(),filter));

				g.drawImage(rotated,x,y,c);*/
			}
		}

		static class RotationFilter extends ImageFilter
		{
			int width;
			int height;
			int rotation;

			RotationFilter(int width, int height, int rotation)
			{
				this.width = width;
				this.height = height;
				this.rotation = rotation;
			}

			public void setDimensions(int _width, int _height)
			{
				super.setDimensions(width,height);
			}

			// Fuck all the retards at Sun who made it impossible to
			// write polymorphic array-access code.

			public void setPixels(int x, int y, int w, int h,
				ColorModel model, byte pixels[], int off,
				int scansize)
			{
				byte[] retVal = new byte[width * height];

				for(int i = x; i < Math.max(x + w,width); i++)
				{
					for(int j = y; j < Math.max(y + h,height); j++)
					{
						retVal[(i - x) * height
							+ (rotation == CW
							? (j - y)
							: (width - j + w))]
							= pixels[(j - y) * scansize
								+ (i - x) + off];
					}
				}

				super.setPixels(x,y,w,h,model,pixels,off,scansize);
			}

			public void setPixels(int x, int y, int w, int h,
				ColorModel model, int pixels[], int off,
				int scansize)
			{
				int[] retVal = new int[width * height];

				for(int i = x; i < Math.max(x + w,width); i++)
				{
					for(int j = y; j < Math.max(y + h,height); j++)
					{
						retVal[(i - x) * height
							+ (rotation == CW
							? (j - y)
							: (width - j + w))]
							= pixels[(j - y) * scansize
								+ (i - x) + off];
					}
				}

				super.setPixels(x,y,w,h,model,pixels,off,scansize);
			}
		}
	}

	class ButtonMouseHandler extends MouseAdapter
	{
		Color background = UIManager.getColor("Button.background");
		Color lighter = background.brighter();

		public void mouseEntered(MouseEvent evt)
		{
			Component comp = (Component)evt.getSource();
			comp.setBackground(lighter);
		}

		public void mouseExited(MouseEvent evt)
		{
			Component comp = (Component)evt.getSource();
			comp.setBackground(background);
		}
	}

	class ResizeMouseHandler extends MouseAdapter implements MouseMotionListener
	{
		boolean canDrag;
		int dragStartDimension;
		Point dragStart;

		public void mousePressed(MouseEvent evt)
		{
			dragStartDimension = dimension;
			dragStart = evt.getPoint();
			dragStart.x = (getWidth() - dragStart.x);
			dragStart.y = (getHeight() - dragStart.y);
		}

		public void mouseMoved(MouseEvent evt)
		{
			Border border = getBorder();
			if(border == null)
			{
				// collapsed
				return;
			}

			Insets insets = border.getBorderInsets(PanelWindowContainer.this);
			int cursor = Cursor.DEFAULT_CURSOR;
			canDrag = false;
			if(position.equals(DockableWindowManager.TOP))
			{
				if(evt.getY() >= getHeight() - insets.bottom)
				{
					cursor = Cursor.N_RESIZE_CURSOR;
					canDrag = true;
				}
			}
			else if(position.equals(DockableWindowManager.LEFT))
			{
				if(evt.getX() >= getWidth() - insets.right)
				{
					cursor = Cursor.W_RESIZE_CURSOR;
					canDrag = true;
				}
			}
			else if(position.equals(DockableWindowManager.BOTTOM))
			{
				if(evt.getY() <= insets.top)
				{
					cursor = Cursor.S_RESIZE_CURSOR;
					canDrag = true;
				}
			}
			else if(position.equals(DockableWindowManager.RIGHT))
			{
				if(evt.getX() <= insets.left)
				{
					cursor = Cursor.E_RESIZE_CURSOR;
					canDrag = true;
				}
			}

			setCursor(Cursor.getPredefinedCursor(cursor));
		}

		public void mouseDragged(MouseEvent evt)
		{
			if(!canDrag)
				return;

			if(dragStart == null) // can't happen?
				return;

			if(position.equals(DockableWindowManager.TOP))
				dimension = evt.getY() + dragStart.y;
			else if(position.equals(DockableWindowManager.LEFT))
				dimension = evt.getX() + dragStart.x;
			else if(position.equals(DockableWindowManager.BOTTOM))
			{
				dimension = getHeight() - (/* dragStart.y
					- */ evt.getY());
			}
			else if(position.equals(DockableWindowManager.RIGHT))
			{
				dimension = getWidth() - (/* dragStart.x
					- */ evt.getX());
			}

			dimension = Math.max(SPLITTER_WIDTH,dimension);
			if(dimension == SPLITTER_WIDTH)
				dimension = dragStartDimension;

			revalidate();
		}

		public void mouseExited(MouseEvent evt)
		{
			setCursor(Cursor.getPredefinedCursor(
				Cursor.DEFAULT_CURSOR));
		}
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == closeBox)
				show(null);
		}
	}
}

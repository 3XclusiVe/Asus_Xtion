/****************************************************************************
*                                                                           *
*  OpenNI 1.x Alpha                                                         *
*  Copyright (C) 2011 PrimeSense Ltd.                                       *
*                                                                           *
*  This file is part of OpenNI.                                             *
*                                                                           *
*  OpenNI is free software: you can redistribute it and/or modify           *
*  it under the terms of the GNU Lesser General Public License as published *
*  by the Free Software Foundation, either version 3 of the License, or     *
*  (at your option) any later version.                                      *
*                                                                           *
*  OpenNI is distributed in the hope that it will be useful,                *
*  but WITHOUT ANY WARRANTY; without even the implied warranty of           *
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the             *
*  GNU Lesser General Public License for more details.                      *
*                                                                           *
*  You should have received a copy of the GNU Lesser General Public License *
*  along with OpenNI. If not, see <http://www.gnu.org/licenses/>.           *
*                                                                           *
****************************************************************************/
package org.OpenNI.Samples.UserTracker;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.OpenNI.Point3D;
import org.OpenNI.SkeletonJoint;

public class UserTrackerApplication {

    /**
	 * 
	 */
	public UserTracker viewer;
	private static boolean shouldRun = true;
	private JFrame frame;

    public UserTrackerApplication (JFrame frame)
    {
    	this.frame = frame;
    	frame.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent arg0) {}
			@Override
			public void keyReleased(KeyEvent arg0) {}
			@Override
			public void keyPressed(KeyEvent arg0) {
				if (arg0.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					shouldRun = false;
				}
			}
		});
    }

    public static GestureRecognizer GestureRecognizerComponent;
    
    public static void main(String s[])
    {
        JFrame f = new JFrame("OpenNI User Tracker");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });
        UserTrackerApplication app = new UserTrackerApplication(f);
        
        JFrame gestureFrame = new JFrame("Gesture recognizer");
        GestureRecognizerComponent = new GestureRecognizer();
        gestureFrame.add(GestureRecognizerComponent);
        gestureFrame.pack();
        gestureFrame.setVisible(true);
        gestureFrame.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				System.out.println("wefqwf");
				GestureRecognizerComponent.scanVectors();
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
        	
        }); 
        
        app.viewer = new UserTracker();
        f.add("Center", app.viewer);
        f.pack();
        f.setVisible(true);
        app.run();

    }

    void run()
    {
        while(shouldRun) {
            viewer.updateDepth();
            GestureRecognizerComponent.updateDepth();
            viewer.repaint();
            GestureRecognizerComponent.repaint();
        }
        frame.dispose();
    }
    
}

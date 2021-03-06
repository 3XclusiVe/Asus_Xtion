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

import org.OpenNI.*;

import java.nio.ShortBuffer;
import java.util.HashMap;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

public class GestureRecognizer extends Component
{
	class NewUserObserver implements IObserver<UserEventArgs>
	{
		@Override
		public void update(IObservable<UserEventArgs> observable,
				UserEventArgs args)
		{
			System.out.println("New user " + args.getId());
			try
			{
				if (skeletonCap.needPoseForCalibration())
				{
					poseDetectionCap.startPoseDetection(calibPose, args.getId());
				}
				else
				{
					skeletonCap.requestSkeletonCalibration(args.getId(), true);
				}
			} catch (StatusException e)
			{
				e.printStackTrace();
			}
		}
	}
	class LostUserObserver implements IObserver<UserEventArgs>
	{
		@Override
		public void update(IObservable<UserEventArgs> observable,
				UserEventArgs args)
		{
			System.out.println("Lost user " + args.getId());
			joints.remove(args.getId());
		}
	}
	
	class CalibrationCompleteObserver implements IObserver<CalibrationProgressEventArgs>
	{
		@Override
		public void update(IObservable<CalibrationProgressEventArgs> observable,
				CalibrationProgressEventArgs args)
		{
			System.out.println("Calibraion complete: " + args.getStatus());
			try
			{
			if (args.getStatus() == CalibrationProgressStatus.OK)
			{
				System.out.println("starting tracking "  +args.getUser());
					skeletonCap.startTracking(args.getUser());
	                joints.put(new Integer(args.getUser()), new HashMap<SkeletonJoint, SkeletonJointPosition>());
			}
			else if (args.getStatus() != CalibrationProgressStatus.MANUAL_ABORT)
			{
				if (skeletonCap.needPoseForCalibration())
				{
					poseDetectionCap.startPoseDetection(calibPose, args.getUser());
				}
				else
				{
					skeletonCap.requestSkeletonCalibration(args.getUser(), true);
				}
			}
			} catch (StatusException e)
			{
				e.printStackTrace();
			}
		}
	}
	class PoseDetectedObserver implements IObserver<PoseDetectionEventArgs>
	{
		@Override
		public void update(IObservable<PoseDetectionEventArgs> observable,
				PoseDetectionEventArgs args)
		{
			System.out.println("Pose " + args.getPose() + " detected for " + args.getUser());
			try
			{
				poseDetectionCap.stopPoseDetection(args.getUser());
				skeletonCap.requestSkeletonCalibration(args.getUser(), true);
			} catch (StatusException e)
			{
				e.printStackTrace();
			}
		}
	}
    /**
	 * 
	 */
	private Graphics g;
	private static final long serialVersionUID = 1L;
	private OutArg<ScriptNode> scriptNode;
    private Context context;
    private DepthGenerator depthGen;
    private UserGenerator userGen;
    private SkeletonCapability skeletonCap;
    private PoseDetectionCapability poseDetectionCap;
    private byte[] imgbytes;
    private float histogram[];
    String calibPose = null;
    HashMap<Integer, HashMap<SkeletonJoint, SkeletonJointPosition>> joints;

    private boolean drawBackground = true;
    private boolean drawPixels = true;
    private boolean drawSkeleton = true;
    private boolean printID = true;
    private boolean printState = true;
    
    
    private BufferedImage bimg;
    int width, height;
    
    private final String SAMPLE_XML_FILE = "SamplesConfig.xml";
    public GestureRecognizer()
    {

        try {
            scriptNode = new OutArg<ScriptNode>();
            context = Context.createFromXmlFile(SAMPLE_XML_FILE, scriptNode);

            depthGen = DepthGenerator.create(context);
            DepthMetaData depthMD = depthGen.getMetaData();

            histogram = new float[10000];
            width = depthMD.getFullXRes();
            height = depthMD.getFullYRes();
            
            imgbytes = new byte[width*height*3];

            userGen = UserGenerator.create(context);
            skeletonCap = userGen.getSkeletonCapability();
            poseDetectionCap = userGen.getPoseDetectionCapability();
            
            userGen.getNewUserEvent().addObserver(new NewUserObserver());
            userGen.getLostUserEvent().addObserver(new LostUserObserver());
            skeletonCap.getCalibrationCompleteEvent().addObserver(new CalibrationCompleteObserver());
            poseDetectionCap.getPoseDetectedEvent().addObserver(new PoseDetectedObserver());
            
            calibPose = skeletonCap.getSkeletonCalibrationPose();
            joints = new HashMap<Integer, HashMap<SkeletonJoint,SkeletonJointPosition>>();
            
            skeletonCap.setSkeletonProfile(SkeletonProfile.ALL);
			
			context.startGeneratingAll();
        } catch (GeneralException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private void calcHist(ShortBuffer depth)
    {
        // reset
        for (int i = 0; i < histogram.length; ++i)
            histogram[i] = 0;
        
        depth.rewind();

        int points = 0;
        while(depth.remaining() > 0)
        {
            short depthVal = depth.get();
            if (depthVal != 0)
            {
                histogram[depthVal]++;
                points++;
            }
        }
        
        for (int i = 1; i < histogram.length; i++)
        {
            histogram[i] += histogram[i-1];
        }

        if (points > 0)
        {
            for (int i = 1; i < histogram.length; i++)
            {
                histogram[i] = 1.0f - (histogram[i] / (float)points);
            }
        }
    }


    void updateDepth()
    {
        try {

            context.waitAnyUpdateAll();

            DepthMetaData depthMD = depthGen.getMetaData();
            SceneMetaData sceneMD = userGen.getUserPixels(0);

            ShortBuffer scene = sceneMD.getData().createShortBuffer();
            ShortBuffer depth = depthMD.getData().createShortBuffer();
            calcHist(depth);
            depth.rewind();
            
            while(depth.remaining() > 0)
            {
                int pos = depth.position();
                short pixel = depth.get();
                short user = scene.get();
                
        		imgbytes[3*pos] = 0;
        		imgbytes[3*pos+1] = 0;
        		imgbytes[3*pos+2] = 0;                	

                if (drawBackground || pixel != 0)
                {
                	int colorID = user % (colors.length-1);
                	if (user == 0)
                	{
                		colorID = colors.length-1;
                	}
                	if (pixel != 0)
                	{
                		float histValue = histogram[pixel];
                		imgbytes[3*pos] = (byte)(histValue*colors[colorID].getRed());
                		imgbytes[3*pos+1] = (byte)(histValue*colors[colorID].getGreen());
                		imgbytes[3*pos+2] = (byte)(histValue*colors[colorID].getBlue());
                	}
                }
            }
        } catch (GeneralException e) {
            e.printStackTrace();
        }
    }


    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    Color colors[] = {Color.RED, Color.BLUE, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.PINK, Color.YELLOW, Color.WHITE};
    public void getJoint(int user, SkeletonJoint joint) throws StatusException
    {
        SkeletonJointPosition pos = skeletonCap.getSkeletonJointPosition(user, joint);
		if (pos.getPosition().getZ() != 0)
		{
			joints.get(user).put(joint, new SkeletonJointPosition(depthGen.convertRealWorldToProjective(pos.getPosition()), pos.getConfidence()));
		}
		else
		{
			joints.get(user).put(joint, new SkeletonJointPosition(new Point3D(), 0));
		}
    }
    /*********************/
    public Point3D getSkeletonJointPosition(int user, SkeletonJoint joint)
    {
    		SkeletonJointPosition position = null;
    		Point3D realWorldPosition = null;
			try {
				position = skeletonCap.getSkeletonJointPosition(user, joint);
				realWorldPosition = depthGen.convertRealWorldToProjective(position.getPosition());
			} catch (StatusException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//drawCircle(realWorldPosition);
    		
    		return realWorldPosition;
    }
    
    public void drawCircle( Point3D point ) {
    	drawCenteredCircle(this.g, (int)point.getX(), (int)point.getY(), 10);
    }
    
    public void drawCenteredCircle(Graphics g2, int f, int h, int r) {
    	
    	  f = f-(r/2);
    	  h = h-(r/2);
    	  g2.fillOval(f,h,r,r);
    	  
    }
    
    /*********************/
    
    public void getJoints(int user) throws StatusException
    {
    	getJoint(user, SkeletonJoint.HEAD);
    	getJoint(user, SkeletonJoint.NECK);
    	
    	getJoint(user, SkeletonJoint.LEFT_SHOULDER);
    	getJoint(user, SkeletonJoint.LEFT_ELBOW);
    	getJoint(user, SkeletonJoint.LEFT_HAND);

    	getJoint(user, SkeletonJoint.RIGHT_SHOULDER);
    	getJoint(user, SkeletonJoint.RIGHT_ELBOW);
    	getJoint(user, SkeletonJoint.RIGHT_HAND);

    	getJoint(user, SkeletonJoint.TORSO);

    	getJoint(user, SkeletonJoint.LEFT_HIP);
        getJoint(user, SkeletonJoint.LEFT_KNEE);
        getJoint(user, SkeletonJoint.LEFT_FOOT);

    	getJoint(user, SkeletonJoint.RIGHT_HIP);
        getJoint(user, SkeletonJoint.RIGHT_KNEE);
        getJoint(user, SkeletonJoint.RIGHT_FOOT);

    }
    void drawLine(Graphics g, HashMap<SkeletonJoint, SkeletonJointPosition> jointHash, SkeletonJoint joint1, SkeletonJoint joint2)
    {
		Point3D pos1 = jointHash.get(joint1).getPosition();
		Point3D pos2 = jointHash.get(joint2).getPosition();

		if (jointHash.get(joint1).getConfidence() == 0 || jointHash.get(joint2).getConfidence() == 0)
			return;

		//g.drawLine((int)pos1.getX(), (int)pos1.getY(), (int)pos2.getX(), (int)pos2.getY());
    }
    public void drawSkeleton(Graphics g, int user) throws StatusException
    {
    	getJoints(user);
    	HashMap<SkeletonJoint, SkeletonJointPosition> dict = joints.get(new Integer(user));

    	drawLine(g, dict, SkeletonJoint.HEAD, SkeletonJoint.NECK);

    	drawLine(g, dict, SkeletonJoint.LEFT_SHOULDER, SkeletonJoint.TORSO);
    	drawLine(g, dict, SkeletonJoint.RIGHT_SHOULDER, SkeletonJoint.TORSO);

    	drawLine(g, dict, SkeletonJoint.NECK, SkeletonJoint.LEFT_SHOULDER);
    	drawLine(g, dict, SkeletonJoint.LEFT_SHOULDER, SkeletonJoint.LEFT_ELBOW);
    	drawLine(g, dict, SkeletonJoint.LEFT_ELBOW, SkeletonJoint.LEFT_HAND);

    	drawLine(g, dict, SkeletonJoint.NECK, SkeletonJoint.RIGHT_SHOULDER);
    	drawLine(g, dict, SkeletonJoint.RIGHT_SHOULDER, SkeletonJoint.RIGHT_ELBOW);
    	drawLine(g, dict, SkeletonJoint.RIGHT_ELBOW, SkeletonJoint.RIGHT_HAND);

    	drawLine(g, dict, SkeletonJoint.LEFT_HIP, SkeletonJoint.TORSO);
    	drawLine(g, dict, SkeletonJoint.RIGHT_HIP, SkeletonJoint.TORSO);
    	drawLine(g, dict, SkeletonJoint.LEFT_HIP, SkeletonJoint.RIGHT_HIP);

    	drawLine(g, dict, SkeletonJoint.LEFT_HIP, SkeletonJoint.LEFT_KNEE);
    	drawLine(g, dict, SkeletonJoint.LEFT_KNEE, SkeletonJoint.LEFT_FOOT);

    	drawLine(g, dict, SkeletonJoint.RIGHT_HIP, SkeletonJoint.RIGHT_KNEE);
    	drawLine(g, dict, SkeletonJoint.RIGHT_KNEE, SkeletonJoint.RIGHT_FOOT);
    	
    	
    	   Point3D head = getSkeletonJointPosition(1, SkeletonJoint.HEAD);
       	Point3D neck = this.getSkeletonJointPosition(1, SkeletonJoint.NECK);
       	Point3D LEFT_SHOULDER = this.getSkeletonJointPosition(1, SkeletonJoint.LEFT_SHOULDER);
       	Point3D LEFT_ELBOW = this.getSkeletonJointPosition(1, SkeletonJoint.LEFT_ELBOW);
       	Point3D LEFT_HAND = this.getSkeletonJointPosition(1, SkeletonJoint.LEFT_HAND);
       	Point3D RIGHT_SHOULDER = this.getSkeletonJointPosition(1, SkeletonJoint.RIGHT_SHOULDER);

       	Point3D RIGHT_ELBOW = this.getSkeletonJointPosition(1, SkeletonJoint.RIGHT_ELBOW);
       	Point3D RIGHT_HAND = this.getSkeletonJointPosition(1, SkeletonJoint.RIGHT_HAND);
       	
       	Point3D TORSO = this.getSkeletonJointPosition(1, SkeletonJoint.TORSO);

       	Point3D LEFT_HIP = this.getSkeletonJointPosition(1, SkeletonJoint.LEFT_HIP);
       	Point3D LEFT_KNEE = this.getSkeletonJointPosition(1, SkeletonJoint.LEFT_KNEE);
       	Point3D LEFT_FOOT = this.getSkeletonJointPosition(1, SkeletonJoint.LEFT_FOOT);
       	
       	Point3D RIGHT_HIP = this.getSkeletonJointPosition(1, SkeletonJoint.RIGHT_HIP);

       	Point3D RIGHT_KNEE = this.getSkeletonJointPosition(1, SkeletonJoint.RIGHT_KNEE);

       	Point3D RIGHT_FOOT = this.getSkeletonJointPosition(1, SkeletonJoint.RIGHT_FOOT);
       	
    	/** ___________________________________ **/
       	
       	int StartX = 150;
       	int StartY = 100;
       	
       	int curX = StartX;
       	int curY = StartY;
       	
       	neckVector = normalization(new Vector3D(head, neck));
       	leftShoulder = normalization(new Vector3D(neck, LEFT_SHOULDER));
       	leftElbow = normalization(new Vector3D(LEFT_SHOULDER, LEFT_ELBOW));
       	leftHand = normalization(new Vector3D(LEFT_ELBOW, LEFT_HAND));
       	
       	rightShoulder = normalization(new Vector3D(neck, RIGHT_SHOULDER));
       	rightElbow = normalization(new Vector3D(RIGHT_SHOULDER, RIGHT_ELBOW));
       	rightHand = normalization(new Vector3D(RIGHT_ELBOW, RIGHT_HAND));
       	
       	leftWing = normalization(new Vector3D(LEFT_SHOULDER, TORSO));
       	 rightSide = normalization(new Vector3D(TORSO, RIGHT_HIP));
       	 rightKnee = normalization(new Vector3D(RIGHT_HIP, RIGHT_KNEE));
       	 footKnee = normalization(new Vector3D(RIGHT_KNEE, RIGHT_FOOT));
       	
       	 rightWing = normalization(new Vector3D(RIGHT_SHOULDER, TORSO));
       	 leftSide = normalization(new Vector3D(TORSO, LEFT_HIP));
       	 leftKnee = normalization(new Vector3D(LEFT_HIP, LEFT_KNEE));
       	 leftFoot = normalization(new Vector3D(LEFT_KNEE, LEFT_FOOT));
       	
    	/** ___________________________________ **/
       	
       	g.drawLine(curX, curY, curX - (int)neckVector.X(), curY - (int)neckVector.Y());
       	
       	g.drawLine(curX, curY, curX + (int)leftShoulder.X(), curY + (int)leftShoulder.Y());
       	curX = curX + (int)leftShoulder.X();
       	curY = curY + (int)leftShoulder.Y();
       	
       	g.drawLine(curX, curY, curX + (int)leftElbow.X(), curY + (int)leftElbow.Y());
       	curX = curX + (int)leftElbow.X();
       	curY = curY + (int)leftElbow.Y();
       	
       	g.drawLine(curX, curY, curX + (int)leftHand.X(), curY + (int)leftHand.Y());
       	curX = StartX;
       	curY = StartY;
       	
       	/** ___________________________________ **/
       	
       	g.drawLine(curX, curY, curX + (int)rightShoulder.X(), curY + (int)rightShoulder.Y());
       	curX = curX + (int)rightShoulder.X();
       	curY = curY + (int)rightShoulder.Y();
       	
       	g.drawLine(curX, curY, curX + (int)rightElbow.X(), curY + (int)rightElbow.Y());
       	curX = curX + (int)rightElbow.X();
       	curY = curY + (int)rightElbow.Y();
       	
       	g.drawLine(curX, curY, curX + (int)rightHand.X(), curY + (int)rightHand.Y());
       	curX = StartX + (int)leftShoulder.X();
       	curY = StartY + (int)leftShoulder.Y();
       	
       	/** ___________________________________ **/
       	
       	g.drawLine(curX, curY, curX + (int)leftWing.X(), curY + (int)leftWing.Y());
       	curX = curX + (int)leftWing.X();
       	curY = curY + (int)leftWing.Y();
       	
       	g.drawLine(curX, curY, curX + (int)rightSide.X(), curY + (int)rightSide.Y());
       	curX = curX + (int)rightSide.X();
       	curY = curY + (int)rightSide.Y();
       	
       	g.drawLine(curX, curY, curX + (int)rightKnee.X(), curY + (int)rightKnee.Y());
       	curX = curX + (int)rightKnee.X();
       	curY = curY + (int)rightKnee.Y();
       	
       	g.drawLine(curX, curY, curX + (int)footKnee.X(), curY + (int)footKnee.Y());
       	curX = StartX + (int)rightShoulder.X();
       	curY = StartY + (int)rightShoulder.Y();
       	
       	/** ___________________________________ **/
       	
       	g.drawLine(curX, curY, curX + (int)rightWing.X(), curY + (int)rightWing.Y());
       	curX = curX + (int)rightWing.X();
       	curY = curY + (int)rightWing.Y();
       	
       	g.drawLine(curX, curY, curX + (int)leftSide.X(), curY + (int)leftSide.Y());
       	curX = curX + (int)leftSide.X();
       	curY = curY + (int)leftSide.Y();
       	
       	g.drawLine(curX, curY, curX + (int)leftKnee.X(), curY + (int)leftKnee.Y());
       	curX = curX + (int)leftKnee.X();
       	curY = curY + (int)leftKnee.Y();
       	
       	g.drawLine(curX, curY, curX + (int)leftFoot.X(), curY + (int)leftFoot.Y());
       	curX = curX + (int)leftFoot.X();
       	curY = curY + (int)leftFoot.Y();

    }
    
    Vector3D neckVector = null;
   	Vector3D leftShoulder = null;
   	Vector3D leftElbow = null;
   	Vector3D leftHand = null;
   	
   	Vector3D rightShoulder = null;
   	Vector3D rightElbow = null;
   	Vector3D rightHand = null;
   	
   	Vector3D leftWing = null;
   	Vector3D rightSide = null;
   	Vector3D rightKnee = null;
   	Vector3D footKnee = null;
   	
   	Vector3D rightWing = null;
   	Vector3D leftSide = null;
   	Vector3D leftKnee = null;
   	Vector3D leftFoot = null;
    
    public void scanVectors(String pose) {
    	write("output.arff" ,neckVector.toString());
    	write("output.arff" ,leftShoulder.toString());
    	write("output.arff" ,leftElbow.toString());
    	write("output.arff" ,leftHand.toString());
    	write("output.arff" ,rightShoulder.toString());
    	write("output.arff" ,rightElbow.toString());
    	write("output.arff" ,rightHand.toString());
    	write("output.arff" ,leftWing.toString());
    	write("output.arff" ,rightSide.toString());
    	write("output.arff" ,rightKnee.toString());
    	write("output.arff" ,footKnee.toString());
    	write("output.arff" ,rightWing.toString());
    	write("output.arff" ,leftSide.toString());
    	write("output.arff" ,leftKnee.toString());
    	write("output.arff" ,leftFoot.toString());
    	write("output.arff" ,pose);
    	write("output.arff" ,"\n");
    	
    	System.out.print(neckVector);
    	System.out.print(leftShoulder);
    	System.out.print(leftElbow);
    	System.out.print(leftHand);
    	System.out.print(rightShoulder);
    	System.out.print(rightElbow);
    	System.out.print(rightHand);
    	System.out.print(leftWing);
    	System.out.print(rightSide);
    	System.out.print(rightKnee);
    	System.out.print(footKnee);
    	
    	System.out.print(rightWing);
    	System.out.print(leftSide);
    	System.out.print(leftKnee);
    	System.out.print(leftFoot);
    	System.out.println();	
    }
    
    public static void write(String fileName, String text) {
        File file = new File(fileName);
     
        try {
            if(!file.exists()){
                file.createNewFile();
                File header = new File("header.txt");
                copyFile(header, file);
            }
  
            FileWriter out = new FileWriter(file.getAbsoluteFile(), true);
     
            try {
                
                out.write(text);
            } finally {
                out.close();
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static void copyFile(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }


    
    
    private Vector3D normalization(Vector3D vector) {
    	
    	vector = vector.getNormalizedVector();
    	
    	vector.multiply(50f);
    	
    	return vector;
    }
    
    public void paint(Graphics g)
    {
    	this.g = g;
    	if (drawPixels)
    	{
            DataBufferByte dataBuffer = new DataBufferByte(imgbytes, width*height*3);

            WritableRaster raster = Raster.createInterleavedRaster(dataBuffer, width, height, width * 3, 3, new int[]{0, 1, 2}, null); 

            ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{8, 8, 8}, false, false, ComponentColorModel.OPAQUE, DataBuffer.TYPE_BYTE);

            bimg = new BufferedImage(colorModel, raster, false, null);

    		g.drawImage(bimg, 0, 0, null);
    	}
        try
		{
			int[] users = userGen.getUsers();
			for (int i = 0; i < users.length; ++i)
			{
		    	Color c = colors[users[i]%colors.length];
		    	c = new Color(255-c.getRed(), 255-c.getGreen(), 255-c.getBlue());

		    	g.setColor(c);
				if (drawSkeleton && skeletonCap.isSkeletonTracking(users[i]))
				{
					drawSkeleton(g, users[i]);
				}
				if (printID)
				{
					Point3D com = depthGen.convertRealWorldToProjective(userGen.getUserCoM(users[i]));
					String label = null;
					if (!printState)
					{
						label = new String(""+users[i]);
					}
					else if (skeletonCap.isSkeletonTracking(users[i]))
					{
						// Tracking
						label = new String(users[i] + " - Tracking");
					}
					else if (skeletonCap.isSkeletonCalibrating(users[i]))
					{
						// Calibrating
						label = new String(users[i] + " - Calibrating");
					}
					else
					{
						// Nothing
						label = new String(users[i] + " - Looking for pose (" + calibPose + ")");
					}

					g.drawString(label, (int)com.getX(), (int)com.getY());
				}
			}
		} catch (StatusException e)
		{
			e.printStackTrace();
		}
    }
    
    public void printPoint(Point3D point) {
    	if(point == null) {
    		System.out.println("(null:null:null)");
    	} else {
    		float x = point.getX();
    		float y = point.getY();
    		float z = point.getZ();
    		
    		System.out.println("(" + x + ":" + y +":" + z +")");
    	}
    }
}


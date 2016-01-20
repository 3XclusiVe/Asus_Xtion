package org.OpenNI.Samples.UserTracker;

import org.OpenNI.Point3D;

public class Vector3D {
	
	private Point3D mVectorCoordinates;
	private double mMagnitude = 0;
	
	public Vector3D(Point3D start, Point3D end) {
		mVectorCoordinates = new Point3D(end.getX() - start.getX(), end.getY() - start.getY(), end.getZ() - start.getZ());
	}
	
	private Vector3D (Point3D coordinates) {
		mVectorCoordinates = coordinates;
		
	}
	
	public Vector3D getNormalizedVector() {
		
		this.mMagnitude = getMagnitude();
		
		float X = (float) (this.mVectorCoordinates.getX() / this.mMagnitude);
		float Y = (float) (this.mVectorCoordinates.getY() / this.mMagnitude);
		float Z = (float) (this.mVectorCoordinates.getZ() / this.mMagnitude);
		
		Point3D coordinates = new Point3D(X,Y,Z);
		
		return new Vector3D(coordinates);
	}
	
	public double getMagnitude() {

		double magnitude = Math.sqrt(	this.mVectorCoordinates.getX() * this.mVectorCoordinates.getX() +
											this.mVectorCoordinates.getY() * this.mVectorCoordinates.getY() +
											this.mVectorCoordinates.getZ() * this.mVectorCoordinates.getZ());
			
		return magnitude;
	}
	
	public void multiply(float number) {
		
		float X = (float) this.mVectorCoordinates.getX();
		float Y = (float) this.mVectorCoordinates.getY();
		float Z = (float) this.mVectorCoordinates.getZ();
		
		this.mVectorCoordinates = new Point3D(X * number, Y * number, Z * number);
	}
	
	@Override
	public String toString() {
		return new String(	mVectorCoordinates.getX() + ":" + 
							mVectorCoordinates.getY() + ":" +
							mVectorCoordinates.getZ());
		
	}
	
	public float X() {
		return (float)mVectorCoordinates.getX();
	}
	
	public float Y() {
		return (float)mVectorCoordinates.getY();
	}
	
	public float Z() {
		return (float)mVectorCoordinates.getZ();
	}

}

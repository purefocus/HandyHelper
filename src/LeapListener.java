import com.leapmotion.leap.*;
import com.leapmotion.leap.Frame;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

public class LeapListener
		extends Listener
{


	private static Dimension res = Toolkit.getDefaultToolkit().getScreenSize();
	private boolean grabbing = false;
	private boolean pinching = false;
	private Vector palmVel;
	private Vector pointDir;
	private Point pFilt;

	private Robot robot;
	private Point mouseLoc;


	public void onInit(Controller controller)
	{
		System.out.println("Initialized");
	}

	public void onConnect(Controller controller)
	{
		System.out.println("Connected");
		controller.enableGesture(Gesture.Type.TYPE_SWIPE);
		controller.enableGesture(Gesture.Type.TYPE_CIRCLE);
		controller.enableGesture(Gesture.Type.TYPE_SCREEN_TAP);
		controller.enableGesture(Gesture.Type.TYPE_KEY_TAP);

		try
		{
			robot = new Robot();
		}
		catch (AWTException e)
		{
			e.printStackTrace();
		}


	}

	public void onDisconnect(Controller controller)
	{
		//Note: not dispatched when running in a debugger.
		System.out.println("Disconnected");
	}

	public void onExit(Controller controller)
	{
		System.out.println("Exited");
	}

	AverageFilter grip = new AverageFilter(1.0, 0.1);
	AverageFilter pinch = new AverageFilter(2.0, 0.1);
	boolean clicking;
	Vector offset;
	Vector lastPos;

	boolean left = false;
	boolean right = false;
	boolean ctrl = false;

	public void onFrame(Controller controller)
	{
		mouseLoc = MouseInfo.getPointerInfo().getLocation();
		// Get the most recent frame and report some basic information
		Frame frame = controller.frame();

		HandList hands = frame.hands();

//
		//Get hands
		for (Hand hand : frame.hands())
		{

			if (hand.isLeft())
			{


				grabbing = grip.addValue(hand.grabAngle()) >= 0.9f;
				pinching = pinch.addValue(hand.pinchStrength()) >= 0.5f;//hand.pinchStrength() >= 1f;
				palmVel = hand.palmVelocity();

				Pointable pointer = hand.pointables().get(1);
				pinching = !pointer.isExtended();
				boolean pinky = !hand.pointables().get(4).isExtended();
//				System.out.println(grabbing + " : " + pinching);

				if (!clicking && (pinching || pinky) && !grabbing)
				{
					clicking = true;
					if (pinky && !pinching)
					{
						robot.mousePress(InputEvent.BUTTON3_MASK);
						System.out.println("right");
						right = true;
					}
					else
					{
						robot.mousePress(InputEvent.BUTTON1_MASK);
						System.out.println("left");
						left = true;
					}
				}
//				else if (!clicking && grabbing && !(pinching || pinky))
//				{
//					System.out.println("CTRL");
//					clicking = true;
//					robot.keyPress(InputEvent.CTRL_MASK);
//					ctrl = true;
//				}
				else if (clicking && !(pinching || pinky) && !grabbing)
				{

					clicking = false;
					System.out.println("Rel");
					if (left)
					{
						robot.mouseRelease(InputEvent.BUTTON1_MASK);
						left = false;
					}
					if (right)
					{
						robot.mouseRelease(InputEvent.BUTTON3_MASK);
						right = false;
					}
					if (ctrl)
					{
						robot.keyRelease(InputEvent.CTRL_MASK);
						ctrl = false;
					}
				}

				clicking = left || right || ctrl;

				if (grabbing)
				{
					robot.mouseWheel((int) palmVel.getZ() / 100);
				}
			}
			else if (hand.isRight())
			{
				Pointable pointer = hand.pointables().get(1);
				Pointable thumb = hand.pointables().get(0);

				if (pointer.isExtended() && thumb.isExtended())
				{
					Vector point = pointer.direction();

					if (pointDir == null)
					{
						pointDir = point;
					}

					float scale1 = 1.2f;
					float scale2 = 0.5f;


					point.setX(((int) (point.getX() * 100)) / 100.0f);
					point.setY(((int) (point.getY() * 100)) / 100.0f);
//					System.out.println(point);
					point.setZ(point.getZ() / 15);
					point = point.normalized();

					pointDir = pointDir.times(scale1).plus(point.times(scale2)).divide(scale1 + scale2);
					Point npos = pointerToPixel(pointDir, pointer.tipPosition(), hand.palmPosition());

					if (pFilt == null)
					{
						pFilt = new Point(npos.x, npos.y);
					}

					pFilt.x = (int) ((pFilt.x * scale1 + npos.x * scale2) / (scale1 + scale2));
					pFilt.y = (int) ((pFilt.y * scale1 + npos.y * scale2) / (scale1 + scale2));
					npos.x = pFilt.x;
					npos.y = pFilt.y;
					npos.x += res.getWidth() / 2;
					npos.y += res.getHeight() / 2;
//					System.out.println(npos);
					robot.mouseMove(npos.x, npos.y);
//				System.out.println(npos);
				}
			}


		}

		if (palmVel != null)
		{
			palmVel = filterPalmVelocity(palmVel);
		}

		palmVel = null;
	}

	Vector filterPalmVelocity(Vector vel)
	{
		vel = vel.divide(20f);
		if (vel.magnitude() <= 2f)
		{
			vel = new Vector(0, 0, 0);
		}
		if (vel.getX() <= 2.0 && vel.getZ() <= 2.0)
		{
			return new Vector(0, 0, 0);
		}
		return vel;
	}

	int i = 0;
	Point p = new Point();
	Point lp = new Point();

	public Point pointerToPixel(Vector dir, Vector pos, Vector palmPos)
	{
		pos.setY(pos.getY() - palmPos.getY());

//		float a1 = dir.pitch();
//		float a2 = dir.yaw();
//		float a3 = dir.roll();
		p.x = (int) (pos.getX() * 20);// (int) (300 * Math.tan(a2) + pos.getX() * 4);
		p.y = (int) -(pos.getY() * 30);//int) (-300 * Math.tan(a1) + pos.getZ() * 4);

		return p;
	}

	private class AverageFilter
	{
		double scale1;
		double scale2;

		double value;

		AverageFilter(double s1, double s2)
		{
			scale1 = s1;
			scale2 = s2;
		}

		double addValue(double val)
		{
			return value = (value * scale1 + val * scale2) / (scale1 + scale2);
		}

		double get()
		{
			return value;
		}


	}
}

/******************************************************************************\
 * Copyright (C) 2012-2013 Leap Motion, Inc. All rights reserved.               *
 * Leap Motion proprietary and confidential. Not for distribution.              *
 * Use subject to the terms of the Leap Motion SDK Agreement available at       *
 * https://developer.leapmotion.com/sdk_agreement, or another agreement         *
 * between Leap Motion and you, your company or other organization.             *
 \******************************************************************************/

import com.leapmotion.leap.Controller;

import java.io.IOException;

class LeapMain
{
	public static void main(String[] args)
	{
		// Create a sample listener and controller
		LeapListener listener = new LeapListener();
		Controller controller = new Controller();


		// Have the sample listener receive events from the controller
		controller.addListener(listener);

		// Keep this process running until Enter is pressed
		System.out.println("Press Enter to quit...");
		try
		{
			System.in.read();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		// Remove the sample listener when done
		controller.removeListener(listener);
	}

}

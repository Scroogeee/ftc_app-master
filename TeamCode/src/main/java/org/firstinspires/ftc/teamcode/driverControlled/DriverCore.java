package org.firstinspires.ftc.teamcode.driverControlled;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.driverControlled.driverDriving.DriveStraight4W;
import org.firstinspires.ftc.teamcode.driverControlled.driverRobotModules.DriverGlyphControl;
import org.firstinspires.ftc.teamcode.driverControlled.driverRobotModules.DriverRelicControl;
import org.firstinspires.ftc.teamcode.robotModules.JewelControl;

/**
 * Created by FTC on 25.09.2017.
 */
@TeleOp(name = "FROG-DriverControlled", group = "drive")
public class DriverCore extends OpMode {

	//DriveCross4W drive4 = new DriveCross4W();
	//DriveControl2W driveControl = new DriveControl2W();
	private DriveStraight4W driveStraight4W = new DriveStraight4W();
	private DriverGlyphControl driverGlyphControl = new DriverGlyphControl();
	private DriverRelicControl driverRelicControl = new DriverRelicControl();
	private JewelControl jewelControl = new JewelControl();

	/**
	 * Initializes the Robot and its sub-components such as:
	 * <ul>
	 * <li>Drive Controls</li>
	 * </ul>
	 */
	@Override
	public void init() {
		//driveControl.init(this);
		//drive4.init(this);
		driveStraight4W.init(this);
		driverGlyphControl.init(this);
		driverRelicControl.init(this);
		jewelControl.initialize(this.hardwareMap);
		jewelControl.updateArm(1);
	}

	/**
	 * This is the main loop which executes every tick.
	 */
	@Override
	public void loop() {
		//drive4.update();
		//driveControl.update();
		driveStraight4W.update();
		driverGlyphControl.updateGlyphArm();
		driverRelicControl.update();
	}


}

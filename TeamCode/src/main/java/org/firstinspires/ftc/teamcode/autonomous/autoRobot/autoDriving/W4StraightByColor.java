package org.firstinspires.ftc.teamcode.autonomous.autoRobot.autoDriving;

import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cGyro;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cRangeSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.autonomous.AutonomousCore;
import org.firstinspires.ftc.teamcode.util.enums.HardwareConfiguration;

import java.util.ArrayList;

import static com.qualcomm.robotcore.hardware.DcMotor.RunMode.*;
import static org.firstinspires.ftc.teamcode.util.Constants.*;
import static org.firstinspires.ftc.teamcode.util.enums.HardwareConfiguration.*;

/**
 * Created by FTC on 25.01.2018.
 */

public class W4StraightByColor extends W4StraightAuto {

	// These constants define the desired driving/control characteristics
	// The can/should be tweaked to suite the specific robot drive train.
	public static final double DRIVE_SPEED = 0.2;     // Nominal speed for better accuracy.
	public static final double TURN_SPEED = 0.6;     // Nominal half speed for better accuracy.
	static final double COUNTS_PER_MOTOR_REV = 1440;    // eg: TETRIX Motor Encoder
	static final double DRIVE_GEAR_REDUCTION = 1.0;     // This is < 1.0 if geared UP
	static final double WHEEL_DIAMETER_INCHES = 4.0;     // For figuring circumference
	static final double COUNTS_PER_INCH = (COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) /
			(WHEEL_DIAMETER_INCHES * 3.1415926535);
	static final double HEADING_THRESHOLD = 1;      // As tight as we can make it with an integer gyro
	static final double P_TURN_COEFF = 0.3;     // Larger is more responsive, but also less stable
	static final double P_DRIVE_COEFF = 0.13;     // Larger is more responsive, but also less stable
	/**
	 * Gyro
	 */
	protected ModernRoboticsI2cGyro gyro = null;
	/**
	 * Range Sensor
	 */
	protected ModernRoboticsI2cRangeSensor rangeSensor = null;
	HardwareConfiguration hwConfig = null;
	private boolean useGyro = false;
	private boolean useRange = false;
	private double rangeBaseValue = 0;

	public W4StraightByColor(AutonomousCore param_ac) {
		super(param_ac);
	}

	public boolean isGyroUsed() {
		return useGyro;
	}

	public boolean isRangeUsed() {
		return useRange;
	}

	@Override
	public void initialize() {
		super.initialize();
		hwConfig = autonomousCore.getHardwareConfiguration();
		if (hwConfig == GREEN || hwConfig == YELLOW) {
			useGyro = true;
		}
		if (hwConfig == GREEN || hwConfig == BLUE) {
			useRange = true;
		}
		if (useGyro) {
			initGyro();
		}
		if (useRange) {
			initRange();
		}
	}

	/**
	 * Initializes the range sensor
	 */
	private void initRange() {
		rangeSensor = autonomousCore.hardwareMap.get(ModernRoboticsI2cRangeSensor.class, range_sensor_name);
		calibrateRange();
	}

	/**
	 * Initializes the gyroscope
	 */
	private void initGyro() {
		gyro = (ModernRoboticsI2cGyro) autonomousCore.hardwareMap.gyroSensor.get(gyro_name);

		// Ensure the robot it stationary, then reset the encoders and calibrate the gyro.
		A.setMode(STOP_AND_RESET_ENCODER);
		B.setMode(STOP_AND_RESET_ENCODER);
		C.setMode(STOP_AND_RESET_ENCODER);
		D.setMode(STOP_AND_RESET_ENCODER);
		// Send telemetry message to alert driver that we are calibrating;
		autonomousCore.telemetry.addData(">", "Calibrating Gyro");    //
		autonomousCore.telemetry.update();

		gyro.calibrate();

		// make sure the gyro is calibrated before continuing
		while (!autonomousCore.isStopRequested() && gyro.isCalibrating()) {
			autonomousCore.sleep(50);
			autonomousCore.idle();
		}

		autonomousCore.telemetry.addData(">", "Robot Ready.");    //
		autonomousCore.telemetry.update();

		A.setMode(RUN_USING_ENCODER);
		B.setMode(RUN_USING_ENCODER);
		C.setMode(RUN_USING_ENCODER);
		D.setMode(RUN_USING_ENCODER);
		gyro.resetZAxisIntegrator();

	}

	/**
	 * Method to drive on a fixed compass bearing (angle), based on encoder counts.
	 * Move will stop if either of these conditions occur:
	 * 1) Move gets to the desired position
	 * 2) Driver stops the opmode running.
	 *
	 * @param speed    Target speed for forward motion.  Should allow for _/- variance for adjusting heading
	 * @param distance Distance (in inches) to move from current position.  Negative distance means move backwards.
	 * @param angle    Absolute Angle (in Degrees) relative to last gyro reset.
	 *                 0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
	 *                 If a relative angle is required, add/subtract from current heading.
	 */
	private void gyroDrive(double speed,
	                       double distance,
	                       double angle) {

		int newLeftTarget;
		int newRightTarget;
		int moveCounts;
		double max;
		double error;
		double steer;
		double leftSpeed;
		double rightSpeed;

		// Ensure that the opmode is still active
		if (autonomousCore.opModeIsActive()) {
			setupForTank(true);
			A.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
			B.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
			C.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
			D.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
			autonomousCore.telemetry.addData("A:", A.getTargetPosition());
			autonomousCore.telemetry.addData("B:", B.getTargetPosition());
			autonomousCore.telemetry.addData("C:", C.getTargetPosition());
			autonomousCore.telemetry.addData("D:", D.getTargetPosition());
			autonomousCore.telemetry.update();
			// Determine new target position, and pass to motor controller
			moveCounts = (int) (distance * COUNTS_PER_INCH);
			newLeftTarget = A.getCurrentPosition() + moveCounts;
			newRightTarget = B.getCurrentPosition() + moveCounts;

			// Set Target and Turn On RUN_TO_POSITION
			A.setTargetPosition(newLeftTarget);
			B.setTargetPosition(newRightTarget);
			D.setTargetPosition(newLeftTarget);
			C.setTargetPosition(newRightTarget);

			A.setMode(DcMotor.RunMode.RUN_TO_POSITION);
			B.setMode(DcMotor.RunMode.RUN_TO_POSITION);
			C.setMode(DcMotor.RunMode.RUN_TO_POSITION);
			D.setMode(DcMotor.RunMode.RUN_TO_POSITION);

			// start motion.
			speed = Range.clip(Math.abs(speed), 0.0, 1.0);
			A.setPower(speed);
			B.setPower(speed);
			C.setPower(speed);
			D.setPower(speed);

			// keep looping while we are still active, and BOTH motors are running.
			while (autonomousCore.opModeIsActive() &&
					(A.isBusy() && B.isBusy() && C.isBusy() && D.isBusy())) {

				// adjust relative speed based on heading error.
				error = getError(angle);
				steer = getSteer(error, P_DRIVE_COEFF);

				// if driving in reverse, the motor correction also needs to be reversed
				if (distance < 0)
					steer *= -1.0;

				leftSpeed = speed - steer;
				rightSpeed = speed + steer;

				// Normalize speeds if either one exceeds +/- 1.0;
				max = Math.max(Math.abs(leftSpeed), Math.abs(rightSpeed));
				if (max > 1.0) {
					leftSpeed /= max;
					rightSpeed /= max;
				}

				A.setPower(leftSpeed);
				B.setPower(rightSpeed);
				D.setPower(leftSpeed);
				C.setPower(rightSpeed);

				// Display drive status for the driver.
/*				autonomousCore.telemetry.addData("Err/St", "%5.1f/%5.1f", error, steer);
				autonomousCore.telemetry.addData("Target", "%7d:%7d", newLeftTarget, newRightTarget);
				autonomousCore.telemetry.addData("Actual", "%7d:%7d", A.getCurrentPosition(),
						B.getCurrentPosition());
				autonomousCore.telemetry.addData("Speed", "%5.2f:%5.2f", leftSpeed, rightSpeed);
				autonomousCore.telemetry.update();
*/
			}

			// Stop all motion;
			A.setPower(0);
			B.setPower(0);
			C.setPower(0);
			D.setPower(0);

			// Turn off RUN_TO_POSITION
			A.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
			B.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
			C.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
			D.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
		}
	}

	/**
	 * Method to spin on central axis to point in a new direction.
	 * Move will stop if either of these conditions occur:
	 * 1) Move gets to the heading (angle)
	 * 2) Driver stops the opmode running.
	 *
	 * @param speed Desired speed of turn.
	 * @param angle Absolute Angle (in Degrees) relative to last gyro reset.
	 *              0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
	 *              If a relative angle is required, add/subtract from current heading.
	 */
	public void gyroTurn(double speed, double angle) {
		setupForTank(true);
		A.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
		B.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
		C.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
		D.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
		// keep looping while we are still active, and not on heading.
		while (autonomousCore.opModeIsActive() && !onHeading(speed, angle, P_TURN_COEFF)) {
			// Update telemetry & Allow time for other processes to run.
			autonomousCore.telemetry.update();
		}
	}

	/**
	 * Method to obtain and hold a heading for a finite amount of time
	 * Move will stop once the requested time has elapsed
	 *
	 * @param speed    Desired speed of turn.
	 * @param angle    Absolute Angle (in Degrees) relative to last gyro reset.
	 *                 0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
	 *                 If a relative angle is required, add/subtract from current heading.
	 * @param holdTime Length of time (in seconds) to hold the specified heading.
	 */
	public void gyroHold(double speed, double angle, double holdTime) {
		setupForTank(true);
		ElapsedTime holdTimer = new ElapsedTime();

		// keep looping while we have time remaining.
		holdTimer.reset();
		while (autonomousCore.opModeIsActive() && (holdTimer.time() < holdTime)) {
			// Update telemetry & Allow time for other processes to run.
			onHeading(speed, angle, P_TURN_COEFF);
			autonomousCore.telemetry.update();
		}

		// Stop all motion;
		A.setPower(0);
		B.setPower(0);
		C.setPower(0);
		D.setPower(0);
	}

	/**
	 * Perform one cycle of closed loop heading control.
	 *
	 * @param speed  Desired speed of turn.
	 * @param angle  Absolute Angle (in Degrees) relative to last gyro reset.
	 *               0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
	 *               If a relative angle is required, add/subtract from current heading.
	 * @param PCoeff Proportional Gain coefficient
	 * @return if on Heading
	 */
	boolean onHeading(double speed, double angle, double PCoeff) {
		double error;
		double steer;
		boolean onTarget = false;
		double leftSpeed;
		double rightSpeed;

		// determine turn power based on +/- error
		error = getError(angle);

		if (Math.abs(error) <= HEADING_THRESHOLD) {
			steer = 0.0;
			leftSpeed = 0.0;
			rightSpeed = 0.0;
			onTarget = true;
		} else {
			steer = getSteer(error, PCoeff);
			rightSpeed = speed * steer;
			leftSpeed = -rightSpeed;
		}

		// Send desired speeds to motors.
		A.setPower(leftSpeed);
		D.setPower(leftSpeed);
		B.setPower(rightSpeed);
		C.setPower(rightSpeed);

		// Display it for the driver.
		/*autonomousCore.telemetry.addLine("Adjusting Error");
		autonomousCore.telemetry.addData("Target", "%5.2f", angle);
		autonomousCore.telemetry.addData("Err/St", "%5.2f/%5.2f", error, steer);
		autonomousCore.telemetry.addData("Speed.", "%5.2f:%5.2f", leftSpeed, rightSpeed);*/
		return onTarget;
	}

	/**
	 * getError determines the error between the target angle and the robot's current heading
	 *
	 * @param targetAngle Desired angle (relative to global reference established at last Gyro Reset).
	 * @return error angle: Degrees in the range +/- 180. Centered on the robot's frame of reference
	 * +ve error means the robot should turn LEFT (CCW) to reduce error.
	 */
	public double getError(double targetAngle) {

		double robotError;

		// calculate error in -179 to +180 range  (
		robotError = targetAngle - gyro.getIntegratedZValue();
		while (robotError > 180) robotError -= 360;
		while (robotError <= -180) robotError += 360;
		return robotError;
	}

	/**
	 * returns desired steering force.  +/- 1 range.  +ve = steer left
	 *
	 * @param error  Error angle in robot relative degrees
	 * @param PCoeff Proportional Gain Coefficient
	 * @return the Steer value
	 */
	public double getSteer(double error, double PCoeff) {
		return Range.clip(error * PCoeff, -1, 1);
	}

	/**
	 * Drives to the next cryptobox column using the<br>
	 * range sensor in the given direction
	 *
	 * @param d the direction
	 */
	private void driveToNextColumnByRange(DcMotorSimple.Direction d) {
		setupForTank(false);
		A.setMode(RUN_WITHOUT_ENCODER);
		B.setMode(RUN_WITHOUT_ENCODER);
		C.setMode(RUN_WITHOUT_ENCODER);
		D.setMode(RUN_WITHOUT_ENCODER);
		if (useRange) {
			//Drive past the last column
			if (d == DcMotorSimple.Direction.FORWARD) {
				A.setPower(-DRIVE_SPEED);
				B.setPower(DRIVE_SPEED);
				C.setPower(DRIVE_SPEED);
				D.setPower(-DRIVE_SPEED);
			} else if (d == DcMotorSimple.Direction.REVERSE) {
				A.setPower(DRIVE_SPEED);
				B.setPower(-DRIVE_SPEED);
				C.setPower(-DRIVE_SPEED);
				D.setPower(DRIVE_SPEED);
			}
			while (autonomousCore.opModeIsActive() &&
					(rangeSensor.getDistance(DistanceUnit.CM) >=
							(rangeBaseValue - RANGE_THRESHOLD_CM))) {
				autonomousCore.telemetry.addData("Distance:",
						rangeSensor.getDistance(DistanceUnit.CM));
				autonomousCore.telemetry.update();
			}
			//stop motors for half a second
			A.setPower(0);
			B.setPower(0);
			C.setPower(0);
			D.setPower(0);
			//autonomousCore.sleep(1000);
			//Drive to the column
			if (d == DcMotorSimple.Direction.FORWARD) {
				A.setPower(-DRIVE_SPEED);
				B.setPower(DRIVE_SPEED);
				C.setPower(DRIVE_SPEED);
				D.setPower(-DRIVE_SPEED);
			} else if (d == DcMotorSimple.Direction.REVERSE) {
				A.setPower(DRIVE_SPEED);
				B.setPower(-DRIVE_SPEED);
				C.setPower(-DRIVE_SPEED);
				D.setPower(DRIVE_SPEED);
			}
			while (autonomousCore.opModeIsActive() &&
					(rangeSensor.getDistance(DistanceUnit.CM) <
							(rangeBaseValue - RANGE_THRESHOLD_CM))) {
				autonomousCore.telemetry.addData("Distance:",
						rangeSensor.getDistance(DistanceUnit.CM));
				autonomousCore.telemetry.update();
			}
			A.setPower(0);
			B.setPower(0);
			C.setPower(0);
			D.setPower(0);
		}
	}

	/**
	 * Drives to a specified number of cryptobox columns<br>
	 * using the range sensor in the given direction
	 *
	 * @param columnNum the number of cryptbox columns to pass
	 * @param d         the direction
	 */
	public void driveToColumnByRange(int columnNum, DcMotorSimple.Direction d) {
		if (useRange) {
			int columnCounter = 0;
			while (autonomousCore.opModeIsActive()) {
				if (columnCounter >= columnNum) {
					break;
				}
				driveToNextColumnByRange(d);
				columnCounter++;
				//autonomousCore.sleep(1000);
			}
		}
	}

	/**
	 * Sets the range base value to the <br>
	 * current reading of the sensor
	 */
	public void calibrateRange() {
		if (useRange) {
			ArrayList<Double> list = new ArrayList<>();
			double average = 0;
			for (int i = 0; i < 5; i++) {
				autonomousCore.sleep(50);
				list.add(rangeSensor.getDistance(DistanceUnit.CM));
			}
			for (double d : list) {
				average += d;
			}
			average /= list.size();
			rangeBaseValue = average;
			//TODO remove debug
			autonomousCore.telemetry.addData("rangeBase", rangeBaseValue);
			autonomousCore.telemetry.update();
		}
	}

}
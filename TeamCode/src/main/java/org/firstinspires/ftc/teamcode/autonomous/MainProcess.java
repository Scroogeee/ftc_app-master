package org.firstinspires.ftc.teamcode.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.preJC_DriverControlled.Constants;

/**
 * Created by FTC on 20.11.2017.
 */
@Autonomous(name = "AutonomousMode")
public class MainProcess extends LinearOpMode {

    //private instance variables
    DcMotor left_drive = hardwareMap.dcMotor.get(Constants.ld_name);
    DcMotor right_drive = hardwareMap.dcMotor.get(Constants.rd_name);

    //Elapsed time
    ElapsedTime counter = new ElapsedTime();


    @Override
    public void runOpMode() throws InterruptedException {
        counter.reset();
        while (counter.milliseconds() <= 30 * Math.pow(10, 3)) {

        }
    }
}

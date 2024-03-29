// Copyright (c) FIRST and other WPILib contributors.

// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.motorcontrol.MotorController;
import edu.wpi.first.wpilibj.motorcontrol.PWMSparkMax;


public class SwerveModule {
    private static final double WHEEL_RADIUS_METERS = 0.0508;
    private static final int ENCODER_RESOLUTION_TICKS_PER_REV = 4096;

    private static final double MODULE_MAX_ANGULAR_VELOCITY = SwerveDrivetrain.MAX_ANGULAR_SPEED_RADIANS_PER_SECOND;
    private static final double MODULE_MAX_ANGULAR_ACCELERATION = 2 * Math.PI; // radians per second squared

    private final MotorController driveMotor;
    private final MotorController turningMotor;

    private final Encoder driveEncoder;
    private final Encoder turningEncoder;

    // Gains are for example purposes only - must be determined for your own robot!
    private final PIDController drivePIDController = new PIDController(1, 0, 0);

    // Gains are for example purposes only - must be determined for your own robot!
    private final ProfiledPIDController turningPIDController = new ProfiledPIDController(1, 0, 0, new TrapezoidProfile.Constraints(MODULE_MAX_ANGULAR_VELOCITY, MODULE_MAX_ANGULAR_ACCELERATION));

    // Gains are for example purposes only - must be determined for your own robot!
    private final SimpleMotorFeedforward driveFeedforward = new SimpleMotorFeedforward(1, 3);
    private final SimpleMotorFeedforward turnFeedforward = new SimpleMotorFeedforward(1, 0.5);


    /**
     * Constructs a SwerveModule with a drive motor, turning motor, drive encoder and turning encoder.
     *
     * @param driveMotorChannel      PWM output for the drive motor.
     * @param turningMotorChannel    PWM output for the turning motor.
     * @param driveEncoderChannelA   DIO input for the drive encoder channel A
     * @param driveEncoderChannelB   DIO input for the drive encoder channel B
     * @param turningEncoderChannelA DIO input for the turning encoder channel A
     * @param turningEncoderChannelB DIO input for the turning encoder channel B
     */
    public SwerveModule(int driveMotorChannel, int turningMotorChannel, int driveEncoderChannelA, int driveEncoderChannelB, int turningEncoderChannelA, int turningEncoderChannelB) {
        driveMotor = new PWMSparkMax(driveMotorChannel);
        turningMotor = new PWMSparkMax(turningMotorChannel);

        driveEncoder = new Encoder(driveEncoderChannelA, driveEncoderChannelB);
        turningEncoder = new Encoder(turningEncoderChannelA, turningEncoderChannelB);

        // Set the distance per pulse for the drive encoder. We can simply use the
        // distance traveled for one rotation of the wheel divided by the encoder
        // resolution.
        driveEncoder.setDistancePerPulse((2 * Math.PI) * WHEEL_RADIUS_METERS / ENCODER_RESOLUTION_TICKS_PER_REV);

        // Set the distance (in this case, angle) in radians per pulse for the turning encoder.
        // This is the angle through an entire rotation (2 * pi) divided by the
        // encoder resolution.
        turningEncoder.setDistancePerPulse((2 * Math.PI) / ENCODER_RESOLUTION_TICKS_PER_REV);

        // Limit the PID Controller's input range between -pi and pi and set the input
        // to be continuous.
        turningPIDController.enableContinuousInput(-Math.PI, Math.PI);
    }


    /**
     * Returns the current state of the module.
     *
     * @return The current state of the module.
     */
    public SwerveModuleState getState() {
        return new SwerveModuleState(driveEncoder.getRate(), new Rotation2d(turningEncoder.getDistance()));
    }


    /**
     * Returns the current position of the module.
     *
     * @return The current position of the module.
     */
    public SwerveModulePosition getPosition() {
        return new SwerveModulePosition(driveEncoder.getDistance(), new Rotation2d(turningEncoder.getDistance()));
    }


    /**
     * Stops the module
     */
    public void stop() {
        driveMotor.setVoltage(0);
        turningMotor.setVoltage(0);
    }


    /**
     * Sets the desired state for the module.
     *
     * @param desiredState Desired state with speed and angle.
     */
    public void setDesiredState(SwerveModuleState desiredState) {
        // Optimize the reference state to avoid spinning further than 90 degrees
        SwerveModuleState state = SwerveModuleState.optimize(desiredState, new Rotation2d(turningEncoder.getDistance()));

        // Calculate the drive output from the drive PID controller.
        final double driveOutput = drivePIDController.calculate(driveEncoder.getRate(), state.speedMetersPerSecond);

        final double driveFeedforward = this.driveFeedforward.calculate(state.speedMetersPerSecond);

        // Calculate the turning motor output from the turning PID controller.
        final double turnOutput = turningPIDController.calculate(turningEncoder.getDistance(), state.angle.getRadians());

        final double turnFeedforward = this.turnFeedforward.calculate(turningPIDController.getSetpoint().velocity);

        driveMotor.setVoltage(driveOutput + driveFeedforward);
        turningMotor.setVoltage(turnOutput + turnFeedforward);
    }
}

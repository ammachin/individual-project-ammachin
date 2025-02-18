"""

Fly to position coordinates as instructed by user.

Test script (untested).

TODO: Combine set path and changed path (use classes!)

"""

import logging
import numpy as np
import sys
import time
from threading import Event

import cflib.crtp
from cflib.crazyflie import CrazyFlie
from cflib.crazyflie.log import LogConfig
from cflib.crazyflie.syncCrazyflie import SyncCrazyFlie
from cflib.utils import uri_helper
from cflib.utils.reset_estimator import reset_estimator

URI = uri_helper.uri_from_env(default="radio://0/80/2M/E7E7E7E7E7") #TODO: change to my CrazyFlie
deck_attached_event = Event()

position_estimate = [0.0, 0.0, 0.0]


"""
Callback for printing current CrazyFlie position.
"""
def log_pos_callback(timestamp, data, log_conf):
    position_estimate[0] = data["kalman.stateX"]
    position_estimate[1] = data["kalman.stateY"]
    position_estimate[2] = data["kalman.stateZ"]

    print("Position: {%f, %f, %f}" % position_estimate[0], position_estimate[1], position_estimate[2])


"""
Callback for checking whether Loco Positioning deck is attached to CrazyFlie.
"""
def param_deck_loco(_, value_str):
    value = int(value_str) # intially a string
    print(value)
    if value:
        deck_attached_event.set() # global variable 
        print("Deck is attached")
    else:
        print("Deck is not attached")


"""
Initiate the logging of CrazyFlie position coordinates

Parameters
----------
    scf: SyncCrazyFlie instance
"""
def init_log_config(scf):
    # Logging while flying
    log_conf = LogConfig(name="Position", period_in_ms=10)
    log_conf.add_variable("kalman.stateX", "float")
    log_conf.add_variable("kalman.stateY", "float")
    log_conf.add_variable("kalman.stateZ", "float")

    scf.cf.log.addConfig(log_conf)
    log_conf.data_received_cb.add_callback(log_pos_callback)

    log_conf.start()

    return log_conf


"""
Send setpoints to the CrazyFlie.

Parameters
----------
    scf: SyncCrazyFlie instance
    final_pos: vector of final position
    yaw: required rotation to fly initial to final
"""
def fly_trajectory(scf, final_pos, yaw):
    # arm the crazyflie
    scf.cf.platform.send_arming_request(True)
    time.sleep(1.0)

    print("Starting path")
    for i in range(50):
        scf.cf.commander.send_position_setpoint(position_estimate[0], final_pos[1], position_estimate[2])
        time.sleep(0.1)

    print("Flying to {%f, %f, %f}" % final_pos[0], final_pos[1], final_pos[2])
    for i in range(50): # why 50??
        scf.cf.commander.send_position_setpoint(final_pos[0], final_pos[1], final_pos[2], yaw)
        time.sleep(0.1)

    print("Landing")
    for i in range(50):
        scf.cf.commander.send_position_setpoint(position_estimate[0], 0.0, position_estimate[2])
        time.sleep(0.1)


"""
Calculate unit vector.
"""
def calc_unit_vector(v):
    return v / np.linalg.norm(v)


"""
Calculate angle of two vectors + correct direction.
"""
def calculate_yaw(init_pos, final_pos):
    init_unit = calc_unit_vector((init_pos[0], init_pos[1], init_pos[2]))
    final_unit = calc_unit_vector((final_pos[0], final_pos[1], final_pos[2]))

    # arctan2 ensures that we have the right direction of rotation
    return np.degrees(np.arctan2(np.cross(init_unit, final_unit), np.dot(init_unit, final_unit)))


"""
Main function.
"""
if __name__ == "__main__":
    cflib.crtp.initi_drivers()
    with SyncCrazyFlie(URI, cf=CrazyFlie(rw_cache="./cache")) as scf:
        reset_estimator(scf)

        log_conf = init_log_config(scf)

        # Checking deck is attached
        scf.cf.param.add_update_callback(group="deck", name="bcLoco", cb=param_deck_loco)
        time.sleep(1)

        if not deck_attached_event.wait(timeout=5):
            print("Loco Positioning deck not attached")
            sys.exit(1)

        print("Enter all coordinates in the format x.x\n")
        final_pos = []
        final_pos.append = input("Enter x coordinate: ")
        final_pos.append = input("Enter y coordinate: ")
        final_pos.append = input("Enter z coordinate: ")

        for pos in final_pos:
            final_pos[pos] = float(final_pos[pos])

        # We don't actually care about y
        final_pos[1] = 2.0

        yaw = calculate_yaw((position_estimate[0], 2.0, position_estimate[2]),
                      final_pos[0], final_pos[1], final_pos[2])

        fly_trajectory(scf, final_pos, yaw)
    
        log_conf.stop()
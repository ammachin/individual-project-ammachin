"""

Fly a specific path.

Test script.

"""


import logging
import sys
import time
from threading import Event

import cflib.crtp
from cflib.crazyflie import Crazyflie
from cflib.crazyflie.log import LogConfig
from cflib.crazyflie.syncCrazyflie import SyncCrazyflie
from cflib.utils import uri_helper
#from cflib.utils.reset_estimator import reset_estimator

URI = uri_helper.uri_from_env(default="radio://0/80/2M/E7E7E7E7E7") #TODO: change to specific CrazyFlie
deck_attached_event = Event()

#DEFAULT_HEIGHT = 0.5

position_estimate = [0.0, 0.0, 0.0]

sequence = [
    (2.0, 1.0, 0.5, 0),
    (2.5, 2.0, 1.0, 0)
]


"""
Callback for printing current CrazyFlie position.
"""
def log_pos_callback(timestamp, data, log_conf):
    position_estimate[0] = data["kalman.stateX"]
    position_estimate[1] = data["kalman.stateY"]
    position_estimate[2] = data["kalman.stateZ"]

    print(f"Position: ({position_estimate[0], position_estimate[1], position_estimate[2]})")


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
    log_conf = LogConfig(name="Position", period_in_ms=10)
    log_conf.add_variable("kalman.stateX", "float")
    log_conf.add_variable("kalman.stateY", "float")
    log_conf.add_variable("kalman.stateZ", "float")

    scf.cf.log.add_config(log_conf)
    log_conf.data_received_cb.add_callback(log_pos_callback)

    log_conf.start()

    return log_conf


def take_off(cf, position):
    position_test = 1.5
    take_off_time = 1.0
    sleep_time = 0.1
    steps = int(take_off_time / sleep_time)
    vz = position[2]/ take_off_time

    print(f'take off at {position[2]}')

    for i in range(steps):
        cf.commander.send_velocity_world_setpoint(0, 0, vz, 0)
        time.sleep(sleep_time)

"""
Send sequence setpoints to the CrazyFlie.

Parameters
----------
    scf: SyncCrazyFlie instance
"""
def run_sequence(scf, sequence):
    # arm the crazyflie
    scf.cf.platform.send_arming_request(True)
    time.sleep(1.0)

    take_off(scf.cf, sequence[0])

    # for pos in sequence:
    #     print("Setting position {}".format(pos))
    #     for i in range(50): # why 50?
    #         scf.cf.commander.send_position_setpoint(pos[0], pos[1], pos[2], pos[3])
    #         time.sleep(0.1)


"""
Main function.
"""
if __name__ == "__main__":
    cflib.crtp.init_drivers()
    with SyncCrazyflie(URI, cf=Crazyflie(rw_cache="./cache")) as scf:
        #reset_estimator(scf)

        log_conf = init_log_config(scf)

        # Checking deck is attached
        scf.cf.param.add_update_callback(group="deck", name="bcLoco", cb=param_deck_loco)
        time.sleep(1)

        # if not deck_attached_event.wait(timeout=5):
        #     print("Loco Positioning deck not attached")
        #     sys.exit(1)

        run_sequence(scf, sequence)
    
        log_conf.stop()
